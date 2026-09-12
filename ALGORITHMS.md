# Algorithms

This document explains the *methodology* behind each VitalCore score. All of the engines
below are implemented in `domain/calculations` (Milestones 4–7) and unit tested in
`app/src/test/java/com/vitalcore/app/domain/calculations`.

## Principles that apply to every score

- **Original, not reverse-engineered.** VitalCore does not copy WHOOP's, Bevel's, or
  Gentler Streak's proprietary formulas. Each score is built from first principles using
  published physiology research as a guide, with its own weighting.
- **Never invent data.** If a signal is missing (no HRV that night, no SpO2 sensor on the
  device), the score computation says so explicitly rather than substituting a guess.
- **Confidence is a first-class output.** Every score ships with a confidence level
  (Low/Medium/High) derived from the Data Quality Score for the inputs it used.
- **Baseline before comparison.** No score compares "today vs. your baseline" until enough
  history exists (see Personal Baseline below). Before that, scores are presented as raw
  values without a "vs. usual" framing.

## Personal Baseline (`BaselineEngine`)

- Days 1–7: data collection only (`BaselineMaturity.COLLECTING`), no comparisons shown.
- Day 14: `INITIAL` baseline available.
- Day 30+: `ROBUST` baseline.
- Computed as a rolling mean/standard-deviation over up to the last 60 days per metric
  (HRV, resting heart rate, sleep duration, respiratory rate, SpO2, temperature).
- `withoutOutliers()` rejects values beyond 4 standard deviations from the running mean
  before they're folded into the baseline, so one bad sensor reading can't skew it.

## Recovery (0–100) — `RecoveryCalculator`

Combines each *available* signal's deviation from its personal baseline (HRV, resting
heart rate, respiratory rate, SpO2, skin temperature, training load) plus the prior
night's Sleep score, each mapped to a 0..1 "goodness" value via a soft logistic-style
curve and combined using configurable weights (`RecoveryWeights`, default HRV 35% / RHR
20% / Sleep 20% / respiratory 10% / SpO2 5% / temperature 5% / training load 5%). Missing
signals are excluded entirely — their weight is redistributed across what IS available —
rather than treated as neutral. Confidence (Low/Medium/High) is derived directly from how
many of the 7 possible signals were present that day.

## Sleep (0–100) — `SleepCalculator`

Weighted combination of: duration vs. an 8-hour target (40%), efficiency — asleep time /
time in bed, only when the source device reports sleep stages (25%), bed/wake-time
consistency (20%), and sleep debt vs. the personal duration baseline (15%). Efficiency and
sleep debt are `null` rather than guessed when the underlying data isn't available.

## Strain (0–21 scale) — `StrainCalculator`

An original, non-WHOOP formula: each exercise session's heart-rate zone (derived from
`avgHeartRate / maxHeartRate`) multiplies its duration into a load value, non-exercise
daily activity (steps, calories) adds a smaller baseline load, and the total is mapped
onto 0–21 through a logarithmic curve (`21 * ln(1 + load/15) / ln(1 + 400/15)`) — strain
grows quickly at low effort and flattens at very high effort.

## Insights — `InsightsEngine`

Rule-based comparisons against baseline (e.g. "Your HRV is 11% above your baseline"),
fired only when the deviation is meaningful (≥5%). A gated correlation insight ("Days with
less than 7 hours of sleep are associated with lower HRV the next morning") only fires
once at least 14 paired days of history exist and the pattern holds ≥60% of the time.
Insight text never asserts causality — always "associated with," never "causes" or
"because" (enforced by `InsightsEngineTest`).
