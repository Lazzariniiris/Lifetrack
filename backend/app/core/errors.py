from dataclasses import dataclass


@dataclass(slots=True)
class AppError(Exception):
    status_code: int
    code: str
    message: str


def unauthorized() -> AppError:
    return AppError(401, "AUTH_INVALID", "Authentication is invalid or expired.")


def auth_unavailable() -> AppError:
    return AppError(503, "AUTH_UNAVAILABLE", "Authentication is temporarily unavailable.")


def data_unavailable() -> AppError:
    return AppError(503, "DATA_UNAVAILABLE", "Meal data is temporarily unavailable.")


def provider_unavailable() -> AppError:
    return AppError(503, "ANALYSIS_UNAVAILABLE", "Meal analysis is temporarily unavailable.")


def provider_invalid_response() -> AppError:
    return AppError(502, "ANALYSIS_INVALID_RESPONSE", "The analysis provider returned an invalid result.")
