# LifeTrack

LifeTrack is a local-first Android application for personal tracking of habits, hydration and sleep. The MVP stores all tracking data on the device and does not require an account or network connection.

## Included MVP

- Material 3 interface with light, dark and dynamic system color themes.
- Daily home summary with overall progress and quick actions.
- Custom habits with daily goals, completion, archive state and local history.
- Hydration goal, quick/custom entries, configurable local hourly reminders and entry deletion.
- Sleep sessions that support crossing midnight, perceived quality, notes and deletion.
- Weekly summary statistics and an accessible monthly activity calendar.
- Room persistence, DataStore preferences, Hilt dependency injection, WorkManager reminders and Compose Navigation.

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
```

## Privacy and reminders

- Tracking records and preferences remain in the local Room database and DataStore files.
- Notifications are optional. Android 13+ asks for notification permission when reminders are enabled.
- Hourly reminders are deferred WorkManager jobs, so Android battery management can delay them. They are suppressed when the daily water goal is met or during the configured quiet period.
- Hydration and sleep information is general personal tracking, not medical advice or diagnosis.
