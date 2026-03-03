# Dokusho Tracker

Dokusho Tracker is an offline Android app for tracking Japanese reading immersion.
It helps you quickly log completed entries, monitor progress, and stay consistent with reading goals.

## Main Features

- Fast entry logging for novels, light novels, web novels, and visual novels
- Series-aware tracking (title + volume workflows)
- Dashboard with cumulative progress, activity trends, milestones, and media breakdown
- Goal system with progress tracking and completion celebration
- History management with sorting, editing, and delete/undo flow
- Theme customization:
  - Light / Dark / System mode
  - Accent color selection
  - Pure black dark mode option
- Fully local storage using Room + DataStore
- JSON export/import for local backup and restore

## Tech Stack

- Kotlin + Jetpack Compose (Material 3)
- MVVM + Clean Architecture
- Room (SQLite)
- Hilt (Dependency Injection)
- Coroutines + Flow
- DataStore Preferences

## Build

From the project root:

```bash
./gradlew :app:assembleDebug
```

Install the generated APK from:

`app/build/outputs/apk/debug/`

## Notes

- The app is designed to be offline-first; no network connection is required for core features.
- Minimum Android version: API 26.
