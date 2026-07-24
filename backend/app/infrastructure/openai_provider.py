import base64

import httpx
from openai import APIConnectionError, APIStatusError, APITimeoutError, AsyncOpenAI, OpenAIError
from pydantic import ValidationError

from app.core.config import settings
from app.core.errors import provider_invalid_response, provider_unavailable
from app.domain.contracts import GeneratedAnalysis


class OpenAIVisionProvider:
    async def analyze(self, image: bytes, mime_type: str) -> GeneratedAnalysis:
        timeout = httpx.Timeout(settings.openai_timeout_seconds)
        try:
            async with AsyncOpenAI(
                api_key=settings.openai_api_key.get_secret_value(),
                timeout=timeout,
                max_retries=1,
            ) as client:
                response = await client.responses.parse(
                    model=settings.openai_model,
                    instructions=(
                        "Analyze only the visible meal. Return cautious nutrition estimates, never "
                        "medical advice. Supply every schema field; use empty lists when there are no "
                        "alternatives or observations. Confidence values are probabilities from 0 to 1."
                    ),
                    input=[
                        {
                            "role": "user",
                            "content": [
                                {
                                    "type": "input_text",
                                    "text": (
                                        "Identify visible foods, likely ingredients, estimated portions "
                                        "and grams, plausible alternatives, observations, and aggregate "
                                        "calories, protein, carbohydrates, fat, fiber, sugars, and sodium."
                                    ),
                                },
                                {
                                    "type": "input_image",
                                    "image_url": (
                                        f"data:{mime_type};base64,"
                                        f"{base64.b64encode(image).decode('ascii')}"
                                    ),
                                    "detail": "auto",
                                },
                            ],
                        }
                    ],
                    text_format=GeneratedAnalysis,
                    max_output_tokens=2_500,
                    store=False,
                    timeout=timeout,
                )
        except (APITimeoutError, APIConnectionError):
            raise provider_unavailable() from None
        except APIStatusError as exc:
            if exc.status_code == 429 or exc.status_code >= 500:
                raise provider_unavailable() from None
            raise provider_invalid_response() from None
        except (OpenAIError, ValidationError):
            raise provider_invalid_response() from None

        if response.output_parsed is None:
            raise provider_invalid_response()
        return response.output_parsed
