from pydantic import BaseModel, Field

class Food(BaseModel): name: str; estimated_portion: str
class Nutrition(BaseModel): calories: float = Field(ge=0); protein_g: float = Field(ge=0); carbs_g: float = Field(ge=0); fat_g: float = Field(ge=0)
class Analysis(BaseModel): foods: list[Food]; nutrition: Nutrition; confidence: str; disclaimer: str
