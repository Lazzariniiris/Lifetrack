import logging
import re
import time
import uuid

from fastapi import Depends, FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from starlette.exceptions import HTTPException as StarletteHTTPException

from app.api.dependencies import supabase_gateway
from app.api.routes import router
from app.core.config import settings
from app.core.errors import AppError
from app.core.logging import configure_logging, request_id_context
from app.infrastructure.supabase_gateway import SupabaseGateway


configure_logging()
logger = logging.getLogger("lifetrack.api")
REQUEST_ID_PATTERN = re.compile(r"^[A-Za-z0-9._-]{1,64}$")

app = FastAPI(title="LifeTrack API", version="0.2.0", docs_url="/docs", redoc_url="/redoc")
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.allowed_origins,
    allow_credentials=False,
    allow_methods=["GET", "POST", "PATCH", "DELETE", "OPTIONS"],
    allow_headers=["Authorization", "Content-Type", "X-Request-ID"],
    expose_headers=["X-Request-ID"],
)


def error_response(request: Request, status_code: int, code: str, message: str) -> JSONResponse:
    request_id = getattr(request.state, "request_id", request_id_context.get())
    return JSONResponse(
        status_code=status_code,
        content={"error": {"code": code, "message": message, "request_id": request_id}},
        headers={"X-Request-ID": request_id},
    )


@app.middleware("http")
async def request_context(request: Request, call_next):
    supplied_id = request.headers.get("X-Request-ID", "")
    request_id = supplied_id if REQUEST_ID_PATTERN.fullmatch(supplied_id) else str(uuid.uuid4())
    request.state.request_id = request_id
    token = request_id_context.set(request_id)
    started = time.perf_counter()
    try:
        response = await call_next(request)
        response.headers["X-Request-ID"] = request_id
        return response
    finally:
        logger.info(
            "request_completed",
            extra={
                "method": request.method,
                "path": request.url.path,
                "status_code": locals().get("response").status_code if "response" in locals() else 500,
                "duration_ms": round((time.perf_counter() - started) * 1_000, 2),
            },
        )
        request_id_context.reset(token)


@app.exception_handler(AppError)
async def app_error_handler(request: Request, exc: AppError) -> JSONResponse:
    return error_response(request, exc.status_code, exc.code, exc.message)


@app.exception_handler(RequestValidationError)
async def validation_error_handler(request: Request, exc: RequestValidationError) -> JSONResponse:
    del exc
    return error_response(request, 422, "VALIDATION_ERROR", "The request data is invalid.")


@app.exception_handler(StarletteHTTPException)
async def http_error_handler(request: Request, exc: StarletteHTTPException) -> JSONResponse:
    messages = {404: "Resource not found.", 405: "Method not allowed."}
    return error_response(
        request,
        exc.status_code,
        "HTTP_ERROR",
        messages.get(exc.status_code, "The request could not be completed."),
    )


@app.exception_handler(Exception)
async def unexpected_error_handler(request: Request, exc: Exception) -> JSONResponse:
    logger.error("unhandled_exception", extra={"exception_type": type(exc).__name__})
    return error_response(request, 500, "INTERNAL_ERROR", "An unexpected error occurred.")


@app.get("/health/live")
async def liveness() -> dict[str, str]:
    return {"status": "ok"}


@app.get("/health/ready")
async def readiness(gateway: SupabaseGateway = Depends(supabase_gateway)) -> dict[str, str]:
    if not await gateway.is_ready():
        raise AppError(503, "NOT_READY", "The service is not ready.")
    return {"status": "ready", "supabase": "ok", "openai": "configured"}


app.include_router(router, prefix="/v1")
