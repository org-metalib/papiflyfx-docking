# PapiflyFX Docking Samples — Implementation Plan

## Prerequisites

- `papiflyfx-docking-docks` published to local Maven repository (or built in the same reactor).
- `papiflyfx-docking-code` published to local Maven repository (or built in the same reactor).
- Java 25, Maven 3.9+.

## Phase 0 — Module Bootstrap

**Goal**: compilable, empty module wired into the root aggregator.

Tasks:
1. Create `papiflyfx-docking-samples/pom.xml`:
   - Parent: `papiflyfx-docking` root.
   - Compile dependencies: `papiflyfx-docking-docks`, `papiflyfx-docking-code`.
   - Test dependencies: JUnit 5, TestFX, Monocle.
   - `javafx-maven-plugin` with `mainClass = org.metalib.papifly.fx.samples.SamplesApp`.
   - `maven-surefire-plugin` with headless JVM args (same as sibling modules).
2. Add `<module>papiflyfx-docking-samples</module>` to root `pom.xml`.
3. Create package `org.metalib.papifly.fx.samples` with stub `SamplesApp` (empty `start()`).
4. Create `SampleLauncher` with `main(String[] args) { Application.launch(SamplesApp.class, args); }`.

Validation: `mvn compile -pl papiflyfx-docking-samples -am` succeeds.

## Phase 1 — Shell and Catalog

**Goal**: navigable application shell with placeholder content area.

Tasks:
1. Define `SampleScene` interface (`category()`, `title()`, `build(Stage, ObjectProperty<Theme>) → Node`).
2. Implement `SampleCatalog.all()` returning stubs for all 10 entries (each returning a `Label("TODO")`).
3. Implement `SamplesApp.start()`:
   - Left `ListView<SampleDescriptor>` grouped by category label separators.
   - Right `StackPane` content area.
   - Top bar with title `Label` and dark/light `ToggleButton` wired to `themeProperty`.
   - Selection listener calls `sample.build(stage, themeProperty)` and replaces right pane.
4. Wire `themeProperty` (`ObjectProperty<Theme>` initialized to `Theme.DARK`).

Validation: app starts, all 10 sample names visible, clicking each shows `"TODO"` label.

## Phase 2 — Docks Samples

**Goal**: all six layout samples fully functional.

Tasks (one per sample class, in order):

1. **`BasicSplitSample`** — two leaves + vertical split at 0.7.
2. **`NestedSplitSample`** — horizontal outer split (0.25 / 0.75) + vertical inner split (0.7 / 0.3).
3. **`TabGroupSample`** — two tab groups with 3 and 2 leaves respectively.
4. **`FloatingSample`** — `dockManager.setOwnerStage(ownerStage)`, one detachable leaf.
5. **`MinimizeSample`** — toolbar button to minimize/restore a leaf programmatically.
6. **`PersistSample`** — Save/Restore buttons; JSON display overlay using a `TextArea`.

Each sample:
- Registers `themeProperty` on its `DockManager`.
- Returns `dockManager.getRootPane()` wrapped in a `BorderPane` with optional toolbar.

Validation: manual walkthrough of each sample; no uncaught exceptions in console.

## Phase 3 — Code Editor Samples

**Goal**: all four code editor samples functional with syntax highlighting and theme binding.

Tasks:
1. **`MarkdownEditorSample`** — `languageId = "markdown"`, inline README-style text.
2. **`JavaEditorSample`** — `languageId = "java"`, inline `HelloWorld.java` snippet.
3. **`JavaScriptEditorSample`** — `languageId = "javascript"`, inline ES module snippet.
4. **`JsonEditorSample`** — `languageId = "json"`, inline package manifest snippet.

Each sample:
- Creates `ContentStateRegistry` with `CodeEditorStateAdapter`.
- Sets `ContentFactory` to `new CodeEditorFactory()`.
- Creates `CodeEditor`, sets text and language, binds `themeProperty`.
- Wraps in `DockLeaf` → `DockTabGroup` → `DockManager.setRoot(...)`.
- Returns `dockManager.getRootPane()`.

Validation: all four editors display coloured tokens; theme toggle visually changes palette.

## Phase 4 — Smoke Test and Hardening

**Goal**: all acceptance criteria passing in headless CI.

Tasks:
1. Implement `SamplesSmokeTest`:
   - Extends `ApplicationTest` (TestFX + JUnit 5).
   - Iterates `SampleCatalog.all()`, clicks each item in the `ListView`.
   - Waits for render with `WaitForAsyncUtils.waitForFxEvents()`.
   - Asserts no uncaught exception via `Thread.getDefaultUncaughtExceptionHandler()`.
   - For `PersistSample`: clicks Save, asserts JSON text non-empty; clicks Restore, asserts no error.
2. Verify headless execution: `mvn -pl papiflyfx-docking-samples -am -Dtestfx.headless=true test`.
3. Confirm total test runtime < 60 s on baseline hardware.
4. Update root `README.md` to mention the samples module and how to run it.

Validation: CI green, all 10 AC rows satisfied.

## Milestone Summary

| Milestone | Condition |
|---|---|
| M0 | Module compiles in reactor |
| M1 | App starts with full catalog navigation |
| M2 | All six docking layout samples functional |
| M3 | All four code editor samples functional |
| M4 | Headless smoke test passes, all acceptance criteria met |
