from dataclasses import dataclass

from fastapi import Depends
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from app.core.errors import unauthorized
from app.infrastructure.supabase_gateway import SupabaseGateway


bearer = HTTPBearer(auto_error=False)


@dataclass(frozen=True, slots=True)
class AuthenticatedUser:
    user_id: str
    token: str


def supabase_gateway() -> SupabaseGateway:
    return SupabaseGateway()


async def current_user(
    credentials: HTTPAuthorizationCredentials | None = Depends(bearer),
    gateway: SupabaseGateway = Depends(supabase_gateway),
) -> AuthenticatedUser:
    if credentials is None or credentials.scheme.lower() != "bearer":
        raise unauthorized()
    user_id, token = await gateway.authenticate(credentials.credentials)
    return AuthenticatedUser(user_id, token)
