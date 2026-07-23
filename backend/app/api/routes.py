import uuid
import httpx
from fastapi import APIRouter, Depends, File, Form, HTTPException, UploadFile
from pydantic import BaseModel, EmailStr
from app.api.dependencies import current_user, AuthenticatedUser
from app.core.config import settings
from app.infrastructure.provider_factory import meal_analysis_provider
from app.infrastructure.supabase_gateway import SupabaseGateway
from app.domain.contracts import Analysis

router = APIRouter()
class Credentials(BaseModel): email: EmailStr; password: str
@router.post("/auth/register")
async def register(body: Credentials):
    async with httpx.AsyncClient(timeout=10) as client:
        response = await client.post(f"{settings.supabase_url}/auth/v1/signup", json=body.model_dump(), headers={"apikey": settings.supabase_anon_key})
    if response.status_code >= 400: raise HTTPException(response.status_code, response.text)
    return response.json()
@router.post("/auth/login")
async def login(body: Credentials):
    async with httpx.AsyncClient(timeout=10) as client:
        response = await client.post(f"{settings.supabase_url}/auth/v1/token?grant_type=password", json=body.model_dump(), headers={"apikey": settings.supabase_anon_key})
    if response.status_code >= 400: raise HTTPException(401, "Invalid credentials")
    return response.json()
@router.post("/meals/analyze")
async def analyze_meal(consent: bool = Form(...), image: UploadFile = File(...), current_user: AuthenticatedUser = Depends(current_user)):
    if not consent: raise HTTPException(400, "Explicit image processing consent is required")
    if image.content_type not in {"image/jpeg", "image/png", "image/webp"}: raise HTTPException(415, "Unsupported image type")
    payload = await image.read(settings.max_image_bytes + 1)
    if len(payload) > settings.max_image_bytes: raise HTTPException(413, "Image too large")
    result = await meal_analysis_provider().analyze(payload, image.content_type)
    return result

@router.post("/meals")
async def save_meal(result: Analysis, current_user: AuthenticatedUser = Depends(current_user)):
    analysis_id = str(uuid.uuid4())
    await SupabaseGateway().save_analysis(analysis_id, current_user.user_id, current_user.token, result)
    return {"id": analysis_id, **result.model_dump()}
