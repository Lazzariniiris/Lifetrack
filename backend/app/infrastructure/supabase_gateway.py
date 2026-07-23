import httpx
from app.core.config import settings
from app.domain.contracts import Analysis

class SupabaseGateway:
    async def save_analysis(self, analysis_id: str, user_id: str, token: str, result: Analysis) -> None:
        payload = {"id": analysis_id, "user_id": user_id, "foods_json": result.model_dump(mode="json"), "calories": result.nutrition.calories, "protein_g": result.nutrition.protein_g, "carbs_g": result.nutrition.carbs_g, "fat_g": result.nutrition.fat_g}
        headers = {"apikey": settings.supabase_anon_key, "Authorization": f"Bearer {token}", "Content-Type": "application/json", "Prefer": "return=minimal"}
        async with httpx.AsyncClient(timeout=10) as client:
            response = await client.post(f"{settings.supabase_url}/rest/v1/meal_analyses", json=payload, headers=headers)
        response.raise_for_status()
