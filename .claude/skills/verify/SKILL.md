---
name: verify
description: Use at the end of every stage (plan §7) and before any commit on this project. Runs the full gate — spotlessCheck, detekt, lint, testDebugUnitTest, assembleDebug — and reports pass/fail with the first failing output. This is the gate for M10 "each stage ends with a green verify skill".
---

# Verify — green gate before a commit

**Source of truth:** `docs/plan.md` §3 (M10), §8 ("Verification commands").

**Trigger:** end of every stage; any commit; explicit "verify everything" call.

## What it does

Runs in order (fail-fast — stop on the first error, diagnose):

1. `./gradlew spotlessCheck` — Kotlin / Gradle formatting.
2. `./gradlew detekt` — static analysis.
3. `./gradlew lint` — Android Lint.
4. `./gradlew testDebugUnitTest` — all Kotest specs (JUnit Platform).
5. `./gradlew assembleDebug` — APK build.

For stages 09–12 additionally (as relevant):
6. `./gradlew connectedDebugAndroidTest` — Compose UI tests (needs an emulator).
7. `./gradlew :app:assembleRelease -PcomposeReports` — Compose stability reports (stages 11, 12).

## How to read the result

- **Green** → the stage can be closed and committed.
- **Red** → stop, surface the first error with file:line. Do not try to fix several stages at once.

## What it does NOT do

- Does not format on its own (`spotlessApply`) — the PostToolUse hook does that after `Edit`/`Write`.
- Does not run `connectedBenchmarkAndroidTest` or `generateBaselineProfile` — those are heavier commands of stage 12.
- Does not edit code, does not write the commit.

## One-liner

```bash
./gradlew spotlessCheck detekt lint testDebugUnitTest assembleDebug
```

If a device/emulator is attached and the stage requires UI tests:
```bash
./gradlew spotlessCheck detekt lint testDebugUnitTest assembleDebug connectedDebugAndroidTest
```

## Report

- Which tasks passed (`✓`), which first failed (`✗`) with a short log.
- References to the IDs closed in the stage (`Refs: FR-…`).
- Ready-to-use commit line when everything is green:
  ```
  <type>(<scope>): <summary>

  Refs: <IDs>
  ```
