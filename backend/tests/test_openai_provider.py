from types import SimpleNamespace

import httpx
import pytest
from openai import APITimeoutError

from app.core.errors import AppError
from app.domain.contracts import GeneratedAnalysis
from app.infrastructure import openai_provider
from app.infrastructure.openai_provider import OpenAIVisionProvider


def analysis() -> GeneratedAnalysis:
    return GeneratedAnalysis(
        foods=[
            {
                "name": "Soup",
                "ingredients": ["vegetables"],
                "estimated_portion": "one bowl",
                "estimated_grams": 300.0,
                "confidence": 0.75,
                "alternatives": [],
            }
        ],
        nutrition={
            "calories": 220.0,
            "protein_g": 8.0,
            "carbs_g": 35.0,
            "fat_g": 6.0,
            "fiber_g": 7.0,
            "sugars_g": 9.0,
            "sodium_mg": 700.0,
        },
        confidence=0.75,
        observations=[],
    )


class FakeOpenAIClient:
    def __init__(self, parse_result=None, error: Exception | None = None) -> None:
        self.parse_result = parse_result
        self.error = error
        self.kwargs = None
        self.responses = self

    async def __aenter__(self):
        return self

    async def __aexit__(self, exc_type, exc, traceback):
        return False

    async def parse(self, **kwargs):
        self.kwargs = kwargs
        if self.error:
            raise self.error
        return SimpleNamespace(output_parsed=self.parse_result)


@pytest.mark.asyncio
async def test_openai_uses_typed_parse_timeout_and_no_disclaimer(monkeypatch: pytest.MonkeyPatch):
    client = FakeOpenAIClient(parse_result=analysis())
    constructor_kwargs = {}

    def fake_constructor(**kwargs):
        constructor_kwargs.update(kwargs)
        return client

    monkeypatch.setattr(openai_provider, "AsyncOpenAI", fake_constructor)

    result = await OpenAIVisionProvider().analyze(b"image", "image/jpeg")

    assert result.confidence == 0.75
    assert client.kwargs["text_format"] is GeneratedAnalysis
    assert isinstance(client.kwargs["timeout"], httpx.Timeout)
    assert isinstance(constructor_kwargs["timeout"], httpx.Timeout)
    assert client.kwargs["store"] is False
    assert "disclaimer" not in GeneratedAnalysis.model_json_schema()["properties"]
    assert GeneratedAnalysis.model_json_schema()["additionalProperties"] is False


@pytest.mark.asyncio
async def test_openai_timeout_maps_to_controlled_error(monkeypatch: pytest.MonkeyPatch):
    request = httpx.Request("POST", "https://api.openai.com/v1/responses")
    client = FakeOpenAIClient(error=APITimeoutError(request))
    monkeypatch.setattr(openai_provider, "AsyncOpenAI", lambda **kwargs: client)

    with pytest.raises(AppError) as error:
        await OpenAIVisionProvider().analyze(b"image", "image/jpeg")

    assert error.value.status_code == 503
    assert error.value.code == "ANALYSIS_UNAVAILABLE"
    assert "openai" not in error.value.message.lower()
