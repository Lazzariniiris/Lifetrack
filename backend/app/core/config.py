from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")
    supabase_url: str
    supabase_anon_key: str
    supabase_service_role_key: str
    openai_api_key: str
    openai_model: str = "gpt-4.1-mini"
    cors_origins: str = ""
    max_image_bytes: int = 8_000_000

settings = Settings()
