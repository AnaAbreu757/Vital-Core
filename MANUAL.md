# VitalCore — Manual

A complete guide to installing, building, understanding, and extending VitalCore. This
document is meant to sit at the root of the repository (or under `docs/`) as the main
reference for anyone picking up the project — including future-you in six months.

---

## Table of contents

1. [What is VitalCore](#what-is-vitalcore)
2. [Feature overview](#feature-overview)
3. [Requirements](#requirements)
4. [Getting the project running](#getting-the-project-running)
5. [First run on a device](#first-run-on-a-device)
6. [Project structure](#project-structure)
7. [How the scores actually work](#how-the-scores-actually-work)
8. [Data flow, end to end](#data-flow-end-to-end)
9. [Testing](#testing)
10. [CI/CD](#cicd)
11. [Privacy model](#privacy-model)
12. [Configuration reference](#configuration-reference)
13. [Troubleshooting](#troubleshooting)
14. [Known limitations](#known-limitations)
15. [Roadmap](#roadmap)
16. [Contributing](#contributing)
17. [License](#license)

---

## What is VitalCore

VitalCore is an Android health/fitness app in the spirit of WHOOP, Bevel, and Gentler
Streak, built around one rule: **it never talks to a wearable's SDK directly.** Every
signal — heart rate, HRV, sleep, steps, SpO2, respiratory rate, temperature, and more —
arrives through **Android Health Connect**, so any device that syncs into Health Connect
(Xiaomi, Garmin, Samsung, Fitbit, Pixel Watch, …) works with VitalCore without
device-specific code.

From that data, VitalCore computes three original daily scores:

| Score | Scale | What it captures |
|---|---|---|
| **Recovery** | 0–100 | How ready your body is today, vs. your personal baseline |
| **Sleep** | 0–100 | Duration, efficiency, consistency, and debt for last night |
| **Strain** | 0–21 | Cardiovascular load accumulated today, from exercise and daily activity |

None of the formulas are copies of a competitor's proprietary algorithm — see
[How the scores actually work](#how-the-scores-actually-work) and `ALGORITHMS.md` for the
actual math.

On top of the wearable data, VitalCore also lets you log **food, calories, macros, and
water intake** manually, and includes an optional **AI Coach** that can explain your
scores and summarize your day in plain language, using your own AI provider credentials.

## Feature overview

- **Health Connect integration** — reads heart rate, resting heart rate, HRV, sleep
  (incl. stages when available), steps, calories, exercise sessions, distance,
  respiratory rate, SpO2, skin temperature, weight, and blood pressure.
- **Personal baseline engine** — learns your normal ranges over a rolling 60-day window,
  with outlier rejection, and reports its own maturity (collecting → initial → building →
  robust) so scores are never dressed up as more confident than they are.
- **Recovery / Sleep / Strain** — original scoring engines, pure Kotlin, unit tested.
- **Insights** — plain-language, rule-based observations ("Your HRV is 11% above your
  baseline"), plus a gated correlation insight once enough history exists. Language is
  always "associated with," never "causes."
- **Dashboard** — Home screen wired to the real calculation engine: scores, vitals,
  activity, today's insight, and a data-quality readout.
- **Trend charts** — Recovery/Sleep/Strain history over 1D/7D/30D/90D, drawn with a
  lightweight custom Canvas chart (no charting library dependency).
- **Notifications** — a daily morning summary (WorkManager-scheduled) plus event-driven
  high-strain and low-recovery alerts, each independently togglable.
- **Onboarding** — a two-step flow: pick a goal, then connect Health Connect. Nothing else
  is collected.
- **Privacy & data controls** — local-first storage, JSON/CSV export, and an
  irreversible-with-confirmation "delete all data" action.
- **Nutrition tracking** — manual food logging (name, calories, protein/carbs/fat) with
  meal type, and one-tap water logging (250/500/750 ml), with a daily summary and a water
  progress bar. Reachable from the Health tab.
- **AI Coach** — an optional, chat-style assistant, reachable from Home, that explains
  today's Recovery/Sleep/Strain and nutrition in plain language. You configure your own
  Anthropic-Messages-API-compatible endpoint and API key in Settings → AI Coach; nothing
  is bundled into the app, and the key never leaves the device except in the request you
  send to your own configured endpoint.
- **CI** — GitHub Actions runs lint, unit tests, and a debug build on every push/PR to
  `main`.

## Requirements

- **Android Studio** Ladybug (2024.2) or newer
- **JDK 17**
- **Android SDK 35** (compileSdk / targetSdk); **minSdk 28**
- A physical device or emulator with **Health Connect**:
  - Android 14+: built into the OS.
  - Android 9–13: install "Health Connect by Android" from the Play Store.
- At least one data source syncing into Health Connect (a wearable's own app, e.g. Xiaomi
  Wear, Galaxy Wearable, Garmin Connect) if you want real data rather than an empty state.

## Getting the project running

```bash
git clone <your-repo-url>
cd VitalCore
```

### Option A — Android Studio (recommended)

1. **File → Open** and select the `VitalCore` folder.
2. Let Gradle sync. On first sync, Android Studio generates the `gradlew` wrapper JAR
   automatically if it isn't already present.
3. Select a device/emulator with Health Connect available.
4. Run the `app` configuration (▶).

### Option B — Command line

If `gradlew` doesn't yet have its wrapper JAR (this repo ships the wrapper *scripts* and
`gradle-wrapper.properties`, but not the binary JAR — see
[Known limitations](#known-limitations)):

```bash
gradle wrapper --gradle-version 8.10.2   # one-time, needs a local Gradle install
./gradlew assembleDebug
```

Install the resulting APK:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## First run on a device

1. **Onboarding — goal.** Pick the single goal that best describes why you're using the
   app (Improve fitness, Improve sleep, Build muscle, Lose weight, General health,
   Performance). This is stored locally and doesn't currently change the algorithms — it's
   there for future personalization.
2. **Onboarding — connect Health Connect.** Tapping **Connect** opens the system Health
   Connect permission screen listing every record type VitalCore can use. You can grant a
   subset — the app is built to degrade gracefully (see [Data flow](#data-flow-end-to-end)).
   **Skip for now** is also an option; the app still runs, just with no data.
3. **Home tab.** On first load, the app syncs the last 30 days from Health Connect into its
   local database, then computes today's Recovery/Sleep/Strain. With a brand-new Health
   Connect connection this will mostly show "no data" until your wearable's app has synced
   some history into Health Connect.
4. **Recovery / Sleep / Activity tabs** show the same scores over time as a trend chart.
5. **Health tab** shows raw vitals for today plus a data-quality percentage.
6. **Settings tab** — toggle notifications, configure the AI Coach, export your data, or
   delete everything.
7. **Nutrition** — from the Health tab, tap **Nutrition — log food & water** to log meals
   (name, calories, optional macros, meal type) and water (quick +250/500/750 ml buttons).
   Totals and a water progress bar update immediately.
8. **AI Coach** — from Home, tap **Ask AI Coach**. Without a provider configured yet,
   it responds with a clear message pointing you to Settings → AI Coach. Once configured,
   use the quick-action chips (Recovery/Sleep/Strain/Summarize day) or type a free-form
   question — answers are grounded only in your own logged data, never invented numbers.

## Project structure

```
com.vitalcore.app
├── data
│   ├── healthconnect     HealthConnectManager, permission set, availability check
│   ├── database          Room database, entities, DAOs
│   └── repositories      HealthDataRepository, ScoreRepository, BaselineRepository
├── domain
│   ├── model              Pure-Kotlin domain models (HeartRateSample, SleepSession, …)
│   └── calculations        BaselineEngine, RecoveryCalculator, SleepCalculator,
│                           StrainCalculator, InsightsEngine, DataQualityCalculator
├── ui
│   ├── theme               Color / typography / light-dark-dynamic theme
│   ├── navigation           NavGraph, onboarding gate, bottom nav destinations
│   ├── components            TrendChart, PeriodSelector
│   └── screens/*             home, recovery, sleep, activity, health, settings, onboarding
├── notifications            NotificationChannels, MorningSummaryWorker,
│                            NotificationScheduler, AlertNotifier
├── settings                  SettingsRepository (DataStore), PrivacyExportManager
├── ai                        AiProvider, RemoteAiProvider, AiSettingsRepository,
│                            AiCoachRepository — the optional AI Coach layer
└── di                         Hilt modules
```

Full rationale for this layering lives in `ARCHITECTURE.md`. Nutrition data
(`FoodEntryEntity`/`WaterEntryEntity`) lives alongside the other Room entities in
`data/database/entities`, with its own `NutritionDao` and `NutritionRepository`.

## How the scores actually work

Full detail (weights, formulas, edge-case handling) is in `ALGORITHMS.md`. Short version:

- **Baseline** — rolling mean/standard deviation per metric over up to 60 days, outliers
  beyond 4 standard deviations excluded before they skew it.
- **Recovery** — each available signal's deviation from its own baseline (HRV, resting HR,
  respiratory rate, SpO2, temperature, training load) plus last night's Sleep score,
  combined with configurable weights. Missing signals are dropped, not defaulted to
  neutral — their weight is redistributed across what's actually present.
- **Sleep** — weighted mix of duration vs. an 8-hour target, efficiency (only when the
  device reports sleep stages), bedtime consistency, and debt vs. your duration baseline.
- **Strain** — exercise heart-rate zones × duration, plus a smaller contribution from
  daily non-exercise activity, mapped onto 0–21 through a logarithmic curve so it grows
  quickly at low effort and flattens at extreme effort.
- **Insights** — simple threshold rules ("≥5% deviation from baseline"), plus one gated
  correlation insight that only fires with ≥14 days of paired history and a ≥60% pattern
  strength.
- **AI Coach** — not a scoring engine. It reads the outputs above (plus today's logged
  nutrition) as plain text and asks your configured AI provider to explain them. It never
  computes its own Recovery/Sleep/Strain numbers and is explicitly instructed (via its
  system prompt) to never invent data and to avoid causal language.

## Data flow, end to end

```
Wearable's own app  →  Android Health Connect  →  HealthConnectManager (typed reads)
     →  HealthDataRepository.sync()  →  Room (de-duplicated by timestamp)
     →  HealthDataRepository.snapshotForDay()  →  DailyHealthSnapshot (domain model)
     →  BaselineEngine + RecoveryCalculator/SleepCalculator/StrainCalculator/InsightsEngine
     →  ScoreRepository (persist)  →  ViewModels (StateFlow)  →  Compose screens
```

Every read from Health Connect is wrapped so a missing permission, unsupported record
type, or thrown exception degrades to an empty result instead of crashing — "no data" is
treated as a normal state throughout the app, not an error path.

## Testing

Unit tests live in `app/src/test/java/com/vitalcore/app/domain/calculations` and cover
every calculation engine in isolation (no Android dependencies needed):

```bash
./gradlew testDebugUnitTest
```

Coverage includes: empty/missing data, single-sample edge cases, outlier rejection,
baseline maturity thresholds, score clamping (0–100 / 0–21), intensity ordering (higher
effort → higher strain), and insight-language checks (no causal claims).

Instrumented (on-device) tests are scaffolded in `app/src/androidTest` — currently just
the Milestone 1 placeholder; see [Known limitations](#known-limitations).

```bash
./gradlew connectedAndroidTest   # requires a connected device/emulator
./gradlew lint
```

## CI/CD

`.github/workflows/android.yml` runs on every push and pull request to `main`:

1. Checkout, JDK 17, Gradle setup.
2. `./gradlew lint`
3. `./gradlew testDebugUnitTest`
4. `./gradlew assembleDebug`
5. Uploads the lint report and unit-test results as workflow artifacts.

A red CI run blocks merging in spirit (add branch protection rules in GitHub repo settings
if you want it enforced).

## Privacy model

Full detail in `PRIVACY.md`. In short: local-first, no ads, no data sale, no automatic
sharing. Settings → Privacy offers JSON/CSV export and an irreversible delete-all-data
action (confirmed via dialog). The AI Coach is the one feature that makes an outbound
network call at all, and only to the endpoint you yourself configure — it sends the
minimum context needed (today's scores + nutrition) and your API key never leaves the
device except in that request to your own endpoint.

## Configuration reference

| What | Where | Default |
|---|---|---|
| Recovery component weights | `SettingsRepository.recoveryWeights` (DataStore) | HRV 35%, RHR 20%, Sleep 20%, Respiratory 10%, SpO2 5%, Temp 5%, Training load 5% |
| Morning summary time | `NotificationScheduler.scheduleMorningSummary` | 07:30 local time |
| Notification toggles | Settings tab | Morning summary ON, Sleep reminder OFF, High strain ON, Low recovery ON |
| Low recovery alert threshold | `AlertNotifier` | Recovery score < 33 |
| High strain alert threshold | `AlertNotifier` | Strain score ≥ 16.0 |
| Baseline window | `BaselineEngine.MAX_WINDOW_DAYS` | 60 days |
| Sleep target duration | `SleepCalculator.TARGET_SLEEP_MINUTES` | 8 hours |
| AI Coach endpoint/key/model | `AiSettingsRepository` (Settings → AI Coach) | unset — feature inert until configured |
| Water goal | `NutritionRepository.observeDailySummary(waterGoalMilliliters=)` | 2000 ml |

Recovery weights are already wired through the data layer (`SettingsRepository` →
`RecoveryCalculator`); a dedicated slider UI for them is a natural next addition to
`SettingsScreen` (see Known limitations).

## Troubleshooting

**Build fails with "SDK location not found."**
Create `local.properties` at the repo root with `sdk.dir=/path/to/your/Android/sdk`, or let
Android Studio generate it on first open.

**`./gradlew` fails with "could not find or load main class."**
The wrapper JAR isn't present yet — run `gradle wrapper --gradle-version 8.10.2` once with
a local Gradle install, or just open the project in Android Studio, which generates it
automatically.

**Health Connect permission screen never appears.**
Confirm Health Connect is installed/enabled (Settings → Apps → Health Connect on Android
14+, or the standalone Play Store app on older versions), and that the device has at least
one app capable of writing the record types VitalCore requests.

**Home screen shows all "no data."**
This is expected right after connecting Health Connect for the first time if your
wearable's own app hasn't finished syncing history into Health Connect yet. Open your
wearable's app, force a sync, wait a few minutes, then pull to refresh (or relaunch)
VitalCore.

**Notifications never appear.**
On Android 13+, VitalCore requests `POST_NOTIFICATIONS` on first launch — check it wasn't
denied (Settings → Apps → VitalCore → Notifications), and check the specific toggle is on
under Settings → Notifications.

**AI Coach always says "not set up yet."**
Fill in all three fields in Settings → AI Coach (endpoint URL, API key, model) and toggle
it on, then tap Save. The default endpoint field is pre-filled with
`https://api.anthropic.com/v1/messages` as a starting point — swap in your own proxy URL
if you're not calling Anthropic's API directly. If it still fails after saving, the error
message from `RemoteAiProvider` (shown directly in the chat) includes the HTTP status or
exception message to help diagnose it.

## Known limitations

Documented here in full so nobody mistakes this for a finished, production-hardened app:

- **Recovery weight sliders aren't in the Settings UI yet**, even though the underlying
  data layer (`SettingsRepository.recoveryWeights`) already supports per-component
  weighting.
- **No background sync worker.** Data syncs when the Home screen loads, not periodically
  in the background. A `WorkManager` periodic sync worker is a natural Milestone 14.
  candidate.
- **No instrumented (on-device) tests beyond the placeholder.** All real calculation-engine
  coverage is JVM unit tests; UI/integration tests on a device aren't written yet.
  candidate.
- **Sleep consistency and Strain's max heart rate use neutral defaults** rather than a
  fully collected user profile (age, measured max HR).
- **The AI Coach is bring-your-own-endpoint**, not a fine-tuned or embedded model — it
  only explains VitalCore's own computed scores and never invents its own numbers.
- **Nutrition tracking is manual entry only** — no barcode scanning or food database
  lookup yet.

## Roadmap

See `ROADMAP.md` for the full milestone history (1–13, all complete) and Phase 5 ideas
(additional wearable-specific compatibility notes, the optional AI Coach layer).

## Contributing

See `CONTRIBUTING.md` for commit conventions (Conventional Commits) and code style notes.
Short version: one focused commit per feature, `feat:`/`fix:`/`refactor:`/`docs:`/`test:`/
`ci:` prefixes, keep `domain/calculations` free of Android imports.

## License

This repository does not yet declare a license file beyond what's included here — add a
`LICENSE` file matching your intent (MIT is included as a permissive default; swap it for
whatever fits your goals for the project, including "all rights reserved" if you don't
want it reused).
