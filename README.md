# LifeTrack

LifeTrack is a local-first Android application for habits, hydration and sleep, with optional account-scoped meal analysis and history through Supabase and FastAPI.

## Included product

- Material 3 interface with official LifeTrack light and dark themes.
- Official LifeTrack brand system: adaptive launcher icons, Android 13 monochrome icon, branded splash, top bar and About screen.
- Daily home summary with overall progress and quick actions.
- Custom habits with daily goals, completion, archive state and local history.
- Hydration goal, quick/custom entries, adaptive local reminders and entry deletion.
- Daily local motivation and visible hydration streaks.
- Sleep sessions that support crossing midnight, perceived quality, notes and deletion.
- Weekly summary statistics and an accessible monthly activity calendar.
- Room persistence, DataStore preferences, Hilt dependency injection, WorkManager reminders and Compose Navigation.
- Supabase authentication, profile management and password recovery.
- Private meal-image storage, durable offline queue and automatic retry.
- Strict AI analysis contract with confidence, alternatives and editable nutrition estimates.

## Requirements

- Android Studio Ladybug or newer, including its bundled JDK 17+.
- Android SDK Platform 35.
- A device or emulator running Android 8.0 (API 26) or newer.

## Build and run

On Windows PowerShell, from the repository root:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat :app:assembleDebug
```

Install `app\build\outputs\apk\debug\app-debug.apk` on an emulator or compatible Android device. Opening the project in Android Studio and selecting the `app` run configuration provides the same result.

## Tests

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
```

## Privacy and reminders

- Tracking records and preferences remain in the local Room database and DataStore files.
- Meal images and confirmed meal records are private account-scoped cloud data protected by RLS.
- Notifications are optional. Android 13+ asks for notification permission when reminders are enabled.
- Adaptive reminders are deferred WorkManager jobs, so Android battery management can delay them. They recalculate after water changes, respect a 45-minute minimum interval and resume at the next active period after a daily goal or quiet period.
- Hydration and sleep information is general personal tracking, not medical advice or diagnosis.

## Cloud analysis

`backend/` contains the FastAPI service for consent-based meal-image analysis using OpenAI and Supabase. Apply all numbered SQL migrations and configure `LIFETRACK_API_URL` before building a cloud-enabled APK. See `backend/README.md`, `docs/cloud-architecture.md` and `docs/technical-manual.md`.
