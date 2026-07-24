from datetime import datetime
from typing import Annotated, Literal, Protocol
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field, model_validator


DISCLAIMER = (
    "Nutrition values are visual estimates for general tracking only; "
    "they are not medical or dietary advice."
)

FoodName = Annotated[str, Field(min_length=1, max_length=120)]
Ingredient = Annotated[str, Field(min_length=1, max_length=100)]
Observation = Annotated[str, Field(min_length=1, max_length=300)]
PhotoPath = Annotated[str, Field(min_length=3, max_length=512, pattern=r"^[^\\]+$")]
Confidence = Annotated[float, Field(ge=0, le=1)]


class StrictModel(BaseModel):
    model_config = ConfigDict(extra="forbid", strict=True, str_strip_whitespace=True)


class Food(StrictModel):
    name: FoodName
    ingredients: list[Ingredient] = Field(min_length=0, max_length=20)
    estimated_portion: Annotated[str, Field(min_length=1, max_length=120)]
    estimated_grams: float = Field(gt=0, le=5_000)
    confidence: Confidence
    alternatives: list[FoodName] = Field(min_length=0, max_length=5)


class Nutrition(StrictModel):
    calories: float = Field(ge=0, le=20_000)
    protein_g: float = Field(ge=0, le=5_000)
    carbs_g: float = Field(ge=0, le=5_000)
    fat_g: float = Field(ge=0, le=5_000)
    fiber_g: float = Field(ge=0, le=1_000)
    sugars_g: float = Field(ge=0, le=5_000)
    sodium_mg: float = Field(ge=0, le=100_000)


class GeneratedAnalysis(StrictModel):
    foods: list[Food] = Field(min_length=1, max_length=25)
    nutrition: Nutrition
    confidence: Confidence
    observations: list[Observation] = Field(min_length=0, max_length=10)


class AnalysisResponse(GeneratedAnalysis):
    disclaimer: Literal[
        "Nutrition values are visual estimates for general tracking only; they are not medical or dietary advice."
    ] = DISCLAIMER


class MealCreate(GeneratedAnalysis):
    id: UUID
    photo_path: PhotoPath | None = None


class MealUpdate(StrictModel):
    foods: list[Food] | None = Field(default=None, min_length=1, max_length=25)
    nutrition: Nutrition | None = None
    confidence: Confidence | None = None
    observations: list[Observation] | None = Field(default=None, min_length=0, max_length=10)
    photo_path: PhotoPath | None = None
    correction_note: Annotated[str, Field(min_length=1, max_length=1_000)] | None = None

    @model_validator(mode="after")
    def require_change(self) -> "MealUpdate":
        if not self.model_fields_set:
            raise ValueError("At least one field must be provided")
        if "photo_path" not in self.model_fields_set and all(
            getattr(self, name) is None for name in self.model_fields_set
        ):
            raise ValueError("At least one non-null correction must be provided")
        return self


class MealCorrections(StrictModel):
    foods: list[Food] | None = Field(default=None, min_length=1, max_length=25)
    nutrition: Nutrition | None = None
    confidence: Confidence | None = None
    observations: list[Observation] | None = Field(default=None, min_length=0, max_length=10)
    note: Annotated[str, Field(min_length=1, max_length=1_000)] | None = None


class MealResponse(AnalysisResponse):
    id: UUID
    status: Literal["completed", "corrected", "failed"]
    photo_path: PhotoPath | None
    corrections: MealCorrections
    created_at: datetime
    updated_at: datetime


class MealPage(StrictModel):
    items: list[MealResponse]
    limit: int = Field(ge=1, le=100)
    offset: int = Field(ge=0)
    total: int = Field(ge=0)


class MealAnalysisProvider(Protocol):
    async def analyze(self, image: bytes, mime_type: str) -> GeneratedAnalysis: ...
