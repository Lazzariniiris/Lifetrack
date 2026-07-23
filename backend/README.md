# LifeTrack API

FastAPI proxy for optional meal-image analysis. It accepts an image only with explicit consent, processes it in memory through OpenAI Vision, stores only the structured nutrition estimate in Supabase PostgreSQL, and discards the image.

## Railway

Set the variables from `.env.example` in Railway. Add the SQL in `sql/001_initial.sql` through Supabase SQL Editor before deployment. The backend validates Supabase JWTs and writes through PostgREST with the user token, so RLS remains active. Never expose `SUPABASE_SERVICE_ROLE_KEY` or `OPENAI_API_KEY` to Android. Railway supplies HTTPS. Deploy from `backend/` with the included `railway.toml`; Swagger is available at `/docs`.
