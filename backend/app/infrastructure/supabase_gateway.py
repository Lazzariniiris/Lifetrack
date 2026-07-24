import json
from typing import Any
from uuid import UUID

import httpx

from app.core.config import settings
from app.core.errors import AppError, auth_unavailable, data_unavailable, unauthorized
from app.domain.contracts import (
    MealCreate,
    MealCorrections,
    MealPage,
    MealResponse,
    MealUpdate,
    Nutrition,
)


MEAL_COLUMNS = (
    "id,foods_json,calories,protein_g,carbs_g,fat_g,fiber_g,sugars_g,sodium_mg,"
    "confidence,observations,status,photo_path,corrections_json,created_at,updated_at"
)


class SupabaseGateway:
    def __init__(self, client: httpx.AsyncClient | None = None) -> None:
        self.client = client
        self.timeout = httpx.Timeout(settings.supabase_timeout_seconds)

    def _headers(self, token: str | None = None, prefer: str | None = None) -> dict[str, str]:
        headers = {
            "apikey": settings.supabase_anon_key.get_secret_value(),
            "Accept": "application/json",
        }
        if token:
            headers["Authorization"] = f"Bearer {token}"
        if prefer:
            headers["Prefer"] = prefer
        return headers

    async def _send(self, method: str, path: str, **kwargs: Any) -> httpx.Response:
        try:
            if self.client is not None:
                response = await self.client.request(method, path, timeout=self.timeout, **kwargs)
            else:
                async with httpx.AsyncClient(base_url=settings.supabase_url) as client:
                    response = await client.request(method, path, timeout=self.timeout, **kwargs)
        except httpx.RequestError:
            raise data_unavailable() from None

        if response.status_code == 429 or response.status_code >= 500:
            raise data_unavailable()
        return response

    async def authenticate(self, token: str) -> tuple[str, str]:
        try:
            response = await self._send(
                "GET",
                "/auth/v1/user",
                headers=self._headers(token),
            )
        except AppError:
            raise auth_unavailable() from None
        if response.status_code in {401, 403}:
            raise unauthorized()
        if response.status_code != 200:
            raise auth_unavailable()
        try:
            user_id = str(UUID(response.json()["id"]))
        except (KeyError, TypeError, ValueError, json.JSONDecodeError):
            raise auth_unavailable() from None
        return user_id, token

    async def is_ready(self) -> bool:
        try:
            response = await self._send(
                "GET",
                "/rest/v1/meal_analyses",
                params={"select": "id", "limit": "1"},
                headers=self._headers(),
            )
            return response.status_code == 200
        except AppError:
            return False

    async def consume_analysis_quota(self, token: str) -> None:
        response = await self._send(
            "POST",
            "/rest/v1/rpc/consume_meal_analysis_quota",
            json={"limit_value": settings.daily_analysis_limit},
            headers=self._headers(token),
        )
        if response.status_code != 200:
            raise data_unavailable()
        try:
            allowed = response.json()
        except json.JSONDecodeError:
            raise data_unavailable() from None
        if allowed is not True:
            raise AppError(429, "ANALYSIS_LIMIT_REACHED", "Daily meal analysis limit reached.")

    async def save_analysis(self, user_id: str, token: str, meal: MealCreate) -> MealResponse:
        self._validate_photo_path(user_id, meal.photo_path)
        payload = {
            "id": str(meal.id),
            "user_id": user_id,
            "foods_json": [food.model_dump(mode="json") for food in meal.foods],
            **meal.nutrition.model_dump(mode="json"),
            "confidence": meal.confidence,
            "observations": meal.observations,
            "status": "completed",
            "photo_path": meal.photo_path,
        }
        response = await self._send(
            "POST",
            "/rest/v1/meal_analyses",
            params={"on_conflict": "id"},
            json=payload,
            headers=self._headers(token, "resolution=merge-duplicates,return=minimal"),
        )
        if response.status_code not in {200, 201, 204, 409}:
            raise data_unavailable()

        existing = await self.get_meal(meal.id, user_id, token, required=False)
        if existing is None:
            raise AppError(409, "MEAL_ID_CONFLICT", "This meal ID is already in use.")
        return existing

    async def list_meals(
        self, user_id: str, token: str, limit: int, offset: int
    ) -> MealPage:
        response = await self._send(
            "GET",
            "/rest/v1/meal_analyses",
            params={
                "select": MEAL_COLUMNS,
                "user_id": f"eq.{user_id}",
                "deleted_at": "is.null",
                "status": "in.(completed,corrected,failed)",
                "order": "created_at.desc,id.desc",
                "limit": str(limit),
                "offset": str(offset),
            },
            headers=self._headers(token, "count=exact"),
        )
        if response.status_code != 200:
            raise data_unavailable()
        rows = self._json_rows(response)
        content_range = response.headers.get("content-range", "")
        try:
            total = int(content_range.rsplit("/", 1)[1])
        except (IndexError, ValueError):
            total = len(rows)
        return MealPage(
            items=[self._meal_from_row(row) for row in rows],
            limit=limit,
            offset=offset,
            total=total,
        )

    async def get_meal(
        self,
        meal_id: UUID,
        user_id: str,
        token: str,
        *,
        required: bool = True,
    ) -> MealResponse | None:
        response = await self._send(
            "GET",
            "/rest/v1/meal_analyses",
            params={
                "select": MEAL_COLUMNS,
                "id": f"eq.{meal_id}",
                "user_id": f"eq.{user_id}",
                "deleted_at": "is.null",
                "limit": "1",
            },
            headers=self._headers(token),
        )
        if response.status_code != 200:
            raise data_unavailable()
        rows = self._json_rows(response)
        if not rows:
            if required:
                raise AppError(404, "MEAL_NOT_FOUND", "Meal not found.")
            return None
        return self._meal_from_row(rows[0])

    async def update_meal(
        self, meal_id: UUID, user_id: str, token: str, update: MealUpdate
    ) -> MealResponse:
        changes = update.model_dump(mode="json", exclude_unset=True)
        self._validate_photo_path(user_id, changes.get("photo_path"))
        payload: dict[str, Any] = {}
        corrections: dict[str, Any] = {}
        if "foods" in changes:
            payload["foods_json"] = changes["foods"]
            corrections["foods"] = changes["foods"]
        if "nutrition" in changes:
            payload.update(changes["nutrition"])
            corrections["nutrition"] = changes["nutrition"]
        for name in ("confidence", "observations"):
            if name in changes:
                payload[name] = changes[name]
                corrections[name] = changes[name]
        if "correction_note" in changes:
            corrections["note"] = changes["correction_note"]
        if "photo_path" in changes:
            payload["photo_path"] = changes["photo_path"]
        if corrections:
            payload["corrections_json"] = corrections
            payload["status"] = "corrected"

        response = await self._send(
            "PATCH",
            "/rest/v1/meal_analyses",
            params={
                "id": f"eq.{meal_id}",
                "user_id": f"eq.{user_id}",
                "deleted_at": "is.null",
            },
            json=payload,
            headers=self._headers(token, "return=representation"),
        )
        if response.status_code != 200:
            raise data_unavailable()
        rows = self._json_rows(response)
        if not rows:
            raise AppError(404, "MEAL_NOT_FOUND", "Meal not found.")
        return self._meal_from_row(rows[0])

    async def delete_meal(self, meal_id: UUID, user_id: str, token: str) -> None:
        response = await self._send(
            "DELETE",
            "/rest/v1/meal_analyses",
            params={"id": f"eq.{meal_id}", "user_id": f"eq.{user_id}"},
            headers=self._headers(token, "return=representation"),
        )
        if response.status_code != 200:
            raise data_unavailable()
        rows = self._json_rows(response)
        if not rows:
            raise AppError(404, "MEAL_NOT_FOUND", "Meal not found.")
        photo_path = rows[0].get("photo_path")
        if isinstance(photo_path, str) and photo_path:
            storage_response = await self._send(
                "DELETE",
                f"/storage/v1/object/meal-images/{photo_path}",
                headers=self._headers(token),
            )
            if storage_response.status_code not in {200, 204, 404}:
                raise data_unavailable()

    @staticmethod
    def _validate_photo_path(user_id: str, photo_path: str | None) -> None:
        if photo_path is not None and (
            ".." in photo_path.split("/") or not photo_path.startswith(f"{user_id}/")
        ):
            raise AppError(422, "PHOTO_PATH_INVALID", "Photo path must belong to the current user.")

    @staticmethod
    def _json_rows(response: httpx.Response) -> list[dict[str, Any]]:
        try:
            rows = response.json()
        except json.JSONDecodeError:
            raise data_unavailable() from None
        if not isinstance(rows, list) or any(not isinstance(row, dict) for row in rows):
            raise data_unavailable()
        return rows

    @staticmethod
    def _meal_from_row(row: dict[str, Any]) -> MealResponse:
        try:
            foods = row.get("foods_json")
            if isinstance(foods, dict):
                foods = foods.get("foods")
            nutrition = Nutrition(
                calories=row.get("calories"),
                protein_g=row.get("protein_g"),
                carbs_g=row.get("carbs_g"),
                fat_g=row.get("fat_g"),
                fiber_g=row.get("fiber_g"),
                sugars_g=row.get("sugars_g"),
                sodium_mg=row.get("sodium_mg"),
            )
            corrections = MealCorrections.model_validate(row.get("corrections_json") or {})
            data = {
                "id": row.get("id"),
                "foods": foods,
                "nutrition": nutrition.model_dump(mode="json"),
                "confidence": row.get("confidence"),
                "observations": row.get("observations") or [],
                "status": row.get("status"),
                "photo_path": row.get("photo_path"),
                "corrections": corrections.model_dump(mode="json"),
                "created_at": row.get("created_at"),
                "updated_at": row.get("updated_at"),
            }
            return MealResponse.model_validate_json(json.dumps(data))
        except (TypeError, ValueError):
            raise data_unavailable() from None
