# Privacy

VitalCore is privacy-first by default.

## Current defaults (subject to expansion as features land)

- **Local by default.** Health data lives in an on-device Room database. Nothing is
  uploaded automatically.
- **No ads.** VitalCore does not display ads and does not integrate an ad SDK.
  This is an app-level policy for VitalCore itself and is unrelated to any
  third-party service.
- **No data sale.** Health data is never sold or shared with data brokers.
- **No automatic sharing.** Nothing leaves the device without an explicit, user-initiated
  action (e.g. tapping "Export").
- **AI is opt-in and separate.** The core scores (Recovery/Sleep/Strain) work fully with
  AI disabled. If a user enables the optional AI explanation feature, only the minimum
  data needed to answer the specific question is sent to the configured AI provider —
  the user supplies and controls that provider's credentials; VitalCore never ships an
  API key inside the app binary.

## AI is opt-in and separate

The core scores (Recovery/Sleep/Strain) work fully with AI Coach unconfigured or disabled.
When enabled, only the minimum data needed to answer the specific question — today's
scores and logged nutrition, built by `AiCoachRepository.buildTodayContext()` — is sent to
the provider endpoint the user themselves configured in Settings → AI Coach. VitalCore
never ships an API key inside the app binary; the key lives only in a dedicated on-device
DataStore file (`AiSettingsRepository`) and is attached per-request. The `INTERNET`
permission in the manifest exists solely for this feature — no other network calls exist
anywhere else in the app.

## Planned controls (Settings → Privacy, Milestone 12)

- **Permissions** — see and revoke exactly which Health Connect data types VitalCore
  can read, in one place.
- **Export data** — full JSON and CSV export of everything stored locally.
- **Delete all data** — irreversible local wipe of the Room database.

## Health Connect

VitalCore only reads the specific record types it needs (heart rate, HRV, sleep, steps,
calories, exercise, distance, respiratory rate, SpO2, temperature, VO2 max, weight, blood
pressure — availability varies by connected device) and never assumes a data type is
present. Health Connect's own OS-level permission model governs access; VitalCore adds no
separate always-on background collection beyond what's needed for the features described
in [README.md](README.md).

This document will be expanded with concrete UI descriptions and data-retention specifics
as Milestone 12 (Privacy/Export/Settings) is implemented.
