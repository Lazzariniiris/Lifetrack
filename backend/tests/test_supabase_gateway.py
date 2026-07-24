from collections import Counter
from uuid import UUID

import httpx
import pytest

from app.core.errors import AppError
from app.domain.contracts import MealCreate, MealUpdate
from app.infrastructure.supabase_gateway import SupabaseGateway


USER_ID = "11111111-1111-4111-8111-111111111111"
MEAL_ID = UUID("22222222-2222-4222-8222-222222222222")


def meal_create() -> MealCreate:
    return MealCreate(
        id=MEAL_ID,
        foods=[
            {
                "name": "Salad",
                "ingredients": ["lettuce"],
                "estimated_portion": "one plate",
                "estimated_grams": 250.0,
                "confidence": 0.9,
                "alternatives": [],
            }
        ],
        nutrition={
            "calories": 300.0,
            "protein_g": 12.0,
            "carbs_g": 30.0,
            "fat_g": 14.0,
            "fiber_g": 9.0,
            "sugars_g": 6.0,
            "sodium_mg": 450.0,
        },
        confidence=0.88,
        observations=[],
        photo_path=f"{USER_ID}/meal.webp",
    )


def meal_row(*, corrected: bool = False) -> dict:
    meal = meal_create()
    return {
        "id": str(meal.id),
        "foods_json": [food.model_dump(mode="json") for food in meal.foods],
        **meal.nutrition.model_dump(mode="json"),
        "confidence": 0.7 if corrected else meal.confidence,
        "observations": meal.observations,
        "status": "corrected" if corrected else "completed",
        "photo_path": meal.photo_path,
        "corrections_json": {"confidence": 0.7, "note": "Adjusted manually"} if corrected else {},
        "created_at": "2026-07-24T10:00:00Z",
        "updated_at": "2026-07-24T10:05:00Z",
    }


@pytest.mark.asyncio
async def test_auth_maps_invalid_token_without_provider_details():
    async def handler(request: httpx.Request) -> httpx.Response:
        assert request.headers["authorization"] == "Bearer bad-token"
        return httpx.Response(401, json={"message": "sensitive provider detail"})

    async with httpx.AsyncClient(
        transport=httpx.MockTransport(handler), base_url="https://test.supabase.co"
    ) as client:
        gateway = SupabaseGateway(client)
        with pytest.raises(AppError) as error:
            await gateway.authenticate("bad-token")

    assert error.value.code == "AUTH_INVALID"
    assert "sensitive" not in error.value.message


@pytest.mark.asyncio
async def test_auth_timeout_maps_to_unavailable():
    async def handler(request: httpx.Request) -> httpx.Response:
        raise httpx.ReadTimeout("provider timeout detail", request=request)

    async with httpx.AsyncClient(
        transport=httpx.MockTransport(handler), base_url="https://test.supabase.co"
    ) as client:
        with pytest.raises(AppError) as error:
            await SupabaseGateway(client).authenticate("token")

    assert error.value.status_code == 503
    assert error.value.code == "AUTH_UNAVAILABLE"


@pytest.mark.asyncio
async def test_client_uuid_is_idempotent_and_returns_original_row():
    calls = Counter()

    async def handler(request: httpx.Request) -> httpx.Response:
        calls[request.method] += 1
        assert request.headers["authorization"] == "Bearer user-token"
        if request.method == "POST":
            assert request.url.params["on_conflict"] == "id"
            assert "resolution=merge-duplicates" in request.headers["prefer"]
            return httpx.Response(201)
        assert request.url.params["id"] == f"eq.{MEAL_ID}"
        assert request.url.params["user_id"] == f"eq.{USER_ID}"
        return httpx.Response(200, json=[meal_row()])

    async with httpx.AsyncClient(
        transport=httpx.MockTransport(handler), base_url="https://test.supabase.co"
    ) as client:
        gateway = SupabaseGateway(client)
        first = await gateway.save_analysis(USER_ID, "user-token", meal_create())
        second = await gateway.save_analysis(USER_ID, "user-token", meal_create())

    assert first.id == second.id == MEAL_ID
    assert calls == Counter({"POST": 2, "GET": 2})


@pytest.mark.asyncio
async def test_list_patch_delete_use_owner_filters_and_rls_token():
    calls = Counter()

    async def handler(request: httpx.Request) -> httpx.Response:
        calls[request.method] += 1
        assert request.headers["authorization"] == "Bearer user-token"
        assert request.url.params["user_id"] == f"eq.{USER_ID}"
        if request.method == "GET":
            if request.url.params.get("select") == "photo_path":
                return httpx.Response(200, json=[{"photo_path": None}])
            assert request.url.params["limit"] == "10"
            assert request.url.params["offset"] == "20"
            return httpx.Response(200, json=[meal_row()], headers={"Content-Range": "20-20/21"})
        if request.method == "PATCH":
            assert request.url.params["id"] == f"eq.{MEAL_ID}"
            body = __import__("json").loads(request.content)
            assert body["status"] == "corrected"
            assert body["corrections_json"]["note"] == "Adjusted manually"
            return httpx.Response(200, json=[meal_row(corrected=True)])
        assert request.url.params["id"] == f"eq.{MEAL_ID}"
        return httpx.Response(200, json=[{"id": str(MEAL_ID)}])

    async with httpx.AsyncClient(
        transport=httpx.MockTransport(handler), base_url="https://test.supabase.co"
    ) as client:
        gateway = SupabaseGateway(client)
        page = await gateway.list_meals(USER_ID, "user-token", limit=10, offset=20)
        updated = await gateway.update_meal(
            MEAL_ID,
            USER_ID,
            "user-token",
            MealUpdate(confidence=0.7, correction_note="Adjusted manually"),
        )
        await gateway.delete_meal(MEAL_ID, USER_ID, "user-token")

    assert page.total == 21
    assert page.items[0].id == MEAL_ID
    assert updated.status == "corrected"
    assert updated.corrections.note == "Adjusted manually"
    assert calls == Counter({"GET": 2, "PATCH": 1, "DELETE": 1})


@pytest.mark.asyncio
async def test_delete_hidden_row_is_not_found():
    async def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(200, json=[])

    async with httpx.AsyncClient(
        transport=httpx.MockTransport(handler), base_url="https://test.supabase.co"
    ) as client:
        with pytest.raises(AppError) as error:
            await SupabaseGateway(client).delete_meal(MEAL_ID, USER_ID, "user-token")

    assert error.value.status_code == 404
    assert error.value.code == "MEAL_NOT_FOUND"


@pytest.mark.asyncio
async def test_readiness_is_a_single_unauthenticated_postgrest_check():
    requests = []

    async def handler(request: httpx.Request) -> httpx.Response:
        requests.append(request)
        assert "authorization" not in request.headers
        return httpx.Response(200, json=[])

    async with httpx.AsyncClient(
        transport=httpx.MockTransport(handler), base_url="https://test.supabase.co"
    ) as client:
        ready = await SupabaseGateway(client).is_ready()

    assert ready is True
    assert len(requests) == 1
    assert requests[0].url.path == "/rest/v1/meal_analyses"
