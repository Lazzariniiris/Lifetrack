from app.domain.contracts import MealAnalysisProvider
from app.infrastructure.openai_provider import OpenAIVisionProvider

def meal_analysis_provider() -> MealAnalysisProvider:
    return OpenAIVisionProvider()
