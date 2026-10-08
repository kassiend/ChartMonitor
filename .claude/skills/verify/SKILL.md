---
name: verify
description: Use before any commit on this project. Runs the full gate — spotlessCheck, detekt, lint, testDebugUnitTest, assembleDebug — and reports pass/fail with the first failing output.
---

# Verify — green gate before a commit

**Trigger:** any commit; explicit "verify everything" call.

## What it does

Runs in order (fail-fast — stop on the first error, diagnose):

1. `./gradlew spotlessCheck` — Kotlin / Gradle formatting.
2. `./gradlew detekt` — static analysis.
3. `./gradlew lint` — Android Lint.
4. `./gradlew testDebugUnitTest` — all Kotest specs (JUnit Platform).
5. `./gradlew assembleDebug` — APK build.

Optionally:
6. `./gradlew connectedDebugAndroidTest` — Compose UI tests (needs an emulator).
7. `./gradlew :app:assembleRelease -PcomposeReports` — Compose stability reports.

## How to read the result

- **Green** → can be committed.
- **Red** → stop, surface the first error with file:line.

## What it does NOT do

- Does not format on its own (`spotlessApply`) — the PostToolUse hook does that after `Edit`/`Write`.
- Does not edit code, does not write the commit.

## One-liner

```bash
./gradlew spotlessCheck detekt lint testDebugUnitTest assembleDebug
```

If a device/emulator is attached and UI tests are needed:
```bash
./gradlew spotlessCheck detekt lint testDebugUnitTest assembleDebug connectedDebugAndroidTest
```

## Report

- Which tasks passed (`✓`), which first failed (`✗`) with a short log.
- Ready-to-use commit line when everything is green.
