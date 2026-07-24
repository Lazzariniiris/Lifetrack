# Technical Manual

## Architecture

LifeTrack is an Android application built with Kotlin, Jetpack Compose and Material 3. It uses Hilt for dependency injection, Room for local tracking, DataStore for preferences, WorkManager for reliable background work, Supabase for authentication/profile/storage and FastAPI as the authenticated AI boundary.

The application is local-first for habits, hydration and sleep. Meal photos and confirmed meal history are account-scoped cloud data. The UI must not describe the local tracking modules as synchronized.

## Persistence

Room schema version 3 stores tracking data plus an owner-scoped meal queue and meal history. Migration `2 -> 3` isolates meal records by Supabase user, preserves legacy rows under a non-cloud owner and keeps local images until a result is durable or the user explicitly deletes the pending item.

Preferences are stored with DataStore. Access and refresh tokens are encrypted with an Android Keystore AES-GCM key before being written. Android backup and device transfer are disabled for database, preferences and internal files.

## Authentication

Android authenticates directly with Supabase Auth. Registration supports email verification, login validates/refreshes sessions, and recovery returns through `lifetrack://auth/recovery`. That URI must remain in the Supabase redirect allow-list. Profiles are created by the database trigger and read/updated under RLS.

## Meal processing

Before AI analysis, Android normalizes the image, creates an owner-scoped local queue item, uploads the image to the private `meal-images` bucket and inserts an idempotent remote `pending` row. WorkManager retries transient failures with exponential backoff. Permanent failures remain visible and retain the local photo until the user retries or deletes them.

FastAPI verifies JWTs, consent, MIME signatures and image size. OpenAI Structured Outputs uses strict Pydantic models. Results include numeric confidence, grams, alternatives and observations. Confidence below 70% requires explicit user review before saving. The client UUID is reused to merge the pending row into a completed result without duplicate analysis records.

## Deployment

Apply SQL migrations `001`, `002` and `003` in order. Release Android builds require `SUPABASE_URL`, `SUPABASE_ANON_KEY` and `LIFETRACK_API_URL`. Backend readiness is `/health/ready`; liveness is `/health/live`.

Never commit Supabase tokens, OpenAI keys or signing credentials. Production deployment also requires a valid OpenAI key, a deployed HTTPS FastAPI URL and the recovery redirect configured in Supabase.
