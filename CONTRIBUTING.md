# Contributing

## Workflow

This repo is built milestone-by-milestone (see [ROADMAP.md](ROADMAP.md)). Each milestone:

1. Implements one coherent slice of functionality.
2. Compiles cleanly (`./gradlew assembleDebug`).
3. Passes tests (`./gradlew testDebugUnitTest`) and lint (`./gradlew lint`).
4. Gets committed with a clear, conventional commit message.
5. Updates relevant docs (README/ARCHITECTURE/ALGORITHMS/ROADMAP) in the same PR.

Avoid large commits that mix unrelated changes — prefer one focused commit per feature.

## Commit message convention

Follows [Conventional Commits](https://www.conventionalcommits.org/):

```
feat: add Health Connect integration
feat: add sleep score engine
feat: add recovery dashboard
fix: handle missing HRV data
refactor: improve baseline calculation
docs: update architecture diagram
test: add baseline edge case coverage
ci: add lint step to Android workflow
```

## Code style

- Kotlin, idiomatic coroutines/Flow for async work.
- Calculations (`domain/calculations`) must stay free of Android/Compose imports so they
  remain unit-testable in isolation.
- New Room entities go in `data/database/entities`; add a migration, don't mutate an
  existing entity's schema in place once it has shipped.
- UI screens follow the existing `ui/screens/<name>/<Name>Screen.kt` + ViewModel pattern.

## Tests

New calculation logic needs unit tests covering at least: missing data, outliers, timezone
edge cases, and duplicate samples — see [ROADMAP.md](ROADMAP.md) Milestone 13 for the full
test checklist.
