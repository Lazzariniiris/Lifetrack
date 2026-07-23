import logging
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.api.routes import router
from app.core.config import settings

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s %(message)s")
app = FastAPI(title="LifeTrack API", version="0.1.0", docs_url="/docs", redoc_url="/redoc")
app.add_middleware(CORSMiddleware, allow_origins=[x for x in settings.cors_origins.split(",") if x], allow_credentials=False, allow_methods=["POST"], allow_headers=["Authorization","Content-Type"])
@app.get("/health")
async def health(): return {"status":"ok"}
app.include_router(router, prefix="/v1")
