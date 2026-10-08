---
name: mvi-screen
description: Use for any change in a presentation layer (feature:*) — new/modified ViewModel, screen Composable, State/UIEvent/Effect contract, or user action handling. Enforces the project's MVI pattern built on top of core:ui mvi (BaseViewModel / BaseEffectViewModel).
---

# MVI screen

**Trigger:** any change under `feature:*/ui/` — ViewModel, screen, UIEvent, State.

## Contract

```kotlin
@Immutable
data class XState(
    val items: ImmutableList<ItemUi> = persistentListOf(),
    val visibleIds: ImmutableSet<String> = persistentSetOf(),
    val isLoading: Boolean = false,
    // NO chart points in State — those get their own SharedFlow
) : BaseUIState

sealed interface XUIEvent : BaseUIEvent {
    data class ToggleVisibility(val id: String) : XUIEvent
    data object Refresh : XUIEvent
}

sealed interface XEffect : BaseUIEffect {
    data class ShowError(val text: String) : XEffect
    data class NavigateTo(val route: String) : XEffect
}
```

## Reducer-style handling

Reducing lives **inside the ViewModel** via `reduce(event)` overloads (see `core:ui` `BaseViewModel`). The ViewModel owns a single `MutableStateFlow<State>` and updates it with `updateUIState { it.copy(...) }`.

Formatting, sorting and derived values go into private helpers on the ViewModel or into a pure function next to it — never inside a Composable.

Automatic changes (e.g. unchecking on `Finished`) — **derived from status**, not mutated in prefs: `isChecked = id in activeVisible && status is Active`.

## ViewModel

```kotlin
@HiltViewModel
class XViewModel @Inject constructor(
    private val repository: SignalRepository,
    clock: MonotonicClock,
    @Dispatcher(ChartMonitorDispatchers.Default) dispatcher: CoroutineDispatcher,
) : BaseEffectViewModel<XState, XEffect>() {

    override fun getStartUIState() = XState()

    init {
        launchWithoutCatch {
            combine(repository.sources, secondTicker(clock)) { s, now ->
                computeState(s, now, value)
            }
                .flowOn(dispatcher)                             // sorting/formatting off main
                .collect { onUIState(it) }
        }
    }

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

**One shared 1 Hz tick per screen**, aligned to the second boundary:
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
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    XScreen(state, viewModel.chartBatches, viewModel::history, viewModel::onUIEvent)
}

@Composable
internal fun XScreen(
    state: XState,
    batches: SharedFlow<PointBatch>,
    history: (String) -> Points,
    onUIEvent: (BaseUIEvent) -> Unit,
) {
    // 1) ImmutableList/Set — stable for Compose
    // 2) LazyColumn with stable key = { it.id }
    // 3) Modifier.animateItem() for smooth re-sort movement
    // 4) alpha via Modifier.graphicsLayer { alpha = ... } (draw phase, no relayout)
}
```

## Tests

- **Pure helpers**: Kotest `BehaviorSpec` with Given/When/Then.
- **ViewModel**: Kotest + Turbine + `withTestMain(testCoroutineScheduler) { ... }` from `core:testing`.
- **Composable** (if complex): instrumented JUnit4, `createComposeRule()`, `testTag` + `onNodeWithText`.

## Checklist

- [ ] `State` is `@Immutable`, collections are `ImmutableList/Set`.
- [ ] No chart points in `State` (streamed via a separate `SharedFlow`).
- [ ] No formatting, sorting, `Random`, `Clock` inside a Composable.
- [ ] One shared tick per screen, not per item.
- [ ] `collectAsStateWithLifecycle()` in UI.
- [ ] `flowOn(Dispatchers.Default)` on the pipeline — reducer is off the main thread.
- [ ] Compose compiler reports (`-PcomposeReports`): `XState` is stable, item composables are skippable.
