# PapiflyFX Docking Samples — Implementation Plan

## Status: COMPLETE ✓

All four phases delivered. Headless smoke test passes in ~2 s (reactor ~40 s total, well under the 60 s gate).

## Prerequisites

- `papiflyfx-docking-docks` published to local Maven repository (or built in the same reactor).
- `papiflyfx-docking-code` published to local Maven repository (or built in the same reactor).
- Java 25, Maven 3.9+.

## Phase 0 — Module Bootstrap ✓ DONE

**Goal**: compilable, empty module wired into the root aggregator.

Tasks:
1. ✓ Create `papiflyfx-docking-samples/pom.xml`:
   - Parent: `papiflyfx-docking` root.
   - Compile dependencies: `papiflyfx-docking-docks`, `papiflyfx-docking-code`.
   - Test dependencies: JUnit 5, TestFX, Monocle.
   - `javafx-maven-plugin` with `mainClass = org.metalib.papifly.fx.samples.SamplesApp`.
   - `maven-surefire-plugin` with headless JVM args (same as sibling modules).
2. ✓ Add `<module>papiflyfx-docking-samples</module>` to root `pom.xml`.
3. ✓ Create package `org.metalib.papifly.fx.samples` with `SamplesApp`.
4. ✓ Create `SampleLauncher` with `main(String[] args) { Application.launch(SamplesApp.class, args); }`.

Validation: `mvn compile -pl papiflyfx-docking-samples -am` succeeds. ✓

## Phase 1 — Shell and Catalog ✓ DONE

**Goal**: navigable application shell with placeholder content area.

Tasks:
1. ✓ Define `SampleScene` interface (`category()`, `title()`, `build(Stage, ObjectProperty<Theme>) → Node`).
2. ✓ Implement `SampleCatalog.all()` returning all 10 entries in display order.
3. ✓ Implement `SamplesApp.start()`:
   - Left `ListView<Object>` grouped by category label separators (non-selectable headers).
   - Center `StackPane` content area with placeholder label.
   - Top bar with title `Label` and dark/light `ToggleButton` wired to `themeProperty`.
   - Selection listener calls `sample.build(stage, themeProperty)` and replaces center pane.
4. ✓ Wire `themeProperty` (`ObjectProperty<Theme>` initialized to `Theme.dark()`).

Validation: app starts, all 10 sample names visible, clicking each builds the sample. ✓

## Phase 2 — Docks Samples ✓ DONE

**Goal**: all six layout samples fully functional.

Tasks (one per sample class, in order):

1. ✓ **`BasicSplitSample`** — two leaves + vertical split at 0.7.
2. ✓ **`NestedSplitSample`** — horizontal outer split (0.25 / 0.75) + vertical inner split (0.7 / 0.3).
3. ✓ **`TabGroupSample`** — two tab groups with 3 and 2 leaves respectively.
4. ✓ **`FloatingSample`** — `dockManager.setOwnerStage(ownerStage)`, one detachable leaf.
5. ✓ **`MinimizeSample`** — toolbar button to minimize/restore a leaf programmatically.
6. ✓ **`PersistSample`** — Save/Restore buttons; JSON display overlay using a `TextArea` (ID: `persist-sample-json-area`).

Each sample:
- Binds `themeProperty` to its `DockManager` via `dm.themeProperty().bind(themeProperty)`.
- Returns `dockManager.getRootPane()` wrapped in a `BorderPane` with optional toolbar.

Validation: smoke test exercises all six samples headlessly; no uncaught exceptions. ✓

## Phase 3 — Code Editor Samples ✓ DONE

**Goal**: all four code editor samples functional with syntax highlighting and theme binding.

Tasks:
1. ✓ **`MarkdownEditorSample`** — `languageId = "markdown"`, inline README-style text.
2. ✓ **`JavaEditorSample`** — `languageId = "java"`, inline `HelloWorld.java` snippet.
3. ✓ **`JavaScriptEditorSample`** — `languageId = "javascript"`, inline ES module snippet.
4. ✓ **`JsonEditorSample`** — `languageId = "json"`, inline package manifest snippet.

Each sample:
- Creates `ContentStateRegistry` with `CodeEditorStateAdapter`.
- Sets `ContentFactory` to `new CodeEditorFactory()`.
- Creates `CodeEditor`, sets text and language, binds `themeProperty`.
- Wraps in `DockLeaf` → `DockTabGroup` → `DockManager.setRoot(...)`.
- Returns `dockManager.getRootPane()`.

Validation: all four editors load without exception in headless smoke test. ✓

## Phase 4 — Smoke Test and Hardening ✓ DONE

**Goal**: all acceptance criteria passing in headless CI.

Tasks:
1. ✓ Implement `SamplesSmokeTest` (`@ExtendWith(ApplicationExtension.class)`):
   - Iterates `SampleCatalog.all()`, calls `sample.build(stage, themeProperty)` on FX thread.
   - Waits with `WaitForAsyncUtils.waitForFxEvents()` after each sample.
   - Asserts `uncaughtException == null` via `Thread.setDefaultUncaughtExceptionHandler`.
   - Separate test for `PersistSample` build path asserts no exception.
2. ✓ Headless execution verified: `mvn -pl papiflyfx-docking-samples -am -Dtestfx.headless=true test`.
3. ✓ Smoke test runtime: ~2 s for samples module; ~40 s full reactor (well under 60 s gate).
4. Root `README.md` update — pending (optional follow-up).

Validation: `Tests run: 2, Failures: 0, Errors: 0` — BUILD SUCCESS. ✓

## Milestone Summary

| Milestone | Condition | Status |
|-----------|-----------|--------|
| M0 | Module compiles in reactor | ✓ Done |
| M1 | App starts with full catalog navigation | ✓ Done |
| M2 | All six docking layout samples functional | ✓ Done |
| M3 | All four code editor samples functional | ✓ Done |
| M4 | Headless smoke test passes, all acceptance criteria met | ✓ Done |
