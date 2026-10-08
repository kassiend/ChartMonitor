---
name: new-feature-module
description: Use when adding a new screen or feature slice to this project ("add screen X", "scaffold feature Y"). Creates a feature:<name> module per plan §4 with MVI contract, ViewModel, Compose screen, Hilt wiring, navigation registration and a Kotest spec. Does NOT change core:data, core:chart or any other feature.
---

# New feature module

**Source of truth:** `docs/plan.md` §3 (M1, M6, M7, M8, N3, N5), §4 ("Modules" table, "Monitor screen MVI"), §5 (plugin `monitor.android.feature`).

**Trigger:** "add a screen", "new feature", "new flow".

**Rules:** M1, M6, M7, M8, N3, N5.

## Steps

1. **Create the module** `feature/<name>/` with `build.gradle.kts`:
   ```kotlin
   plugins { alias(libs.plugins.monitor.android.feature) }
   android { namespace = "com.dauren.monitor.feature.<name>" }
   dependencies {
       implementation(projects.core.mvi)
       implementation(projects.core.chart) /* if the chart is needed */
   }
   ```
   Register in `settings.gradle.kts`: `include(":feature:<name>")`.
   Convention `monitor.android.feature` already pulls `core:domain`, `core:ui`, `core:designsystem`, `lifecycle-*-compose`, `hilt-navigation-compose`, `kotlinx-collections-immutable`, `core:testing` (for tests).

2. **MVI contract** `<Name>Contract.kt`:
   - `<Name>State : BaseUIState` — `@Immutable data class`, fields are `ImmutableList/Set` with default values.
   - `<Name>UIEvent : BaseUIEvent` — `sealed interface`, one case per user action.
   - `<Name>Effect : BaseUIEffect` — `sealed interface` for one-shots (navigation, snackbar) through a `Channel`.

3. **ViewModel** `<Name>ViewModel.kt`:
   ```kotlin
   @HiltViewModel
   class XViewModel @Inject constructor(
       observeSources: ObserveSourcesUseCase,
       clock: MonotonicClock,
       @Dispatcher(MonitorDispatchers.Default) dispatcher: CoroutineDispatcher,
       logger: Logger,
   ) : BaseEffectViewModel<XState, XEffect>(logger) {
       override fun getStartUIState() = XState()

       override fun onUIEvent(uiEvent: BaseUIEvent) {
           super.onUIEvent(uiEvent)
           when (uiEvent) {
               is XUIEvent.ToggleVisibility -> reduce(uiEvent)
               else -> Unit
           }
       }

       private fun reduce(event: XUIEvent.ToggleVisibility) = updateUIState { ... }
   }
   ```
   State is consumed via `collectAsStateWithLifecycle()` in UI (**M8**).

4. **UI** `<Name>Route.kt` + `<Name>Screen.kt`:
   - `<Name>Route` — stateful wrapper with `hiltViewModel()`.
   - `<Name>Screen(state, batches, onUIEvent)` — stateless.
   - `LazyColumn` with stable `key = { it.id.value }` (**M7**).
   - Tabular digits from `core:designsystem` for timers and values.

5. **Hilt navigation**: add the destination under `app/.../navigation/`, wire it into the nav graph.

6. **Tests**:
   - Kotest `BehaviorSpec` for pure helpers: `Given ... When ... Then ...` with names like `test("FR-… ...")`.
   - Kotest + Turbine for the VM: `withTestMain(scheduler) { ... }` from `core:testing`, virtual time.
   - Instrumented (JUnit4) for `GeneratorRow` / components when UI is complex.

7. Call the `verify` skill.

## Pre-PR checklist

- [ ] The module **does not depend** on `core:data` (N3). Only on `core:domain`, `core:ui`, `core:designsystem`, `core:chart`, `core:mvi`.
- [ ] `State` is stable (`@Immutable`, `ImmutableList/Set`), `GeneratorRow`-like `@Composable`s are skippable under `-PcomposeReports`.
- [ ] ViewModel reducers covered by tests, names reference requirement IDs.
- [ ] No formatting, sorting, Random, Clock inside a Composable (N5, M5).
- [ ] No chart points in State (N1 — they go through a separate `SharedFlow`, see the `scichart-chart` skill).
- [ ] Commit: `feat(<name>): <summary>` + `Refs: FR-…`.
