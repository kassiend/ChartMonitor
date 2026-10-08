---
name: new-signal-source
description: Use when adding a new source of chart points (generator, backend REST, WebSocket, file replay) to this project. Creates a SignalSource + SignalSourceFactory, wires it via Hilt @IntoSet multibinding, writes a Kotest spec on virtual time. Does NOT touch feature:chartmonitor UI or core:chart.
---

# New signal source

**Trigger:** "wire up a source", "new source", "data from backend", "replay from a file".

The screen (`feature:chartmonitor` UI) and the chart (`core:chart`) do NOT change — only the data layer.

## Steps

1. **Implementation in `feature/chartmonitor/data/source/<Name>SignalSource.kt`**:
   ```kotlin
   internal class XSignalSource(
       override val info: SourceInfo,
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
   - `Clock`, `Random`, dispatcher come through the constructor — never read `Dispatchers.*` or `System.currentTimeMillis()` inside.
   - No `GlobalScope`, `runBlocking`, `Thread.sleep` inside the source.

2. **Factory** `<Name>SourceFactory.kt`:
   ```kotlin
   internal class XSourceFactory @Inject constructor(
       private val config: XConfig,
       private val clock: MonotonicClock,
       @Dispatcher(ChartMonitorDispatchers.IO) private val dispatcher: CoroutineDispatcher,
   ) : SignalSourceFactory {
       override fun create(): List<SignalSource> = List(config.count) { ... }
   }
   ```

3. **Hilt multibinding** in the data module's DI:
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

5. **Kotest spec** `src/test/.../<Name>SignalSourceSpec.kt`:
   ```kotlin
   class XSignalSourceSpec : FunSpec({
       coroutineTestScope = true
       test("<what we check>") {
           val clock = TestMonotonicClock(testCoroutineScheduler)
           val source = XSignalSource(info, clock, Random(42), StandardTestDispatcher(testCoroutineScheduler))
           val points = source.points().toList()
           points shouldHaveSize ...
       }
   })
   ```

6. **Integration test** (optional): through `DefaultSignalRepository`, verify that points from the new source appear in `batches`.

7. Call the `verify` skill.

## Checklist

- [ ] No `Dispatchers.*`, `System.currentTimeMillis()`, `GlobalScope`, `runBlocking` inside the source.
- [ ] `points()` completes when `_status.value = Finished`.
- [ ] Factory registered via `@Binds @IntoSet` — screen UI and `core:chart` are not touched.
- [ ] Test runs on virtual time.
- [ ] Commit: `feat(data): add <name> signal source`.
