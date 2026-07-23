import base64, json
from openai import AsyncOpenAI
from app.core.config import settings
from app.domain.contracts import Analysis

class OpenAIVisionProvider:
    async def analyze(self, image: bytes, mime_type: str) -> Analysis:
        schema = Analysis.model_json_schema()
        client = AsyncOpenAI(api_key=settings.openai_api_key)
        response = await client.responses.create(
            model=settings.openai_model,
            input=[{"role":"user","content":[
                {"type":"input_text","text":"Identify visible foods and estimate portions and nutrition. Return estimates only, never medical advice."},
                {"type":"input_image","image_url":f"data:{mime_type};base64,{base64.b64encode(image).decode()}"}
            ]}],
            text={"format":{"type":"json_schema","name":"meal_analysis","schema":schema,"strict":True}},
        )
        return Analysis.model_validate(json.loads(response.output_text))
