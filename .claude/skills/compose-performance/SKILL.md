---
name: compose-performance
description: Use when reviewing UI code for Compose recomposition cost — screen or component Composables, UI model stability, list rendering, animations. Checks stability, recomposition triggers, LazyColumn hygiene. Reads Compose compiler reports if present under build/compose_metrics.
---

# Compose performance

**Source of truth:** `docs/plan.md` §2 (PERF-01, PERF-02, PERF-04), §3 (M7, N5, N6), §4 ("Monitor screen MVI").

**Trigger:** reviewing any Composable; suspected jank or extra recompositions; changes to UI models.

**Rules:** M7, N5, N6.

## Stability checklist

- `State` and UI models (`XItemUi`) — `@Immutable data class` or `@Stable`.
- Collections in State — **from `kotlinx-collections-immutable`** (`ImmutableList`, `ImmutableSet`, `PersistentList/Set`). `List<T>` and `Set<T>` are unstable to the compiler.
- Domain `data class`es used in UI — marked in `compose_stability.conf` (file in the repo root, stage 04).
- Lambdas in Composable signatures are stable when `onClick: () -> Unit`; if they capture VM fields, wrap in `rememberUpdatedState` or forward via `remember { { ... } }`.

## LazyColumn / LazyRow

- Stable `key = { it.id.value }` — otherwise everything re-renders on every re-sort.
- `contentType = { "generator" }` — enables viewHolder reuse between items of the same type.
- `Modifier.animateItem()` for smooth movement on re-sort (`animateItemPlacement` is deprecated).
- Row height **fixed** when possible — otherwise every item runs measure/layout on each tick.
- Changing value text at a fixed width (`Modifier.width(...)` + `textAlign = End`) with tabular digits (`TabularNumbers` from `core:designsystem`). Otherwise width jumps around (PERF-02).

## Where recomposition goes

- `alpha` for "dimming" — via `Modifier.graphicsLayer { alpha = ... }`, **not** `Modifier.alpha(x)`. The former changes in the draw phase, no layout re-trigger.
- Line color — `Color(item.colorArgb)` is fine when `item` is stable.
- Reading `State` from collections — via `collectAsStateWithLifecycle()` at the `Route` level, not deep inside a Composable.
- One tick per screen (**N6**) — `GeneratorRow` receives `timerText: String`, not a clock.

## Compiler reports

```
./gradlew assembleRelease -PcomposeReports
```

Reports live in `build/compose_metrics/`. Look at:
- `*-classes.txt` — which classes are `stable`, `restartable`, `skippable`.
- `*-composables.txt` — which functions are `skippable`, which params are unstable.

**Targets**:
- `MonitorState`, `GeneratorItemUi` → `stable`.
- `GeneratorRow` → `restartable skippable`.
- `MonitorScreen` → `restartable` (not required to be skippable — it is the root).

An unstable param on `GeneratorRow` → look at its type. Most often it is `List<T>` instead of `ImmutableList<T>`.

## Gestures and animations

- `AnimatedVisibility`, `Crossfade` — OK, they are `remember`-based.
- Long animations — via `Animatable` + `LaunchedEffect` with the right key (not `Unit`).
- For one-shot side effects: `LaunchedEffect(key1, key2) { ... }` — the key re-arms the effect.

## What NOT to do

- Do not read `State` at the call site if you can read it deeper down — state hoisting keeps recomposition local (deferred reads).
- Do not pass heavy `Modifier` chains through parameters — pass `Modifier`, add locally.
- Do not wrap everything in a `Box { ... }` just for one `graphicsLayer` — put the modifier directly on the `Row/Column`.
- Do not use `remember { mutableStateOf(...) }` for derived values — use `remember(key) { derivedStateOf { ... } }`.

## Run

```
./gradlew :app:assembleRelease -PcomposeReports
# → build/compose_metrics/
./gradlew :benchmark:connectedBenchmarkAndroidTest    # FrameTimingMetric
```

## Report

Review output: table `file:line | what gets recomposed extra | how to fix`. For serious issues — record as `N1/N5/N6/M7` + ADR.
