# GeneratorMonitor

Android app with one screen: SciChart line chart on top, list of 10 live generators below. Built as the Android Developer test assignment — see `task/task.pdf` for the brief.

**Stack**: Kotlin 2.2 · Jetpack Compose · Hilt · Coroutines/Flow · MVI · multi-module Clean Architecture · build-logic convention plugins · SciChart Android (trial) · Kotest + Turbine for unit tests · Macrobenchmark + Baseline Profile for perf.

---

## Quick start

### 1. SciChart trial key

The app refuses to render without a SciChart runtime key. Request a trial at [scichart.com](https://www.scichart.com/) and put it in `local.properties` (never committed):

```properties
sdk.dir=/path/to/Android/sdk
scichart.license=<your-trial-key>
```

`AndroidApplicationConventionPlugin` reads `scichart.license` and writes it to `BuildConfig.SCICHART_LICENSE`; `MonitorApp.initSciChartLicense()` applies it via `SciChartSurface.setRuntimeLicenseKey(...)` once at startup — **before any `SciChartSurface` is created**. If the key is empty or invalid, you'll see a `Log.w` / `Log.e` line but the app still launches (SciChart draws a watermark).

### 2. Build & run

```bash
./gradlew assembleDebug           # build
./gradlew installDebug            # install on connected device/emulator
```

### 3. Verify

```bash
./gradlew spotlessCheck detekt lint testDebugUnitTest assembleDebug
```

Or inside Claude Code: invoke `skill verify`.

---

## Project layout

```
GeneratorMonitor/
├── app/                       — Application + MainActivity + Hilt graph + SciChart license init
├── core/
│   ├── common/                — pure Kotlin: MonotonicClock, WallClock, @Dispatcher qualifiers, AppInitializer
│   ├── domain/                — pure Kotlin: models + SignalSource / SignalRepository contracts + use cases
│   ├── data/                  — Android: RandomWalkSignalSource, DefaultSignalRepository, PointRingBuffer, Hilt
│   ├── ui/                    — Compose theme + 10-color Tableau SeriesPalette + TabularNumbers
│   ├── chart/                 — SciChart: ChartController + LiveChart (AndroidView wrapper)
│   └── testing/               — FakeSignalSource, FakeSignalRepository, TestMonotonicClock, withTestMain
├── feature/
│   └── monitor/               — MVI: MonitorContract, MonitorReducer, MonitorViewModel, MonitorScreen
├── benchmark/                 — Macrobenchmark + Baseline Profile generator
├── build-logic/convention/    — 9 convention plugins (plan §5)
├── gradle/libs.versions.toml  — version catalog
├── compose_stability.conf     — Compose compiler stability config
├── config/detekt/             — shared Detekt config
├── docs/
│   ├── plan.md                — specification source of truth
│   ├── adr/                   — ADR 001..007
│   ├── open-questions.md      — unresolved spec gaps
│   └── acceptance.md          — PR acceptance checklist (plan §8)
└── .claude/                   — skills, subagents, hooks, MCP config (ships with the repo)
```

Dependency table in `docs/plan.md` §4. Boundaries are enforced by `.claude/agents/arch-reviewer`.

---

## Architecture one-liner

- **Two data streams** (plan §4):
  - **Fast — chart points**: `RandomWalkSignalSource` (cold `Flow<SignalPoint>`) → `DefaultSignalRepository` (ring buffer + 100 ms batcher) → `ChartController.append(batch)` → SciChart `XyDataSeries`. **One redraw per tick** via `UpdateSuspender.using`. MVI never sees individual points.
  - **Slow — list state**: `sources: StateFlow<List<SourceState>>` + 1 Hz `secondTicker` + user prefs → pure `MonitorReducer` on `Dispatchers.Default` → `StateFlow<MonitorState>` → Compose `LazyColumn`.
- **Visibility**: checkbox → `MonitorIntent.ToggleVisibility` → `UiPrefs.hiddenIds` → `State.visibleIds` → `ChartController.setVisible(ids)` by flag, no series recreation.

---

## Add a new signal source

Trigger: skill `new-signal-source`.

1. Implement `SignalSource` in `core:data/.../source/XxxSignalSource.kt`:
   ```kotlin
   internal class XxxSignalSource(
       override val meta: SourceMeta,
       private val clock: MonotonicClock,
       private val dispatcher: CoroutineDispatcher,
   ) : SignalSource {
       override val status: StateFlow<SourceStatus> = ...
       override fun points(): Flow<SignalPoint> = flow { ... }.flowOn(dispatcher)
   }
   ```
2. Build an `XxxSourceFactory : SignalSourceFactory`.
3. Bind it via Hilt multibinding in `SourceFactoriesModule`:
   ```kotlin
   @Binds @IntoSet fun xxx(factory: XxxSourceFactory): SignalSourceFactory
   ```
4. Write a Kotest spec on virtual time (`TestMonotonicClock(testScheduler)`).

**The screen and the chart do not change** — `DefaultSignalRepository` picks it up from the multibinding set (plan ARCH-01).

---

## Add a new screen

Trigger: skill `new-feature-module`.

1. Create `feature/<name>/` with `build.gradle.kts` applying `monitor.android.feature`.
2. MVI contract: `XState` (`@Immutable` with `ImmutableList`/`Set`), `XIntent` (`sealed interface`), `XEffect`.
3. Pure `XReducer`. Formatting + sorting live here, not in Composables (plan N5).
4. `@HiltViewModel` with `combine(...).distinctUntilChanged().flowOn(Default).stateIn(WhileSubscribed(5_000))`.
5. UI: `XRoute` (stateful, `collectAsStateWithLifecycle`) + stateless `XScreen`.
6. Kotest for reducer + VM, instrumented `createComposeRule` for row-level UI.

**`feature:*` depends only on `core:domain / ui / chart`** — never on `core:data` (plan N3).

---

## AI tooling

Everything in `/.claude/` ships with the repo:

| Trigger | Tool |
|---|---|
| “add screen / feature” | skill `new-feature-module` |
| “add signal source” | skill `new-signal-source` |
| any `presentation/` edit | skill `mvi-screen` |
| any `core:chart/` edit | skill `scichart-chart` |
| UI review | skill `compose-performance` |
| before commit | skill `verify` |
| end of stages 5, 6, 8, 11, 12 | subagent `arch-reviewer` |
| end of stages 10, 11, 12 | subagent `perf-reviewer` |
| library version / API lookup | MCP `context7` |
| UI check on emulator | MCP `mobile-mcp` |
| PRs / issues | MCP `github` (needs `GITHUB_PAT` env) |

Hooks (`.claude/hooks/`):
- `format-kotlin.sh` on `Edit`/`Write` — `./gradlew <module>:spotlessApply`
- `compile-changed.sh` on `Stop` — `./gradlew <module>:compileDebugKotlin` for touched modules

---

## Commands

| What | Command |
|---|---|
| Build | `./gradlew assembleDebug` |
| Quality | `./gradlew spotlessCheck detekt lint` |
| Unit tests | `./gradlew testDebugUnitTest` |
| UI tests | `./gradlew connectedDebugAndroidTest` (needs device) |
| Compose compiler reports | `./gradlew assembleRelease -PcomposeReports` |
| Macrobenchmark | `./gradlew :benchmark:connectedBenchmarkAndroidTest` |
| Baseline Profile | `./gradlew :app:generateBaselineProfile` |

---

## Rules

See [`docs/plan.md`](docs/plan.md) §3. Short version: MVI with immutable state; chart points never in state; one 1 Hz ticker per screen; no `GlobalScope`/`runBlocking` in prod code; SciChart license only in `local.properties`; module boundaries enforced by `arch-reviewer`.

---

# GeneratorMonitor (RU)

Приложение из тестового задания Android Developer (см. `task/task.pdf`): один экран с SciChart-графиком сверху и списком из 10 генераторов снизу.

## Запуск

1. Получить триальный ключ SciChart на [scichart.com](https://www.scichart.com/) и прописать в `local.properties`:
   ```properties
   sdk.dir=/path/to/Android/sdk
   scichart.license=<ваш-ключ>
   ```
   Ключ **не коммитится в git** (`local.properties` в `.gitignore`), подхватывается `AndroidApplicationConventionPlugin` в `BuildConfig.SCICHART_LICENSE` и применяется один раз в `MonitorApp.onCreate()` до создания любого `SciChartSurface`.
2. `./gradlew installDebug`.

## Правила

Источник истины — [`docs/plan.md`](docs/plan.md). Приоритет: ТЗ > правила MUST/NEVER > plan > промпт этапа.

Ключевое:
- **Два потока данных**: точки идут из репозитория в `ChartController` одним пакетом за тик (один `UpdateSuspender.using`), минуя MVI State. Список обновляется один раз в секунду через общий `secondTicker`.
- **Чекбокс завершённого генератора снимается автоматически** — выводом из `SourceStatus.Finished`, а не мутацией prefs (plan FR-LIST-04).
- **Новый источник** добавляется через `@Binds @IntoSet SignalSourceFactory` без правок экрана и графика (plan ARCH-01). Триггер — skill `new-signal-source`.

## Что ещё посмотреть

- `docs/adr/` — семь ADR с ключевыми архитектурными решениями.
- `docs/acceptance.md` — чек-лист сдачи с доказательствами (ссылки на тесты, скриншоты).
- `.claude/README.md` — состав AI-обвязки.
