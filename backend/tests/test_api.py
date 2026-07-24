from uuid import uuid4

import pytest
from fastapi.testclient import TestClient

from app.api.dependencies import AuthenticatedUser, current_user, supabase_gateway
from app.core.config import settings
from app.core.errors import provider_unavailable
from app.domain.contracts import GeneratedAnalysis
from app.infrastructure.provider_factory import meal_analysis_provider
from app.main import app


USER_ID = "11111111-1111-4111-8111-111111111111"


def generated_analysis() -> GeneratedAnalysis:
    return GeneratedAnalysis(
        foods=[
            {
                "name": "Rice bowl",
                "ingredients": ["rice", "vegetables"],
                "estimated_portion": "one bowl",
                "estimated_grams": 350.0,
                "confidence": 0.82,
                "alternatives": [],
            }
        ],
        nutrition={
            "calories": 500.0,
            "protein_g": 20.0,
            "carbs_g": 70.0,
            "fat_g": 15.0,
            "fiber_g": 8.0,
            "sugars_g": 7.0,
            "sodium_mg": 600.0,
        },
        confidence=0.8,
        observations=["Sauce quantity is uncertain."],
    )


class FakeProvider:
    def __init__(self) -> None:
        self.calls = 0

    async def analyze(self, image: bytes, mime_type: str) -> GeneratedAnalysis:
        self.calls += 1
        assert image.startswith(b"\xff\xd8\xff")
        assert mime_type == "image/jpeg"
        return generated_analysis()


class FakeGateway:
    async def consume_analysis_quota(self, token: str) -> None:
        assert token == "user-token"


@pytest.fixture
def client():
    app.dependency_overrides[current_user] = lambda: AuthenticatedUser(USER_ID, "user-token")
    app.dependency_overrides[supabase_gateway] = FakeGateway
    with TestClient(app, raise_server_exceptions=False) as test_client:
        yield test_client
    app.dependency_overrides.clear()


def test_auth_requires_bearer_and_returns_stable_error():
    with TestClient(app, raise_server_exceptions=False) as client:
        response = client.get("/v1/meals")

    assert response.status_code == 401
    assert response.json()["error"]["code"] == "AUTH_INVALID"
    assert response.json()["error"]["request_id"] == response.headers["X-Request-ID"]
    assert "detail" not in response.text.lower()


def test_framework_404_uses_stable_error_contract():
    with TestClient(app, raise_server_exceptions=False) as client:
        response = client.get("/does-not-exist")

    assert response.status_code == 404
    assert response.json()["error"]["code"] == "HTTP_ERROR"
    assert response.json()["error"]["message"] == "Resource not found."


def test_analyze_validates_mime_signature_and_server_disclaimer(client: TestClient):
    provider = FakeProvider()
    app.dependency_overrides[meal_analysis_provider] = lambda: provider

    unsupported = client.post(
        "/v1/meals/analyze",
        data={"consent": "true"},
        files={"image": ("meal.gif", b"GIF89a", "image/gif")},
    )
    spoofed = client.post(
        "/v1/meals/analyze",
        data={"consent": "true"},
        files={"image": ("meal.jpg", b"not-a-jpeg", "image/jpeg")},
    )
    valid = client.post(
        "/v1/meals/analyze",
        data={"consent": "true"},
        files={"image": ("meal.jpg", b"\xff\xd8\xffpayload", "image/jpeg")},
    )

    assert unsupported.status_code == 415
    assert unsupported.json()["error"]["code"] == "IMAGE_TYPE_UNSUPPORTED"
    assert spoofed.status_code == 415
    assert spoofed.json()["error"]["code"] == "IMAGE_CONTENT_INVALID"
    assert valid.status_code == 200
    assert valid.json()["confidence"] == 0.8
    assert "medical or dietary advice" in valid.json()["disclaimer"]
    assert provider.calls == 1


def test_analyze_rejects_size_before_provider(client: TestClient, monkeypatch: pytest.MonkeyPatch):
    provider = FakeProvider()
    app.dependency_overrides[meal_analysis_provider] = lambda: provider
    monkeypatch.setattr(settings, "max_image_bytes", 8)

    response = client.post(
        "/v1/meals/analyze",
        data={"consent": "true"},
        files={"image": ("meal.jpg", b"\xff\xd8\xff123456", "image/jpeg")},
    )

    assert response.status_code == 413
    assert response.json()["error"]["code"] == "IMAGE_TOO_LARGE"
    assert provider.calls == 0


def test_analysis_provider_failure_is_controlled(client: TestClient):
    class FailingProvider:
        async def analyze(self, image: bytes, mime_type: str):
            raise provider_unavailable()

    app.dependency_overrides[meal_analysis_provider] = FailingProvider
    response = client.post(
        "/v1/meals/analyze",
        data={"consent": "true"},
        files={"image": ("meal.jpg", b"\xff\xd8\xffpayload", "image/jpeg")},
    )

    assert response.status_code == 503
    assert response.json()["error"] == {
        "code": "ANALYSIS_UNAVAILABLE",
        "message": "Meal analysis is temporarily unavailable.",
        "request_id": response.headers["X-Request-ID"],
    }


def test_analysis_daily_limit_is_controlled(client: TestClient):
    class LimitedGateway:
        async def consume_analysis_quota(self, token: str) -> None:
            from app.core.errors import AppError
            raise AppError(429, "ANALYSIS_LIMIT_REACHED", "Daily meal analysis limit reached.")

    app.dependency_overrides[supabase_gateway] = LimitedGateway
    response = client.post(
        "/v1/meals/analyze",
        data={"consent": "true"},
        files={"image": ("meal.jpg", b"\xff\xd8\xffpayload", "image/jpeg")},
    )
    assert response.status_code == 429
    assert response.json()["error"]["code"] == "ANALYSIS_LIMIT_REACHED"


def test_models_forbid_client_disclaimer_and_out_of_range_confidence(client: TestClient):
    payload = generated_analysis().model_dump(mode="json")
    payload.update({"id": str(uuid4()), "disclaimer": "trust me", "confidence": 1.1})

    response = client.post("/v1/meals", json=payload)

    assert response.status_code == 422
    assert response.json()["error"]["code"] == "VALIDATION_ERROR"


def test_readiness_checks_supabase_only():
    class ReadyGateway:
        calls = 0

        async def is_ready(self) -> bool:
            self.calls += 1
            return True

    gateway = ReadyGateway()
    app.dependency_overrides[supabase_gateway] = lambda: gateway
    try:
        with TestClient(app, raise_server_exceptions=False) as client:
            response = client.get("/health/ready")
            live = client.get("/health/live")
    finally:
        app.dependency_overrides.clear()

    assert response.status_code == 200
    assert response.json() == {"status": "ready", "supabase": "ok", "openai": "configured"}
    assert live.json() == {"status": "ok"}
    assert gateway.calls == 1


def test_readiness_failure_has_stable_contract():
    class UnreadyGateway:
        async def is_ready(self) -> bool:
            return False

    app.dependency_overrides[supabase_gateway] = UnreadyGateway
    try:
        with TestClient(app, raise_server_exceptions=False) as client:
            response = client.get("/health/ready")
    finally:
        app.dependency_overrides.clear()

    assert response.status_code == 503
    assert response.json()["error"]["code"] == "NOT_READY"
