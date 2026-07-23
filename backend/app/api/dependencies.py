import httpx
from fastapi import Depends, HTTPException
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from app.core.config import settings

bearer = HTTPBearer()
class AuthenticatedUser:
    def __init__(self, user_id: str, token: str): self.user_id, self.token = user_id, token
async def current_user(credentials: HTTPAuthorizationCredentials = Depends(bearer)) -> AuthenticatedUser:
    async with httpx.AsyncClient(timeout=10) as client:
        response = await client.get(f"{settings.supabase_url}/auth/v1/user", headers={"apikey": settings.supabase_anon_key, "Authorization": f"Bearer {credentials.credentials}"})
    if response.status_code != 200: raise HTTPException(401, "Invalid or expired Supabase token")
    return AuthenticatedUser(response.json()["id"], credentials.credentials)
