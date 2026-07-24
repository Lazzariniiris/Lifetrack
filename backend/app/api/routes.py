from uuid import UUID

from fastapi import APIRouter, Depends, File, Form, Query, Response, UploadFile, status

from app.api.dependencies import AuthenticatedUser, current_user, supabase_gateway
from app.core.config import settings
from app.core.errors import AppError
from app.domain.contracts import (
    AnalysisResponse,
    MealAnalysisProvider,
    MealCreate,
    MealPage,
    MealResponse,
    MealUpdate,
)
from app.infrastructure.provider_factory import meal_analysis_provider
from app.infrastructure.supabase_gateway import SupabaseGateway


router = APIRouter()
ALLOWED_IMAGE_TYPES = {"image/jpeg", "image/png", "image/webp"}


def _has_valid_signature(payload: bytes, mime_type: str) -> bool:
    signatures = {
        "image/jpeg": payload.startswith(b"\xff\xd8\xff"),
        "image/png": payload.startswith(b"\x89PNG\r\n\x1a\n"),
        "image/webp": len(payload) >= 12 and payload[:4] == b"RIFF" and payload[8:12] == b"WEBP",
    }
    return signatures[mime_type]


@router.post("/meals/analyze", response_model=AnalysisResponse)
async def analyze_meal(
    consent: bool = Form(...),
    image: UploadFile = File(...),
    user: AuthenticatedUser = Depends(current_user),
    provider: MealAnalysisProvider = Depends(meal_analysis_provider),
    gateway: SupabaseGateway = Depends(supabase_gateway),
) -> AnalysisResponse:
    if not consent:
        raise AppError(400, "IMAGE_CONSENT_REQUIRED", "Image processing consent is required.")
    mime_type = (image.content_type or "").lower()
    if mime_type not in ALLOWED_IMAGE_TYPES:
        raise AppError(415, "IMAGE_TYPE_UNSUPPORTED", "Use a JPEG, PNG, or WebP image.")
    try:
        payload = await image.read(settings.max_image_bytes + 1)
    finally:
        await image.close()
    if not payload:
        raise AppError(400, "IMAGE_EMPTY", "The uploaded image is empty.")
    if len(payload) > settings.max_image_bytes:
        raise AppError(413, "IMAGE_TOO_LARGE", "The uploaded image exceeds the size limit.")
    if not _has_valid_signature(payload, mime_type):
        raise AppError(415, "IMAGE_CONTENT_INVALID", "The file content does not match its image type.")
    await gateway.consume_analysis_quota(user.token)
    result = await provider.analyze(payload, mime_type)
    return AnalysisResponse(**result.model_dump())


@router.post("/meals", response_model=MealResponse, status_code=status.HTTP_201_CREATED)
async def save_meal(
    meal: MealCreate,
    user: AuthenticatedUser = Depends(current_user),
    gateway: SupabaseGateway = Depends(supabase_gateway),
) -> MealResponse:
    return await gateway.save_analysis(user.user_id, user.token, meal)


@router.get("/meals", response_model=MealPage)
async def list_meals(
    limit: int = Query(default=20, ge=1, le=100),
    offset: int = Query(default=0, ge=0, le=100_000),
    user: AuthenticatedUser = Depends(current_user),
    gateway: SupabaseGateway = Depends(supabase_gateway),
) -> MealPage:
    return await gateway.list_meals(user.user_id, user.token, limit, offset)


@router.patch("/meals/{meal_id}", response_model=MealResponse)
async def update_meal(
    meal_id: UUID,
    update: MealUpdate,
    user: AuthenticatedUser = Depends(current_user),
    gateway: SupabaseGateway = Depends(supabase_gateway),
) -> MealResponse:
    return await gateway.update_meal(meal_id, user.user_id, user.token, update)


@router.delete("/meals/{meal_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_meal(
    meal_id: UUID,
    user: AuthenticatedUser = Depends(current_user),
    gateway: SupabaseGateway = Depends(supabase_gateway),
) -> Response:
    await gateway.delete_meal(meal_id, user.user_id, user.token)
    return Response(status_code=status.HTTP_204_NO_CONTENT)
