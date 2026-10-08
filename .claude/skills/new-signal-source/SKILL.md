---
name: new-signal-source
description: Use when adding a new source of chart points (generator, backend REST, WebSocket, file replay) to this project. Creates a SignalSource + SignalSourceFactory, wires it via Hilt @IntoSet multibinding, writes a Kotest spec on virtual time. Does NOT touch feature:monitor or core:chart.
---

# New signal source

**Source of truth:** `docs/plan.md` §2 (ARCH-01, ARCH-02), §3 (M2, M4, M5, N4), §4 ("Domain contracts").

**Trigger:** "wire up a source", "new source", "data from backend", "replay from a file".

**Rules:** M2, M4, M5, ARCH-01. **The screen (`feature:monitor`) and the chart (`core:chart`) are NOT touched.**

## Steps

1. **Implementation in `core:data/.../source/<Name>SignalSource.kt`**:
   ```kotlin
   internal class XSignalSource(
       override val meta: SourceMeta,
       private val clock: MonotonicClock,
       private val random: Random,                     // if needed
       private val dispatcher: CoroutineDispatcher,    // via @Dispatcher
   ) : SignalSource {
       private val _status = MutableStateFlow<SourceStatus>(SourceStatus.Active(...))
       override val status: StateFlow<SourceStatus> = _status.asStateFlow()
       override fun points(): Flow<SignalPoint> = flow { ... }.flowOn(dispatcher)
   }
   ```
   - `points()` is a **cold Flow**, collected by the repository exactly once.
   - The flow **completes on its own** once the source transitions to `Finished`.
   - `Clock`, `Random`, dispatcher — only via constructor (**M5**).
   - **No** `Dispatchers.*`, `System.currentTimeMillis()`, `Thread.sleep` inside the source (**N4**).

2. **Factory** `<Name>SourceFactory.kt`:
   ```kotlin
   internal class XSourceFactory @Inject constructor(
       private val config: XConfig,
       private val clock: MonotonicClock,
       @Dispatcher(MonitorDispatchers.IO) private val dispatcher: CoroutineDispatcher,
   ) : SignalSourceFactory {
       override fun create(): List<SignalSource> = List(config.count) { ... }
   }
   ```

3. **Hilt multibinding** in `core:data/.../di/SourceFactoriesModule.kt`:
   ```kotlin
   @Module @InstallIn(SingletonComponent::class)
   internal interface SourceFactoriesModule {
       @Binds @IntoSet fun x(factory: XSourceFactory): SignalSourceFactory
   }
   ```
   This is the only place that knows about the new type. `DefaultSignalRepository` picks it up from `Set<SignalSourceFactory>` without edits.

4. **`<Name>Config` provider** in `app/.../di/` (if a config is needed):
   ```kotlin
   @Module @InstallIn(SingletonComponent::class)
   object XConfigModule { @Provides fun config() = XConfig(...) }
   ```

5. **Kotest spec** `core:data/src/test/.../<Name>SignalSourceSpec.kt`:
   ```kotlin
   class XSignalSourceSpec : FunSpec({
       coroutineTestScope = true
       test("ID-xx <what we check>") {
           val clock = TestMonotonicClock(testCoroutineScheduler)
           val source = XSignalSource(meta, clock, Random(42), StandardTestDispatcher(testCoroutineScheduler))
           val points = source.points().toList()
           points shouldHaveSize ...
       }
   })
   ```
   Test name = requirement ID.

6. **Integration test** (optional): through `DefaultSignalRepository`, verify that points from the new source appear in `batches`.

7. Call the `verify` skill.

## Checklist

- [ ] No `Dispatchers.*`, `System.currentTimeMillis()`, `GlobalScope`, `runBlocking` inside the source (**N4, M4, M5**).
- [ ] `points()` completes when `_status.value = Finished`.
- [ ] Factory registered via `@Binds @IntoSet` — `feature:monitor` and `core:chart` do not change (**ARCH-01**).
- [ ] Test runs on virtual time, name contains the requirement ID.
- [ ] Commit: `feat(data): add <name> signal source` + `Refs: ARCH-01, FR-…`.
