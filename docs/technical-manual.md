# Technical Manual

## Architecture

The application has a single Android module to keep the initial MVP maintainable without premature Gradle modularization. Source packages separate `presentation`, `domain`, `data`, `notifications` and `di`.

Domain repositories are interfaces. Their Room and DataStore implementations are injected by Hilt. ViewModels expose immutable UI state through `StateFlow`; Compose screens collect state with lifecycle awareness.

## Persistence

Room schema version 1 contains habits, habit logs, water entries and sleep entries. Foreign keys protect habit log integrity. Schema exports are written under `app/schemas`; future schema changes must add tested Room migrations rather than destructive fallback migration.

Dates are stored as epoch milliseconds. The UI derives local calendar dates using the current device zone; sleep stores both instants, so sessions across midnight remain valid.

## Reminders

WorkManager schedules one unique hourly hydration worker when enabled. It checks local preferences, quiet hours and daily water consumption before posting a notification. WorkManager is intentionally used as a best-effort scheduler; it is not an exact alarm system.

## Data handling

No analytics, remote backend, credentials or external services are included. Android automatic backup is disabled in this MVP until a versioned export/import flow is implemented.
