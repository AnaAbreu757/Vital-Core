# Architecture

VitalCore follows a one-directional **data → domain → ui** layering, similar in spirit to
clean architecture, sized for a single-module Android app (multi-module split can happen
later if build times demand it — not needed at this scale).

## Package map

```
com.vitalcore.app
├── data
│   ├── healthconnect     Health Connect client wrapper: permission requests,
│   │                     typed record readers, mapping HC records → domain models.
│   ├── database          Room database class, DAOs, migrations.
│   │   └── entities      Room @Entity classes (HeartRateSample, HRVSample,
│   │                     SleepSession, SleepStage, ExerciseSession, DailyActivity,
│   │                     SpO2Sample, RespiratoryRateSample, TemperatureSample,
│   │                     WeightSample, BloodPressureSample, DailyMetrics,
│   │                     RecoveryScore, SleepScore, StrainScore, Insight,
│   │                     UserBaseline).
│   └── repositories      Mediates between Health Connect, Room, and domain use
│                         cases. This is the only layer that "knows" both data
│                         sources exist.
├── domain
│   └── calculations      Recovery / Sleep / Strain / Baseline engines. Pure
│                         Kotlin, no Android or Compose imports, fully unit
│                         testable. Calculations never live inside a Composable.
├── ui
│   ├── theme             Color, typography, light/dark + dynamic color theme.
│   ├── navigation         NavGraph + bottom nav destinations.
│   └── screens/*          One package per top-level screen (home, recovery,
│                          sleep, activity, health, settings), each with its own
│                          ViewModel once data wiring lands.
├── notifications          WorkManager workers + scheduling for morning summary,
│                          sleep reminder, high strain, low recovery alerts.
├── ai                     Optional AI provider abstraction (explanations,
│                          summaries, Q&A, pattern-finding). Never ships an API
│                          key inside the app; talks to a remote provider the
│                          user configures.
├── settings               User preferences: units, notification toggles, score
│                          weighting, privacy/export/delete.
└── di                      Hilt modules binding the above together.
```

## Why the calculation engine is isolated

Recovery, Sleep, and Strain scores are the product's core value. Keeping
`domain/calculations` as plain Kotlin (no `Context`, no Compose, no Room entities
directly — only simple data classes passed in) means:

- Every score can be unit tested with hand-built sample data, including edge
  cases (missing HRV, timezone changes, duplicate samples, outliers).
- The engine has no dependency on whether the data originally came from Health
  Connect, a CSV import, or a test fixture.
- AI is explicitly a *consumer* of the calculation engine's output (for
  explanations), never a dependency of it — scores must work with AI fully
  disabled.

## Multi-wearable principle

The app never imports a vendor SDK (Xiaomi, Garmin, etc.). All device data enters
through `data/healthconnect`, gets normalized into domain models in
`data/repositories`, and is persisted in Room. Adding a new wearable brand is a
Health Connect permission/compatibility concern, not a code change to the
calculation engine or UI.

## AI Coach

The AI Coach (`ai/`) is a bring-your-own-endpoint chat layer, not part of the scoring
pipeline. `AiCoachRepository.buildTodayContext()` assembles today's Recovery/Sleep/Strain
and logged nutrition into a plain-text context block, and the configured `AiProvider`
(`RemoteAiProvider`, calling an Anthropic-Messages-API-compatible endpoint the user
supplies) answers questions against it. The system prompt explicitly instructs the model
to never invent numbers not present in the context and to use "associated with" framing
rather than causal claims, matching `InsightsEngine`'s own rules. With no provider
configured, `RemoteAiProvider` returns a clear "not set up yet" message rather than
failing silently or crashing.

## Nutrition

Food and water logging (`data/repositories/NutritionRepository`) is manual-entry only in
this MVP — no barcode scanning or food database lookup. Each `FoodEntry` carries optional
macro fields (protein/carbs/fat in grams); calories are required. Daily totals are
computed reactively as a `Flow` combining food and water entries for the current UTC day.

## State of this document

This file will grow as each milestone lands — in particular, the `database`
entity schema and the calculation engine's internals will be documented here in
detail once Milestones 3–7 are implemented.
