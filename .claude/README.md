# Claude Code setup for GeneratorMonitor

Трекается в git намеренно — все, кто склонируют проект, получают одинаковую AI-обвязку: skills, субагенты, хуки, MCP-конфиг. Детали архитектуры и правил — в `/CLAUDE.md` и `/docs/plan.md`.

## Что здесь лежит

```
.claude/
├── agents/                              # Субагенты (Agent tool)
│   ├── arch-reviewer.md                 — проверка слоёв/модулей по plan §3-4 (plan)
│   ├── perf-reviewer.md                 — проверка перф-правил M3/N1/N4/N6/N7 (plan)
│   ├── compose-ui-builder.md            — построение Compose UI, no-comment стиль
│   ├── clean-architecture-reviewer.md   — общее Clean Architecture ревью (дубль arch-reviewer, более общий)
│   ├── kotlin-flow-expert.md            — coroutines/Flow рефакторинг
│   ├── feature-scaffolder.md            — быстрый vertical slice (дубль new-feature-module, более общий)
│   └── android-tester.md                — unit + Compose UI тесты (JUnit4/Kotest)
├── skills/
│   ├── new-feature-module/SKILL.md      — модуль feature:<name> + MVI
│   ├── new-signal-source/SKILL.md       — новый SignalSource + @IntoSet
│   ├── mvi-screen/SKILL.md              — правила MVI-экрана (M6-M8, N1, N5, N6)
│   ├── scichart-chart/SKILL.md          — работа с SciChart (M3, N7, N8, N9)
│   ├── compose-performance/SKILL.md     — стабильность, рекомпозиция, отчёты
│   ├── verify/SKILL.md                  — Gate: spotless+detekt+lint+tests+build
│   └── material-expressive/SKILL.md     — опционально: M3 Expressive + Threads palette
├── hooks/
│   ├── format-kotlin.sh                 — PostToolUse: spotlessApply затронутого модуля
│   └── compile-changed.sh               — Stop: компиляция изменённых модулей
├── settings.json                        — permissions, PostToolUse + Stop хуки
└── README.md                            — этот файл
```

Плюс в корне:
- `/CLAUDE.md` — выжимка правил, ссылается на plan §N.
- `/docs/plan.md` — источник истины (ТЗ, MUST/NEVER, модули, этапы).
- `/docs/adr/` — ADR 001-005: ключевые решения.
- `/.mcp.json` — Context7 + mobile-mcp + GitHub MCP.
- `/local.properties.example` — пустой `scichart.license=`; реальный ключ в `local.properties` (не в git).

## Триггеры (из plan §6)

| Задача | Инструмент |
|---|---|
| «добавь экран / фичу» | skill `new-feature-module` |
| «подключи источник точек» | skill `new-signal-source` |
| правка `feature:*/presentation/*` | skill `mvi-screen` |
| правка `core:chart/*` | skill `scichart-chart` |
| ревью UI-кода | skill `compose-performance` |
| конец этапа / перед коммитом | skill `verify` |
| конец этапов 5, 6, 8, 11, 12 | субагент `arch-reviewer` |
| конец этапов 10, 11, 12 | субагент `perf-reviewer` |
| версии и API библиотек | MCP `context7` |
| проверка UI на эмуляторе | MCP `mobile-mcp` |
| PR / issues | MCP `github` |

## MCP

- `context7` (`npx -y @upstash/context7-mcp`) — свежая документация и версии (правило M11).
- `mobile-mcp` (`npx -y @mobilenext/mobile-mcp@latest`) — ADB-управление устройством и эмулятором, скриншоты, жесты. Нужен `adb` в PATH.
- `github` (HTTP MCP) — PR, issues, обзор diff. Требует env-переменную `GITHUB_PAT` (**не коммить токен в репо, N9**).

На первом запуске Claude Code запросит approve для каждого MCP-сервера.

## Hooks

- `format-kotlin.sh` — срабатывает на `Edit/Write/MultiEdit` *.kt/*.kts: определяет модуль по пути, вызывает `<module>:spotlessApply`. Молча не падает.
- `compile-changed.sh` — срабатывает на `Stop` агента: собирает список изменённых `.kt`/`.kts` через git, компилирует соответствующие `<module>:compileDebugKotlin`. Если git не инициализирован — ничего не делает.

## Personal / local

Игнорятся в `/.gitignore`:
- `.claude/settings.local.json` — личные настройки Claude Code.
- `.claude/transcripts/` — транскрипты сессий.
- `.claude/.cache/` — кэш.
