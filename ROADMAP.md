# Roadmap

## Phase 1 — Foundation
- **Milestone 1 — Project scaffold** ✅
  Android project, Gradle Kotlin DSL, Compose, full package architecture, Hilt wiring,
  GitHub Actions CI, base documentation.
- **Milestone 2 — Health Connect** ✅
  `HealthConnectManager` wrapper, full read-permission set, typed record readers for every
  supported data type, safe fallback (empty/null) when a record type isn't available.
- **Milestone 3 — Database & repositories** ✅
  18 Room entities, DAOs with IGNORE-conflict de-dup, `HealthDataRepository` (sync + snapshot)
  and `ScoreRepository`/`BaselineRepository`.

## Phase 2 — Core scores
- **Milestone 4 — Baseline engine** ✅
  `BaselineEngine`: rolling 60-day window, maturity staging (collecting/initial/building/
  robust), outlier rejection (>4 std dev).
- **Milestone 5 — Recovery** ✅
  `RecoveryCalculator`: original 0–100 score, configurable weights, confidence level,
  per-component contribution breakdown.
- **Milestone 6 — Sleep** ✅
  `SleepCalculator`: duration, efficiency (only when stages exist), consistency, sleep debt.
- **Milestone 7 — Strain** ✅
  `StrainCalculator`: original 0–21 logarithmic load curve from HR zones + daily activity.

## Phase 3 — Surface it
- **Milestone 8 — Dashboard** ✅
  `HomeViewModel` + `HomeScreen` wired end-to-end to the real calculation engine.
- **Milestone 9 — Charts** ✅
  Canvas-based `TrendChart` + `PeriodSelector` (1D/7D/30D/90D), used on Recovery/Sleep/Activity.
- **Milestone 10 — Insights** ✅
  `InsightsEngine`: rule-based daily insights + a gated correlation insight ("associated
  with", never "causes").
- **Milestone 11 — Notifications** ✅
  `NotificationScheduler` (WorkManager, morning summary) + `AlertNotifier` (event-driven high
  strain / low recovery), all togglable in Settings.

## Phase 4 — Polish & trust
- **Milestone 12 — Privacy, export, settings** ✅
  `SettingsRepository` (DataStore), `PrivacyExportManager` (JSON/CSV export, delete-all-data),
  real `SettingsScreen`, `OnboardingScreen` (goal selection → Health Connect connection).
- **Milestone 13 — Tests, lint, CI hardening, final cleanup** ✅
  Unit test suites for BaselineEngine, RecoveryCalculator, SleepCalculator, StrainCalculator,
  DataQualityCalculator, and InsightsEngine — covering missing data, outliers, and boundary
  conditions. See README.md "Known gaps" for what a production hardening pass would add next.

## Phase 5 — Beyond MVP
- Additional wearable integrations and compatibility notes as new Health Connect data
  sources appear.
- **AI Coach** ✅ — bring-your-own-endpoint chat assistant (`ai/`) that explains Recovery/
  Sleep/Strain and nutrition in plain language. Configured per-device in Settings; never
  computes its own scores, only explains VitalCore's.
- **Nutrition tracking** ✅ — manual food (calories, macros, meal type) and water logging
  (`data/repositories/NutritionRepository`, `ui/screens/nutrition`), surfaced from the
  Health tab and included in the AI Coach's daily context.
- Future: barcode/food-database lookup for nutrition, a fine-tuned or on-device model
  option for AI Coach, background periodic Health Connect sync.

---

Each milestone above is implemented as its own set of commits, verified with
`./gradlew assembleDebug`, `./gradlew testDebugUnitTest`, and `./gradlew lint` before being
considered done.
