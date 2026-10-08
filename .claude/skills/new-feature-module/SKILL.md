---
name: new-feature-module
description: Use when adding a new screen or feature slice to this project ("add screen X", "scaffold feature Y"). Creates a feature:<name> module with MVI contract, ViewModel, Compose screen, Hilt wiring and a Kotest spec.
---

# New feature module

**Trigger:** "add a screen", "new feature", "new flow".

## Steps

1. **Create the module** `feature/<name>/` with `build.gradle.kts`:
   ```kotlin
   plugins { alias(libs.plugins.monitor.android.feature) }
   android { namespace = "com.dauren.monitor.feature.<name>" }
   dependencies {
       implementation(projects.core.common)
       implementation(projects.core.ui)
       implementation(projects.core.chart) /* if the chart is needed */
   }
   ```
   Register in `settings.gradle.kts`: `include(":feature:<name>")`.
   The `monitor.android.feature` convention already pulls `lifecycle-*-compose`, `hilt-navigation-compose`, `kotlinx-collections-immutable`, `core:testing` (for tests), plus Hilt + Compose.

2. **MVI contract** `<Name>Contract.kt`:
   - `<Name>State : BaseUIState` — `@Immutable data class`, fields are `ImmutableList/Set` with default values.
   - `<Name>UIEvent : BaseUIEvent` — `sealed interface`, one case per user action.
   - `<Name>Effect : BaseUIEffect` — `sealed interface` for one-shots (navigation, snackbar).

3. **ViewModel** `<Name>ViewModel.kt`:
   ```kotlin
   @HiltViewModel
   class XViewModel @Inject constructor(
       private val repository: XRepository,
       clock: MonotonicClock,
       @Dispatcher(ChartMonitorDispatchers.Default) dispatcher: CoroutineDispatcher,
   ) : BaseEffectViewModel<XState, XEffect>() {
       override fun getStartUIState() = XState()

       init {
           launchWithoutCatch {
               combine(repository.sources, secondTicker(clock)) { s, now ->
                   computeState(s, now, value)
               }.flowOn(dispatcher).collect { onUIState(it) }
           }
       }

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
   State is consumed via `collectAsStateWithLifecycle()` in UI.

4. **UI** `<Name>Route.kt` + `<Name>Screen.kt`:
   - `<Name>Route` — stateful wrapper with `hiltViewModel()`.
   - `<Name>Screen(state, batches, onUIEvent)` — stateless.
   - `LazyColumn` with stable `key = { it.id }`.
   - Tabular digits for timers and values.

5. **Hilt navigation**: add the destination under `app/.../navigation/`, wire it into the nav graph.

6. **Tests**:
   - Kotest `BehaviorSpec` for pure helpers: `Given ... When ... Then ...`.
   - Kotest + Turbine for the VM: `withTestMain(scheduler) { ... }` from `core:testing`, virtual time.
   - Instrumented (JUnit4) for item composables if UI is complex.

7. Call the `verify` skill.

## Pre-PR checklist

- [ ] `State` is stable (`@Immutable`, `ImmutableList/Set`); row composables are skippable under `-PcomposeReports`.
- [ ] ViewModel reducers covered by tests.
- [ ] No formatting, sorting, Random, Clock inside a Composable.
- [ ] No chart points in State (they go through a separate `SharedFlow`, see the `scichart-chart` skill).
- [ ] Commit: `feat(<name>): <summary>`.
