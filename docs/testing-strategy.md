# Testing Strategy

The initial suite focuses on deterministic domain rules:

- Habit name and target validation.
- Hydration volume validation.
- Sleep duration validation across midnight.
- Weighted daily progress calculation.

Run unit tests with `./gradlew :app:testDebugUnitTest`. The next testing increment should add Room migration tests, repository integration tests and Compose UI tests for creation, validation and navigation flows.

The optional backend has an isolated pytest suite with mocked OpenAI and Supabase boundaries. It covers JWT handling, image MIME/signature and size validation, provider failures, client-UUID idempotency, owned CRUD behavior, pagination and readiness without external calls.

```powershell
Set-Location backend
uv sync --group dev
uv run pytest
```
