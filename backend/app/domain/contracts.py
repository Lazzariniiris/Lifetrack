from typing import Protocol
from pydantic import BaseModel, Field

class Food(BaseModel): name: str; ingredients: list[str]; estimated_portion: str
class Nutrition(BaseModel):
    calories: float = Field(ge=0)
    protein_g: float = Field(ge=0)
    carbs_g: float = Field(ge=0)
    fat_g: float = Field(ge=0)
    fiber_g: float = Field(ge=0)
    sugars_g: float = Field(ge=0)
    sodium_mg: float = Field(ge=0)
class Analysis(BaseModel): foods: list[Food]; nutrition: Nutrition; confidence: str; disclaimer: str

class MealAnalysisProvider(Protocol):
    async def analyze(self, image: bytes, mime_type: str) -> Analysis: ...
