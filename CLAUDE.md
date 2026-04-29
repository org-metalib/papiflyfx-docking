# CLAUDE.md

This file provides guidance to Claude Code (`claude.ai/code`) when working with code in this repository.

## Agent Team

This repository is managed by a team of specialized AI agents defined in [`AGENTS.md`](AGENTS.md). When working in this repo you **must** follow the agent operating model:

- **Role assignments, routing rules, and review gates** are defined in `AGENTS.md` and detailed per-agent in `spec/agents/`.
- **Before starting work**, identify which agent role applies (see routing rules in `AGENTS.md`). Operate within that role's primary domain and principles.
- **Cross-cutting changes** require a named lead agent and reviewers as specified in the operating model (`spec/agents/README.md`).
- **Shared contract changes** (`papiflyfx-docking-api`, `papiflyfx-docking-docks`, `*-settings-api`, `*-login-idapi`, `*-login-session-api`) require the owning specialist's review per the review gates.
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

- `papiflyfx-docking-code/` - canvas-based code editor with search, go-to-line, gutters, markers, and incremental lexing
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
- When changing dependencies or plugins, keep versions and plugin configuration centralized in the parent `pom.xml`.

## SOLID Principles

- Single Responsibility: keep modules and classes narrowly focused. Examples already present in the repo are the split between docking core, settings SPI/runtime, login SPI/runtime, and individual content modules.
- Open/Closed: extend behavior through existing extension points such as `ContentFactory`, `ContentStateAdapter`, `SettingsCategory`, `SettingsContributor`, and `ServiceLoader` providers before changing central framework code.
- Liskov Substitution: new implementations of shared contracts must behave as true substitutes. A custom factory, adapter, store, or provider should honor the same invariants, nullability expectations, and restore semantics as the built-in implementations.
- Interface Segregation: avoid large cross-cutting interfaces. If a new capability is optional or domain-specific, model it as a smaller SPI instead of widening an existing contract used by unrelated modules.
- Dependency Inversion: code against the API/SPI modules first. Feature modules should prefer `papiflyfx-docking-api`, `papiflyfx-docking-settings-api`, `papiflyfx-docking-login-idapi`, and `papiflyfx-docking-login-session-api` over direct dependencies on runtime implementations unless runtime wiring is the point of the module.

## Testing Notes

- Test stack: JUnit Jupiter `5.10.2`, TestFX `4.0.18`, Monocle `21.0.2`.
- Active test modules currently include `code`, `docks`, `github`, `hugo`, `login-idapi`, `login`, `media`, `samples`, `settings`, and `tree`.
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

<!-- gitnexus:start -->
# GitNexus — Code Intelligence

This project is indexed by GitNexus as **papiflyfx-docking** (19917 symbols, 50273 relationships, 300 execution flows). Use the GitNexus MCP tools to understand code, assess impact, and navigate safely.

> If any GitNexus tool warns the index is stale, run `npx gitnexus analyze` in terminal first.

## Always Do

- **MUST run impact analysis before editing any symbol.** Before modifying a function, class, or method, run `gitnexus_impact({target: "symbolName", direction: "upstream"})` and report the blast radius (direct callers, affected processes, risk level) to the user.
- **MUST run `gitnexus_detect_changes()` before committing** to verify your changes only affect expected symbols and execution flows.
- **MUST warn the user** if impact analysis returns HIGH or CRITICAL risk before proceeding with edits.
- When exploring unfamiliar code, use `gitnexus_query({query: "concept"})` to find execution flows instead of grepping. It returns process-grouped results ranked by relevance.
- When you need full context on a specific symbol — callers, callees, which execution flows it participates in — use `gitnexus_context({name: "symbolName"})`.

## Never Do

- NEVER edit a function, class, or method without first running `gitnexus_impact` on it.
- NEVER ignore HIGH or CRITICAL risk warnings from impact analysis.
- NEVER rename symbols with find-and-replace — use `gitnexus_rename` which understands the call graph.
- NEVER commit changes without running `gitnexus_detect_changes()` to check affected scope.

## Resources

| Resource | Use for |
|----------|---------|
| `gitnexus://repo/papiflyfx-docking/context` | Codebase overview, check index freshness |
| `gitnexus://repo/papiflyfx-docking/clusters` | All functional areas |
| `gitnexus://repo/papiflyfx-docking/processes` | All execution flows |
| `gitnexus://repo/papiflyfx-docking/process/{name}` | Step-by-step execution trace |

## CLI

| Task | Read this skill file |
|------|---------------------|
| Understand architecture / "How does X work?" | `.claude/skills/gitnexus/gitnexus-exploring/SKILL.md` |
| Blast radius / "What breaks if I change X?" | `.claude/skills/gitnexus/gitnexus-impact-analysis/SKILL.md` |
| Trace bugs / "Why is X failing?" | `.claude/skills/gitnexus/gitnexus-debugging/SKILL.md` |
| Rename / extract / split / refactor | `.claude/skills/gitnexus/gitnexus-refactoring/SKILL.md` |
| Tools, resources, schema reference | `.claude/skills/gitnexus/gitnexus-guide/SKILL.md` |
| Index, status, clean, wiki CLI commands | `.claude/skills/gitnexus/gitnexus-cli/SKILL.md` |

<!-- gitnexus:end -->
