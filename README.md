# VitalCore

VitalCore is an Android health/fitness app inspired by the philosophy of WHOOP, Bevel, and
Gentler Streak — but with its own algorithms and identity. It reads data from any wearable
supported by **Android Health Connect** and turns it into Recovery, Sleep, Strain, Health,
Activity, and Insight metrics.

The first device being tested against is a **Xiaomi Smart Band 10**, but the architecture is
device-agnostic by design: the app never talks to a vendor SDK directly. Everything flows
through Health Connect.

## Status

✅ Milestones 1–13 complete (MVP), plus Firebase login/backup, Strava OAuth2, Apple Health
import, Nutrition tracking, AI Coach, Friends & Streaks. See README.md (full version follows
in the next commits) and MANUAL_DEPLOY.md for setup.

(This is a bootstrap commit to initialize the repository — the complete README and full
codebase are added in the commits immediately following this one.)
