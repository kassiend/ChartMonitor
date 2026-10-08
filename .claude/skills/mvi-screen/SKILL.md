---
name: mvi-screen
description: Use for any change in a presentation layer (feature:*) — new/modified ViewModel, screen Composable, State/Intent/Effect contract, reducer, or user action handling. Enforces the project's MVI rules from plan §3 and §4 ("Monitor screen MVI").
---

# MVI screen

**Source of truth:** `docs/plan.md` §3 (M6, M7, M8, N1, N5, N6), §4 ("Monitor screen MVI").

**Trigger:** any change under `feature:*/presentation/` — ViewModel, screen, reducer, Intent, State.

**Rules:** M6, M7, M8, N1, N5, N6.

## Contract

```kotlin
@Immutable
data class XState(
    val items: ImmutableList<ItemUi> = persistentListOf(),
    val visibleIds: ImmutableSet<SourceId> = persistentSetOf(),
    val isLoading: Boolean = false,
    // NO chart points (N1) — those get their own SharedFlow
) : BaseUIState

sealed interface XUIEvent : BaseUIEvent {
    data class ToggleVisibility(val id: SourceId) : XUIEvent
    data object Refresh : XUIEvent
}

sealed interface XEffect : BaseUIEffect {
    data class ShowError(val text: String) : XEffect
    data class NavigateTo(val route: String) : XEffect
}
```

## Reducer-style handling

Reducing is **inside the ViewModel** via `reduce(event)` overloads (see `core:mvi` `BaseViewModel`). The ViewModel owns a single `MutableStateFlow<State>` and updates it with `updateUIState { it.copy(...) }`.

Formatting, sorting, and derived values go into private helpers on the ViewModel or into a pure function next to it — not inside Composables (**N5**).

Automatic changes (e.g. unchecking on `Finished`) — **derived from status, not mutated in prefs**: `isChecked = id in activeVisible && status is Active`.

## ViewModel

```kotlin
@HiltViewModel
class XViewModel @Inject constructor(
    observeSources: ObserveSourcesUseCase,
    observeBatches: ObservePointBatchesUseCase,  // for chart; NOT in State (N1)
    private val getHistory: GetSourceHistoryUseCase,
    clock: MonotonicClock,
    @Dispatcher(MonitorDispatchers.Default) dispatcher: CoroutineDispatcher,
    logger: Logger,
) : BaseEffectViewModel<XState, XEffect>(logger) {

    override fun getStartUIState() = XState()

    val chartBatches: SharedFlow<PointBatch> = observeBatches()  // bypasses State (N1)

    override fun initScreen() {
        launchWithCatch {
            combine(observeSources(), secondTicker(clock)) { s, now ->
                computeState(s, now, value)
            }
                .flowOn(dispatcher)                             // sorting/formatting off main (N5)
                .collect { onUIState(it) }
        }
    }

    fun history(id: SourceId): PointSeries = getHistory(id)

    override fun onUIEvent(uiEvent: BaseUIEvent) {
        super.onUIEvent(uiEvent)
        when (uiEvent) {
            is XUIEvent.ToggleVisibility -> reduce(uiEvent)
            XUIEvent.Refresh -> reduce(uiEvent)
            else -> Unit
        }
    }

    private fun reduce(event: XUIEvent.ToggleVisibility) = updateUIState { ... }
    private fun reduce(event: XUIEvent.Refresh) = updateUIState { ... }
}
```

**One shared 1 Hz tick per screen** (**N6**), aligned to the second boundary:
```kotlin
fun secondTicker(clock: MonotonicClock, periodMs: Long = 1_000): Flow<Long> = flow {
    while (true) {
        val now = clock.nowMs()
        emit(now)
        delay(periodMs - now % periodMs)
    }
}
```

## UI

```kotlin
@Composable
fun XRoute(viewModel: XViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()      // M8
    XScreen(state, viewModel.chartBatches, viewModel::history, viewModel::onUIEvent)
}

@Composable
internal fun XScreen(
    state: XState,
    batches: SharedFlow<PointBatch>,
    history: (SourceId) -> PointSeries,
    onUIEvent: (BaseUIEvent) -> Unit,
) {
    // 1) ImmutableList/Set — stable for Compose (M7)
    // 2) LazyColumn with key = { it.id.value } (M7)
    // 3) Modifier.animateItem() for smooth re-sort movement
    // 4) Tabular digits (TabularNumbers from core:designsystem) for timer and value — width stays stable (PERF-02)
    // 5) alpha via Modifier.graphicsLayer { alpha = ... } (draw phase, no relayout)
}
```

## Tests

- **Pure helpers**: Kotest `BehaviorSpec` with Given/When/Then, test names `test("FR-LIST-xx ...")`.
- **ViewModel**: Kotest + Turbine + `withTestMain(testCoroutineScheduler) { ... }` from `core:testing`.
- **Composable** (if complex): instrumented JUnit4, `createComposeRule()`, `testTag` + `onNodeWithText`.

## Checklist

- [ ] `State` is `@Immutable`, fields are `ImmutableList/Set` (**M7**).
- [ ] No chart points in `State` (**N1**).
- [ ] No formatting, sorting, `Random`, `Clock` inside a Composable (**N5**).
- [ ] One shared tick per screen, not per item (**N6**).
- [ ] `collectAsStateWithLifecycle()` in UI (**M8**).
- [ ] `stateIn(viewModelScope, WhileSubscribed(5_000), initial)` — state survives config change without losing upstream.
- [ ] `flowOn(Dispatchers.Default)` before `stateIn` — reducer is off the main thread.
- [ ] Compose compiler reports (`-PcomposeReports`): `XState` is stable, `XRow` is skippable.
