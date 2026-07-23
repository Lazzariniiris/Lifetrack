# Technical Manual

## Architecture

The application has a single Android module to keep the initial MVP maintainable without premature Gradle modularization. Source packages separate `presentation`, `domain`, `data`, `notifications` and `di`.

Domain repositories are interfaces. Their Room and DataStore implementations are injected by Hilt. ViewModels expose immutable UI state through `StateFlow`; Compose screens collect state with lifecycle awareness.

## Brand assets

The official LifeTrack logo is represented by raster launcher variants in `mipmap-mdpi` through `mipmap-xxxhdpi`, adaptive icon descriptors for API 26+ and API 33+ monochrome support, a custom vector foreground, a notification small icon and a favicon asset. The Compose splash uses the extracted logo mark and original wordmark. Official colors are centralized in `res/values/colors.xml` and reflected in the Material 3 theme.

## Persistence

Room schema version 1 contains habits, habit logs, water entries and sleep entries. Foreign keys protect habit log integrity. Schema exports are written under `app/schemas`; future schema changes must add tested Room migrations rather than destructive fallback migration.

Dates are stored as epoch milliseconds. The UI derives local calendar dates using the current device zone; sleep stores both instants, so sessions across midnight remain valid.

## Reminders

WorkManager schedules one unique adaptive hydration worker when enabled. It recalculates after a water entry, deletion, preference change or app launch using remaining volume, active time, minimum interval, current pace and historical intervals. It schedules the next active day after a completed goal or quiet period. WorkManager is intentionally used as a best-effort scheduler; it is not an exact alarm system.

## Data handling

No analytics, remote backend, credentials or external services are included. Android automatic backup is disabled in this MVP until a versioned export/import flow is implemented.
