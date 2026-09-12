# VitalCore

VitalCore is an Android health/fitness app inspired by the philosophy of WHOOP, Bevel, and
Gentler Streak — but with its own algorithms and identity. It reads data from any wearable
supported by **Android Health Connect** and turns it into Recovery, Sleep, Strain, Health,
Activity, and Insight metrics.

The first device being tested against is a **Xiaomi Smart Band 10**, but the architecture is
device-agnostic by design: the app never talks to a vendor SDK directly. Everything flows
through Health Connect.

```
Xiaomi / Samsung / Garmin / Fitbit / Pixel Watch / ...
              ↓
        Health Connect
              ↓
        Data ingestion
              ↓
        Normalization
              ↓
             Room
              ↓
      Calculation Engine
              ↓
   Recovery / Sleep / Strain
              ↓
              UI
```

## Status

✅ **Milestones 1–13 complete** (MVP). See [ROADMAP.md](ROADMAP.md) for the full breakdown.

What exists right now:
- A real, compilable Android project (Gradle Kotlin DSL, Jetpack Compose, Material 3, Hilt,
  Room, WorkManager, DataStore).
- Full Health Connect integration (`data/healthconnect`): permission handling, typed readers
  for every supported record type, graceful handling of missing data/permissions.
- Room database with 18 entities, DAOs, and a repository layer that normalizes Health Connect
  data into domain models, de-duplicated on re-sync.
- A personal baseline engine (`domain/calculations/BaselineEngine`) with outlier rejection and
  maturity staging (collecting → initial → building → robust).
- Original Recovery (0–100), Sleep (0–100), and Strain (0–21) scoring engines, each pure
  Kotlin and unit tested independently of Android.
- A rule-based Insights engine that never asserts causality.
- A real Home dashboard wired end-to-end to the calculation engine, plus Recovery/Sleep/
  Activity screens with trend charts (1D/7D/30D/90D) via a lightweight Canvas-based chart
  component.
- WorkManager-scheduled morning summary notifications, plus event-driven high-strain/
  low-recovery alerts — all togglable in Settings.
- Onboarding (goal selection → Health Connect permission request), Settings/Privacy screen
  with JSON/CSV export and delete-all-data.
- **Nutrition tracking** (`data/repositories/NutritionRepository`, `ui/screens/nutrition`):
  local food logging (name, calories, macros, meal type) and quick water logging, with a
  daily summary card and progress bar reachable from the Health tab.
- **AI Coach** (`ai/`): an optional, chat-style assistant that explains today's Recovery/
  Sleep/Strain and nutrition in plain language. Bring-your-own endpoint and API key
  (Anthropic-Messages-API-compatible), configured in Settings → AI Coach and stored only
  on-device via a dedicated DataStore file. No API key ships inside the app, and the
  calculation engine works fully with the AI Coach left unconfigured.
- **Google Sign-In + Firebase cloud backup** (`auth/`) — optional account via Google
  Sign-In (Credential Manager), federated into Firebase Auth. Once signed in, scores can be
  backed up to Firestore. Fully optional — the app works completely without an account.
- **Strava connection** (`integrations/strava/`) — a real OAuth2 integration. Imports
  recent activities into the same table Health Connect data lands in, so they count toward
  Strain identically. Requires your own free Strava API application.
- **Samsung Health / Xiaomi Wear / Mi Fit** — already covered via Health Connect; there is
  no separate public third-party API for these on Android, so VitalCore doesn't fake one.
- **Apple Health import** (`integrations/applehealth/`) — since no Android app can read
  Apple Health directly, VitalCore imports a CSV export from iPhone instead.
- GitHub Actions CI (`.github/workflows/android.yml`): lint, unit tests, debug assemble on
  every push/PR to `main`.
- A real unit test suite for every calculation engine, covering missing data, outliers, and
  boundary conditions.
- A real Room `Migration` (v1→v2, adding the nutrition tables) — see
  `VitalCoreDatabase.MIGRATION_1_2` — replacing the "no migrations yet" gap from earlier.

## Deployment

For GitHub publishing, Firebase project setup, Google Sign-In configuration, and Strava
API registration — plus a full honest explanation of what's real vs. not possible for
Samsung Health/Apple Health/Mi Fit — see **`MANUAL_DEPLOY.md`**.

## Known gaps / next steps beyond this MVP

This is a complete, compilable MVP built end-to-end from a scaffold — not a finished product.
Honest gaps a real team would tackle next:
- Sleep consistency (bedtime variance) and Strain's user max-HR are currently using neutral
  defaults/estimates rather than a full onboarding-collected profile.
- Recovery/Sleep component weights are user-configurable in the data layer
  (`SettingsRepository.recoveryWeights`) but the Settings UI doesn't yet expose sliders for
  them — only notification toggles, AI Coach configuration, and privacy controls are wired
  into `SettingsScreen`.
- No CameraX/Health Connect background sync worker yet — data syncs when `HomeViewModel` loads,
  not periodically in the background.
- No instrumented (on-device) tests beyond the Milestone 1 placeholder; the real test coverage
  added in Milestone 13 is JVM unit tests over the calculation engines.
- The AI Coach is a bring-your-own-endpoint chat layer, not a fine-tuned or embedded model —
  it only explains VitalCore's own computed scores and never computes its own numbers.
- Nutrition tracking is manual entry only — no barcode scanning or food database lookup yet.
- Firebase cloud backup is one-way (device → cloud) in this MVP; pulling a backup down to
  restore it on a new device is stubbed (`CloudSyncRepository.restoreLatestBackupTimestamp`
  reads the timestamp but doesn't yet write the data back into local Room).
- Strava's OAuth client secret is embedded in the built APK via `local.properties` →
  `BuildConfig`, which is a reasonable simplification for a personal/single-user install
  but not safe for public distribution — see `MANUAL_DEPLOY.md` section 4.3 for the
  production-grade fix (proxy the token exchange through a backend).

## Requirements

- Android Studio Ladybug (2024.2) or newer
- JDK 17
- Android SDK 35 (compileSdk/targetSdk), minSdk 28
- A device or emulator with Health Connect installed (Android 14+ has it built in; on
  Android 9–13 it's a separate Play Store app)

## Getting started

```bash
git clone <this-repo-url>
cd VitalCore
```

Open the folder in Android Studio and let it sync — Android Studio will generate the Gradle
wrapper jar automatically on first sync. If you prefer the command line and don't yet have a
`gradlew` binary in the repo, run once (with a local Gradle install):

```bash
gradle wrapper --gradle-version 8.10.2
```

Then:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lint
```

## Architecture

See [ARCHITECTURE.md](ARCHITECTURE.md) for the full breakdown of layers and packages.

In short: **data → domain → ui**, one-directional. Calculations (Recovery/Sleep/Strain/
Baseline) live in `domain/calculations` as plain Kotlin with no Android or Compose
dependencies, so they can be unit tested in isolation and never live inside a `@Composable`.

Score methodology (what each metric considers, and why) is documented in
[ALGORITHMS.md](ALGORITHMS.md) as it's implemented, milestone by milestone.

## Privacy

Privacy-first by default: data stays on-device, no ads, no data sale, no automatic sharing.
Full detail in [PRIVACY.md](PRIVACY.md) — this file is a live document and will be filled in
as the Settings/Privacy/Export milestone (12) lands.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for commit message conventions and the milestone-based
workflow this repo follows.

## Roadmap

See [ROADMAP.md](ROADMAP.md).
