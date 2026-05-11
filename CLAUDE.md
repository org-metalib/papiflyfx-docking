# CLAUDE.md

This file provides guidance to Claude Code (`claude.ai/code`) when working with code in this repository.

## Agent Team

This repository is managed by a team of specialized AI agents defined in [`AGENTS.md`](AGENTS.md). When working in this repo you **must** follow the agent operating model:

- **Role assignments, routing rules, and review gates** are defined in `AGENTS.md` and detailed per-agent in `spec/agents/`.
- **Before starting work**, identify which agent role applies (see routing rules in `AGENTS.md`). Operate within that role's primary domain and principles.
- **Cross-cutting changes** require a named lead agent and reviewers as specified in the operating model (`spec/agents/README.md`).
- **Shared contract changes** (`papiflyfx-docking-api`, `papiflyfx-docking-docks`, `*-settings-api`, `*-login-idapi`, `*-login-session-api`) require the owning specialist's review per the review gates.
- **API/SPI design default**: do not try to preserve backward compatibility with previous versions unless the task or plan explicitly requires it. Prefer the clearest current contract over compatibility shims or legacy overloads.
- **Handoffs** between agent roles must follow the handoff contract format in `spec/agents/README.md`.
- **Priority classification** (P0–P3) and **escalation rules** in `spec/agents/README.md` govern task sequencing and conflict resolution.

When the user invokes a specific agent handle (e.g., _"As @core-architect..."_), adopt that role's focus areas, key principles, and task guidance from the corresponding `spec/agents/<role>.md` file.

## Project Overview

PapiflyFX Docking is a multi-module Java/JavaFX framework for IDE-style docking layouts: tab groups, nested splits, floating windows, minimize/maximize flows, and JSON session persistence.

- groupId: `org.metalib.papifly.docking`
- version: `0.0.15-SNAPSHOT`
- Java: `25`
- JavaFX: `23.0.1`
- Maven: `3.9+` via `./mvnw`
- package prefix: `org.metalib.papifly.fx`

Current UI convention:
- no FXML
- mostly programmatic JavaFX
- limited module-local CSS resources exist in `papiflyfx-docking-code`, `papiflyfx-docking-tree`, and `papiflyfx-docking-github` for overlays and theme support

Build/test defaults:
- the root POM defaults `testfx.headless=true` for reliable CI and sandbox runs
- JavaFX classifier selection is handled by OS-specific Maven profiles in the root POM

## Build Commands

```bash
# Java setup (SDKMAN)
sdk use java 25.0.1.fx-zulu

# Compile all modules
./mvnw compile

# Full build
./mvnw clean package

# Test all modules
./mvnw test

# Explicit headless test run
./mvnw -Dtestfx.headless=true test

# Interactive UI test run
./mvnw -Dtestfx.headless=false test

# Focused module build/test
./mvnw -pl papiflyfx-docking-docks -am clean package
./mvnw -pl papiflyfx-docking-code -am test
./mvnw -pl papiflyfx-docking-login -am test

# Single test class
./mvnw -pl papiflyfx-docking-samples -am -Dtest=SamplesSmokeTest test

# Run demo apps
./mvnw javafx:run -pl papiflyfx-docking-docks
./mvnw javafx:run -pl papiflyfx-docking-samples

# Code editor benchmarks (excluded by default)
./mvnw -pl papiflyfx-docking-code -am -Dtestfx.headless=true -Dgroups=benchmark -Dsurefire.excludedGroups= test
```

## Module Structure

### Shared contracts and SPI

- `papiflyfx-docking-api/` - public docking/content/theme interfaces and DTOs
- `papiflyfx-docking-settings-api/` - public settings, storage, validation, and secret-management SPI
- `papiflyfx-docking-login-idapi/` - identity-provider SPI plus built-in providers (`GitHub`, `Google`, generic OIDC)
- `papiflyfx-docking-login-session-api/` - auth session lifecycle, store, and secret-store abstractions

### Core/runtime modules

- `papiflyfx-docking-docks/` - core docking framework (`DockManager`, `DockLeaf`, `DockTabGroup`, `DockSplitGroup`, serialization, floating windows, minimized bar)
- `papiflyfx-docking-settings/` - dockable settings runtime and UI (`SettingsRuntime`, categories, JSON persistence, secure secret-store backends)
- `papiflyfx-docking-login/` - authentication orchestrator, settings integration, session broker, and dockable login UI

### Content modules

- `papiflyfx-docking-code/` - canvas-based code editor with search, go-to-line, gutters, markers, language SPI, syntax style SPI, per-language settings resolver, and plain-text fallback
- `papiflyfx-docking-code-java/` - Java lexer/fold language pack discovered through ServiceLoader
- `papiflyfx-docking-code-javascript/` - JavaScript lexer/fold language pack discovered through ServiceLoader
- `papiflyfx-docking-code-json/` - JSON lexer, fold provider, tests, and ServiceLoader language contribution for the code editor
- `papiflyfx-docking-code-yaml/` - YAML lexer, fold provider, tests, and ServiceLoader language contribution for the code editor
- `papiflyfx-docking-code-markdown/` - Markdown lexer, fold provider, tests, and ServiceLoader language contribution for the code editor
- `papiflyfx-docking-tree/` - canvas-based virtualized tree view with search overlay and theme integration
- `papiflyfx-docking-media/` - media/image/video viewer content
- `papiflyfx-docking-hugo/` - Hugo preview content with managed preview lifecycle
- `papiflyfx-docking-github/` - GitHub toolbar contribution backed by JGit and JavaFX UI

### Demo module

- `papiflyfx-docking-samples/` - runnable demo application covering docks, code, tree, media, settings, login, Hugo, and GitHub integrations

### Dependency Notes

- `papiflyfx-docking-api` is the base docking/content contract module.
- `papiflyfx-docking-docks` depends on `papiflyfx-docking-api`.
- `papiflyfx-docking-settings-api` depends on `papiflyfx-docking-api`.
- `papiflyfx-docking-settings` depends on `settings-api`, `api`, and `docks`.
- `papiflyfx-docking-code`, `papiflyfx-docking-hugo`, and `papiflyfx-docking-github` also depend on `settings-api`.
- `papiflyfx-docking-code-java`, `papiflyfx-docking-code-javascript`, `papiflyfx-docking-code-json`, `papiflyfx-docking-code-yaml`, and `papiflyfx-docking-code-markdown` depend on `papiflyfx-docking-code` and are discovered through `ServiceLoader<LanguageSupportProvider>`; language-specific syntax colors use `ServiceLoader<SyntaxStyleProvider>`.
- `papiflyfx-docking-login-session-api` depends on `login-idapi` and `settings-api`.
- `papiflyfx-docking-login` depends on `login-idapi`, `login-session-api`, `api`, `settings-api`, and `docks`.
- `papiflyfx-docking-samples` pulls together the runtime/content modules for demos and smoke coverage.

## Architecture

### DockManager Internal Layout

```text
mainContainer (BorderPane)
├── center: rootPane (StackPane)
│   ├── dockingLayer   ← DockElement nodes
│   └── overlayLayer   ← drag/drop-zone hints (OverlayCanvas)
└── bottom: minimizedBar
```

### Core Types

- `DockManager` - main entry point, owns theme, root layout, floating manager, minimized store, and session capture/restore
- `DockElement` - base layout node abstraction
- `DockLeaf` - terminal content node with metadata (`DockData`) and optional `contentFactoryId`
- `DockTabGroup` - tab host for one or more leaves
- `DockSplitGroup` - split container with orientation and divider position
- `DockState` - `DOCKED`, `FLOATING`, `MINIMIZED`, `MAXIMIZED`
- `FloatingWindowManager` and `FloatingDockWindow` - floating-window support
- `MinimizedStore` and `MinimizedBar` - minimized leaf lifecycle

### Session Persistence

- `LayoutNode` is the layout data model used for JSON serialization.
- `DockSessionData` captures layout plus floating/minimized/maximized state.
- Serialization lives in `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/serial/`.
- `DockSessionSerializer` and `DockSessionPersistence` use `java.util.Map`-style JSON handling instead of an external JSON library.

### Content Integration Pattern

Every restorable dockable content type follows the same shape:

1. A `ContentFactory` that recreates the content node from a factory id.
2. A `ContentStateAdapter` that saves/restores module-specific state.
3. A matching `FACTORY_ID` / type key pair.
4. A `DockLeaf` whose `contentFactoryId` is set before persistence.
5. Optional `bindThemeProperty(...)` support for live theme propagation.

Current examples:
- `CodeEditorFactory` + `CodeEditorStateAdapter`
- `TreeViewFactory` + `TreeViewStateAdapter`
- `MediaViewerFactory` + `MediaViewerStateAdapter`
- `HugoPreviewFactory` + `HugoPreviewStateAdapter`
- `GitHubToolbarFactory` + `GitHubToolbarStateAdapter`
- `SettingsContentFactory` + `SettingsStateAdapter`
- `LoginFactory` + `LoginStateAdapter`

## Working Conventions

- Use `./mvnw`, not bare `mvn`.
- `DockManager#createTabGroup()` already wires drag and tab-button handlers.
- `DockManager` overloads `setRoot(DockElement)` and `setRoot(LayoutNode)`; cast to `DockElement` if an inline call becomes ambiguous.
- Prefer `ContentStateRegistry.fromServiceLoader()` when the module already ships `META-INF/services` descriptors. Otherwise create a registry and register adapters explicitly.
- Set the `ContentStateRegistry` on `DockManager` before restoring sessions/layouts that depend on custom content.
- Set `leaf.setContentFactoryId(...)` for any content that should survive session capture/restore.
- Call `setOwnerStage(stage)` before relying on floating behavior; `DockManager` can resolve an attached scene window, but explicit setup is safer.
- Content modules that only need settings SPI should depend on `papiflyfx-docking-settings-api`, not the `papiflyfx-docking-settings` runtime implementation.
- Code editor language packs should contribute languages through `LanguageSupportProvider` and a `META-INF/services` descriptor. Language-specific colors should use `SyntaxStyleProvider`, and per-language defaults should be supplied through `LanguageEditorDefaults`; avoid editing `TokenType`, `FoldKind`, or `CodeEditorTheme` for each new language.
- When changing dependencies or plugins, keep versions and plugin configuration centralized in the parent `pom.xml`.

## SOLID Principles

- Single Responsibility: keep modules and classes narrowly focused. Examples already present in the repo are the split between docking core, settings SPI/runtime, login SPI/runtime, and individual content modules.
- Open/Closed: extend behavior through existing extension points such as `ContentFactory`, `ContentStateAdapter`, `SettingsCategory`, `SettingsContributor`, and `ServiceLoader` providers before changing central framework code.
- Liskov Substitution: new implementations of shared contracts must behave as true substitutes. A custom factory, adapter, store, or provider should honor the same invariants, nullability expectations, and restore semantics as the built-in implementations.
- Interface Segregation: avoid large cross-cutting interfaces. If a new capability is optional or domain-specific, model it as a smaller SPI instead of widening an existing contract used by unrelated modules.
- Dependency Inversion: code against the API/SPI modules first. Feature modules should prefer `papiflyfx-docking-api`, `papiflyfx-docking-settings-api`, `papiflyfx-docking-login-idapi`, and `papiflyfx-docking-login-session-api` over direct dependencies on runtime implementations unless runtime wiring is the point of the module.

## Testing Notes

- Test stack: JUnit Jupiter `5.10.2`, TestFX `4.0.18`, Monocle `21.0.2`.
- Active test modules currently include `code`, `code-java`, `code-javascript`, `code-json`, `code-yaml`, `code-markdown`, `docks`, `github`, `hugo`, `login-idapi`, `login`, `media`, `samples`, `settings`, and `tree`.
- `papiflyfx-docking-code` excludes `benchmark` tests by default through Surefire configuration.
- UI-heavy modules disable the module path in Surefire and already include the necessary `--enable-native-access`, `--add-exports`, and `--add-opens` flags.
- Several UI modules expose a `headless-tests` Maven profile activated by `-Dtestfx.headless=true`.

## Reference Docs

- root overview: `README.md`
- **agent team & operating model**: `AGENTS.md` (required — defines roles, routing, review gates)
- agent role specs: `spec/agents/` (per-agent focus areas, principles, task guidance)
- agent shared protocol: `spec/agents/README.md` (workflow, handoffs, escalation, priorities)
- architecture/specs: `spec/`
- docks docs: `papiflyfx-docking-docks/README.md`
- code docs: `papiflyfx-docking-code/README.md`
- Hugo docs: `papiflyfx-docking-hugo/README.md`
- GitHub docs: `papiflyfx-docking-github/README.md`
- settings API docs: `papiflyfx-docking-settings-api/README.md`
- settings runtime docs: `papiflyfx-docking-settings/README.md`
- login docs: `papiflyfx-docking-login/README.md`

## GitNexus — Code Intelligence

This project is indexed by GitNexus. Use the GitNexus MCP tools to understand code, assess impact, and navigate safely. If a GitNexus tool warns the index is stale, run `npx gitnexus analyze` in terminal.

For tool usage, impact analysis, refactoring, debugging, and CLI commands see the GitNexus skill files under `.claude/skills/gitnexus/`. This section is maintained manually — the auto-injected stats block (symbol/relationship/execution-flow counts) was removed because it churned on every index refresh. To stop it from coming back, see `Preserving CLAUDE.md / AGENTS.md across re-indexes` below.

### Preserving CLAUDE.md / AGENTS.md across re-indexes

`npx gitnexus analyze` accepts `--skip-agents-md`, which keeps the analyzer from regenerating the `CLAUDE.md` / `AGENTS.md` context files. Always use this flag here, both for manual runs and for the Claude Code PostToolUse hook that fires on `git commit` / `git merge`:

```bash
# Manual re-index
npx gitnexus analyze --skip-agents-md
```

The hook lives at `~/.claude/hooks/gitnexus/gitnexus-hook.cjs` (registered in `~/.claude/settings.json`). Edit that script to pass `--skip-agents-md` to its `npx gitnexus analyze ...` invocation so the auto-run on commit/merge no longer rewrites these files.
