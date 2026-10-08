# GeneratorMonitor — правила для агента

Источник истины: [`docs/plan.md`](docs/plan.md). При конфликте: **ТЗ > MUST/NEVER (plan §3) > plan > промпт этапа > решение агента**. Любое отклонение от плана = новый ADR в `docs/adr/`.

Перед любой задачей: прочитай plan §3 (правила) → plan §4 (архитектура) → свой этап в plan §7 → промпт `task/prompts/NN-*.md`.

## Модули (plan §4)

`app` · `core:common` · `core:domain` (pure Kotlin) · `core:data` · `core:ui` (theme + palette + typography) · `core:chart` · `core:testing` · `feature:monitor` · `benchmark`

- Слои: `presentation → domain ← data`. `feature:*` зависит от `domain`, `ui`, `chart`. **`feature:*` никогда не зависит от `core:data`.**
- Новый код кладётся строго в модуль из таблицы plan §4; зависимости вне колонки «Может зависеть от» запрещены.

## Правила (plan §3)

**MUST**
- **M1** Слои: presentation → domain ← data. Domain — чистый Kotlin.
- **M2** Источники данных живут в app-scope репозитории-синглтоне, не во ViewModel.
- **M3** Точки идут из репозитория напрямую в `ChartController` и добавляются одним пакетом за тик.
- **M4** Монотонные часы (инжектируемый `MonotonicClock`) для дедлайнов, таймеров, тиков.
- **M5** `Random`, `Clock`, диспетчеры — только через конструктор.
- **M6** MVI: иммутабельный State, чистый редьюсер, Intent — единственный вход, Effect для one-shot.
- **M7** UI-модели стабильны; коллекции — immutable; стабильные ключи в `LazyColumn` по `id`.
- **M8** `collectAsStateWithLifecycle()` — состояние с учётом lifecycle.
- **M9** Все версии — в version catalog; настройка сборки — в convention-плагинах.
- **M10** Каждый этап = зелёный `verify` + один коммит со ссылками на ID.
- **M11** Версии и API — только из Context7 / официальной документации.

**NEVER**
- **N1** Точки графика в MVI State или в Compose-состоянии.
- **N2** Android, SciChart, Compose-типы в `core:domain`.
- **N3** `feature:*` → `core:data`.
- **N4** `GlobalScope`, `runBlocking` в прод-коде, блокирующие вызовы на main.
- **N5** Логика, сортировка, форматирование внутри Composable.
- **N6** Таймер на каждый элемент списка — только один общий тик 1 Гц.
- **N7** Пересоздание серий / поверхности SciChart при переключении видимости или новых точках.
- **N8** Авто-скейл по оси X.
- **N9** Ключ SciChart или другие секреты в git.
- **N10** Правки вне своего этапа без ADR.

## Два потока данных (plan §4)

1. **Быстрый (точки):** генератор → `SignalRepository.batches` → `ChartController` → SciChart. **Compose не участвует (M3, N1).**
2. **Медленный (список):** `sources` + общий тик 1 Гц + видимость → редьюсер на `Default` → `StateFlow<MonitorState>` → `LazyColumn`.
3. **Видимость:** чекбокс → Intent → `State.visibleIds` → `ChartController.setVisible(...)` (флагом серии, не пересозданием, N7).

## Тесты

- Юнит-тесты — **только Kotest** (FunSpec / BehaviorSpec) + `kotest-property` + Turbine. Асинхронность — на виртуальном времени (`coroutineTestScope = true`).
- Инструментальные (Compose UI, Macrobenchmark, Baseline Profile) — **AndroidX JUnit4** (Kotest не запускается на устройстве).
- Имя теста начинается с ID требования: `test("FR-GEN-05 one point per second without drift") { … }`.
- `Random`, `Clock`, диспетчеры прокидываются через конструктор — тесты детерминированы.

## AI-обвязка (plan §6)

| Триггер | Инструмент |
|---|---|
| «добавь экран/фичу» | skill `new-feature-module` |
| «подключи источник» | skill `new-signal-source` |
| правка `presentation/` | skill `mvi-screen` |
| правка графика | skill `scichart-chart` |
| ревью UI | skill `compose-performance` |
| конец этапа | skill `verify` |
| конец этапов 5, 6, 8, 11, 12 | субагент `arch-reviewer` |
| конец этапов 10, 11, 12 | субагент `perf-reviewer` |
| версии / API библиотек | MCP `context7` |
| проверка UI на эмуляторе | MCP `mobile-mcp` |

Секреты MCP — только через `${ENV}`. SciChart docs — через `WebFetch` с scichart.com.

## Команды (plan §8)

| Что | Команда |
|---|---|
| Сборка | `./gradlew assembleDebug` |
| Качество | `./gradlew spotlessCheck detekt lint` |
| Юнит | `./gradlew testDebugUnitTest` |
| UI | `./gradlew connectedDebugAndroidTest` |
| Отчёты Compose | `./gradlew assembleRelease -PcomposeReports` |
| Бенчмарки | `./gradlew :benchmark:connectedBenchmarkAndroidTest` |
| Baseline Profile | `./gradlew :app:generateBaselineProfile` |

## Решения по умолчанию (plan §9)

- Пакет: `com.dauren.monitor`. `minSdk = 26`, `targetSdk = 37`, `compileSdk = 37`.
- Ширина окна X: последние 2 минуты. Двойной тап возвращает к ним.
- Отступ по Y: 10% сверху/снизу.
- Формат значения: `%+.2f`. Таймер завершённого: `00:00`, строка приглушена, чекбокс выключен.
- Ёмкость буфера на источник: 500 000 точек (конфиг).
- Раскладка: портрет — график сверху ~55%, список снизу; ландшафт — рядом.
- Тема: светлая + тёмная по системной.
- Код и коммиты — английский; README — EN + RU.

## Коммиты (plan §7)

```
<type>(<scope>): <что сделано>

Refs: FR-…, PERF-…, ARCH-…
```

Один этап = один коммит. Следующий этап не начинается, пока не пройден Gate текущего.

## Куда класть новое

- **Новый источник точек** → `core:data/.../source/` + фабрика + `@Binds @IntoSet` в `SourceFactoriesModule`. Экран и график НЕ меняются. Триггер skill `new-signal-source`.
- **Новый экран** → `feature:<name>/` по шаблону `feature:monitor`. Триггер skill `new-feature-module`.
- **Новый use case** → `core:domain/.../usecase/`.
- **Новый тест** → рядом с кодом, имя с ID требования.

## ADR

Решения записаны в `docs/adr/`. Новый ADR — на каждое отклонение от плана.
