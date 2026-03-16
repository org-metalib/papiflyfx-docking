# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

PapiflyFX Docking — a multi-module JavaFX docking framework for IDE-style layouts.
- groupId: `org.metalib.papifly.docking`, version `0.0.15-SNAPSHOT`
- Java 25, JavaFX 23.0.1, Maven 3.9+
- Package prefix: `org.metalib.papifly.fx`
- No FXML, no CSS — all programmatic JavaFX

## Build Commands

```bash
# Java setup (SDKMAN)
sdk use java 25.0.1.fx-zulu

# Build all
./mvnw clean package

# Test all (headless for CI/no-display)
./mvnw test -Dtestfx.headless=true

# Single module
./mvnw -pl papiflyfx-docking-docks -am clean package
./mvnw -pl papiflyfx-docking-code -am -Dtestfx.headless=true test

# Single test class
./mvnw -Dtest=SamplesSmokeTest test -pl papiflyfx-docking-samples -am

# Run samples demo app
./mvnw javafx:run -pl papiflyfx-docking-samples

# Run docks standalone demo
./mvnw javafx:run -pl papiflyfx-docking-docks

# Code editor benchmarks (excluded by default)
./mvnw -pl papiflyfx-docking-code -am -Dtestfx.headless=true -Dgroups=benchmark -Dsurefire.excludedGroups= test
```

## Module Structure

```
papiflyfx-docking-api/      → Public interfaces & records (Theme, ContentFactory, ContentStateAdapter, LeafContentData, DisposableContent)
papiflyfx-docking-docks/    → Core docking framework (DockManager, DockLeaf, DockTabGroup, DockSplitGroup, serialization)
papiflyfx-docking-code/     → Canvas-based code editor (CodeEditor, lexers, search, gutter)
papiflyfx-docking-tree/     → Canvas-based virtualized tree component
papiflyfx-docking-media/    → JavaFX media viewer (uses javafx-media + javafx-web)
papiflyfx-docking-hugo/     → Dockable Hugo preview (WebView, managed hugo server lifecycle)
papiflyfx-docking-github/   → GitHub workflow toolbar (JGit-based: branch, commit, push, PR creation, PAT auth)
papiflyfx-docking-samples/  → Runnable demo app (NOT published to Maven Central)
```

Dependency flow: `api` ← `docks` ← `{code, tree, media, hugo, github}` ← `samples`
All content modules depend on `api` at compile scope and `docks` at test scope only.

## Architecture

### DockManager Internal Layout

```
mainContainer (BorderPane)
├── center: rootPane (StackPane)
│   ├── dockingLayer   ← DockElement nodes
│   └── overlayLayer   ← drag drop-zone hints (OverlayCanvas)
└── bottom: minimizedBar
```

### Core Types (docks)

- **DockElement** — base interface: `getNode()`, `serialize()`, `dispose()`
- **DockLeaf** — terminal content node, rendered by DockTabGroup. Has metadata (`DockData`), content `Node`, optional `contentFactoryId` for session restore
- **DockTabGroup** — tab bar + active content panel
- **DockSplitGroup** — SplitPane-like with two children, orientation, divider position
- **DockState** — enum: `DOCKED`, `FLOATING`, `MINIMIZED`, `MAXIMIZED`

### Serialization (no external JSON library)

- `LayoutNode` — sealed hierarchy for JSON: `TabGroupData`, `SplitData`, `LeafData`
- `DockSessionData` — full session: layout + floating + minimized + maximized
- `DockSessionSerializer` uses `java.util.Map`-based JSON

### CodeEditor Architecture (code module)

`CodeEditor` extends `StackPane`, implements `DisposableContent`. Canvas-based virtualized rendering with:
- `Document` (text model), `Viewport`, `SelectionModel`, `MultiCaretModel`
- `GutterView`, `MarkerModel`, `SearchController`, `GoToLineController`
- `IncrementalLexerPipeline` with language-specific lexers (Java/JS/JSON/Markdown/PlainText)
- Controller pattern: `EditorInputController`, `EditorEditController`, `EditorPointerController`, `EditorNavigationController`

### Content Module Pattern

Each content module (code, tree, media, hugo, github) follows the same integration pattern:
1. A main content `Node` (e.g., `CodeEditor`, `GitHubToolbarContribution`)
2. A `ContentFactory` implementation for creating content from leaf data
3. A `ContentStateAdapter` implementation for session persistence
4. ServiceLoader registration for content-state restore

## Critical API Patterns

- `DockManager.createTabGroup()` already calls `setupTabGroupDragHandlers` internally — never call it again
- Cast explicitly when calling `setRoot`: `dm.setRoot((DockElement) group)` to resolve overload ambiguity with `setRoot(LayoutNode)`
- Register `ContentStateAdapter` BEFORE setting the content factory:
  ```java
  ContentStateRegistry.register(new CodeEditorStateAdapter());  // first
  dm.setContentFactory(new CodeEditorFactory());                // then
  ```
- `leaf.setContentFactoryId(CodeEditorFactory.FACTORY_ID)` — required for session restore
- `editor.bindThemeProperty(dm.themeProperty())` — live theme sync for code editor

## Maven Module Guidelines

- All dependency versions managed in parent pom via properties
- All plugin versions managed in parent pom `<pluginManagement>`
- Modules should be self-contained with clear boundaries
- Use `./mvnw` (Maven Wrapper), not bare `mvn`

## Test Setup

- JUnit Jupiter 5.10.2 + TestFX 4.0.18 + Monocle 21.0.2 (headless)
- Profile `headless-tests` activated by `-Dtestfx.headless=true`
- Surefire requires `--enable-native-access=javafx.graphics` + multiple `--add-exports`/`--add-opens` flags (configured in module POMs)
- `useModulePath=false` in all Surefire configs
- Naming: `*Test` for unit tests, `*FxTest` for UI tests, `benchmark` JUnit tag for benchmarks

## Reference Docs

- Architecture specs: `spec/`
- Docks module README: `papiflyfx-docking-docks/README.md`
- Code editor README: `papiflyfx-docking-code/README.md`
- Hugo module README: `papiflyfx-docking-hugo/README.md`
- GitHub module README: `papiflyfx-docking-github/README.md`
- Agent guidelines: `AGENTS.md`