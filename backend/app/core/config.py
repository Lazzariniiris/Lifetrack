from pydantic import Field, SecretStr, field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    supabase_url: str
    supabase_anon_key: SecretStr
    openai_api_key: SecretStr
    openai_model: str = Field(default="gpt-4.1-mini", min_length=1, max_length=100)
    cors_origins: str = ""
    max_image_bytes: int = Field(default=8_000_000, ge=1_024, le=20_000_000)
    supabase_timeout_seconds: float = Field(default=8.0, gt=0, le=30)
    openai_timeout_seconds: float = Field(default=30.0, gt=0, le=120)
    daily_analysis_limit: int = Field(default=50, ge=1, le=1_000)

    @field_validator("supabase_url")
    @classmethod
    def validate_supabase_url(cls, value: str) -> str:
        value = value.strip().rstrip("/")
        if not value.startswith(("https://", "http://localhost", "http://127.0.0.1")):
            raise ValueError("SUPABASE_URL must use HTTPS")
        return value

    @property
    def allowed_origins(self) -> list[str]:
        return [origin.strip() for origin in self.cors_origins.split(",") if origin.strip()]


settings = Settings()
