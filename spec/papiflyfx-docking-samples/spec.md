# PapiflyFX Docking Samples — Specification

`papiflyfx-docking-samples` is a runnable JavaFX application showcasing every major feature of the docking framework and its code-editor content type. It serves as a developer reference, integration validation harness, and smoke-test target for CI.

## 1. Vision

- One entry point that demonstrates both layout docking and code editor integration.
- Each sample is self-contained and fully runnable in isolation within the application shell.
- No production library code lives here — samples module is never published to Maven Central.
- Pure programmatic JavaFX, no FXML or CSS, consistent with the rest of the project.

## 2. Scope

### 2.1 In-Scope

- Application shell with sample catalog navigation and theme toggle.
- Six docking layout samples covering the full `DockManager` API surface.
- Four code editor samples, one per supported language ID.
- Headless TestFX smoke test that launches every sample without crashing.

### 2.2 Non-Goals

- Tutorial text, tooltips, or in-app documentation.
- Deployment packaging (fat JAR, installer, native image).
- Samples for post-MVP features (multi-caret, mini-map).

### 2.3 Module Boundary

- New Maven child module `papiflyfx-docking-samples` added to root aggregator.
- Compile dependencies: `papiflyfx-docking-docks`, `papiflyfx-docking-code`.
- Test dependencies: JUnit 5, TestFX, Monocle (same as sibling modules).
- No reverse dependency: library modules must not import from this module.

## 3. Architecture

### 3.1 Application Shell (`SamplesApp`)

`SamplesApp extends Application` hosts a single `Stage` divided into:

- **Left panel** — `ListView<SampleDescriptor>` grouped by category (Docks / Code).
- **Right panel** — content area that replaces its child when a sample is selected.
- **Top bar** — application title + dark/light theme toggle `ToggleButton`.

Selecting a sample calls `sample.build(stage) → Node` and sets the result as the right panel's content. The previous sample's `DockManager` (if any) is discarded. The theme `ObjectProperty<Theme>` lives on `SamplesApp` and is passed into each sample's builder so all `DockManager` instances share the same theme binding.

### 3.2 Sample Contract

```java
public interface SampleScene {
    String category();
    String title();
    Node build(Stage ownerStage, ObjectProperty<Theme> themeProperty);
}
```

### 3.3 Sample Catalog

`SampleCatalog` is a static factory returning `List<SampleScene>` in display order:

| # | Category | Title | Class |
|---|---|---|---|
| 1 | Docks | Basic Split | `BasicSplitSample` |
| 2 | Docks | Nested Splits | `NestedSplitSample` |
| 3 | Docks | Tab Groups | `TabGroupSample` |
| 4 | Docks | Floating Window | `FloatingSample` |
| 5 | Docks | Minimize to Bar | `MinimizeSample` |
| 6 | Docks | Session Persist | `PersistSample` |
| 7 | Code | Markdown Editor | `MarkdownEditorSample` |
| 8 | Code | Java Editor | `JavaEditorSample` |
| 9 | Code | JavaScript Editor | `JavaScriptEditorSample` |
| 10 | Code | JSON Editor | `JsonEditorSample` |

## 4. Sample Specifications

### 4.1 Docks Samples

**BasicSplitSample**
- Creates two `DockLeaf` panels (`"Panel A"`, `"Panel B"`), each containing a `Label`.
- Places them in a `DockTabGroup` each, then assembles with `DockManager.createVerticalSplit(..., 0.7)`.
- Demonstrates: `DockManager` setup, leaf creation, split layout, resize divider.

**NestedSplitSample**
- Three-pane IDE layout: left sidebar (25%) + right area split horizontally; right area split vertically into editor (70%) + console (30%).
- Demonstrates: nested `createHorizontalSplit` + `createVerticalSplit`, multi-pane coordination.

**TabGroupSample**
- Two `DockTabGroup`s, left group has three leaves, right group has two leaves.
- All leaves use `Label` content.
- Demonstrates: multi-tab groups, tab close button, float control, minimize control per tab.

**FloatingSample**
- Calls `dockManager.setOwnerStage(ownerStage)` to enable floating.
- One leaf can be detached to a floating window via the tab float button.
- Demonstrates: floating window lifecycle, re-docking.

**MinimizeSample**
- Two leaves; one is programmatically minimized on load via a toolbar button.
- Minimized bar appears at the bottom; clicking it restores the panel.
- Demonstrates: minimize/restore API and visual bar.

**PersistSample**
- IDE-style layout (editor + console + sidebar) with `Label` content.
- Toolbar buttons: **Save Session** (calls `dockManager.saveSessionToString()`, displays JSON in a `TextArea` overlay), **Restore Session** (calls `dockManager.restoreSessionFromString(json)`).
- Demonstrates: full round-trip session persistence without code editor.

### 4.2 Code Editor Samples

Each code sample follows the same setup pattern:

1. Create `DockManager`; register `CodeEditorStateAdapter` via `ContentStateRegistry`.
2. Set `ContentFactory` to `new CodeEditorFactory()`.
3. Create `CodeEditor`, set `languageId` and inline sample text.
4. Bind `editor.bindThemeProperty(themeProperty)`.
5. Wrap in `DockLeaf`, place into `DockTabGroup`, set as `DockManager` root.
6. Return `dockManager.getRootPane()`.

Sample texts are short inline strings (≤ 60 lines); no external file I/O required.

| Sample | `languageId` | Inline content description |
|---|---|---|
| `MarkdownEditorSample` | `markdown` | Short README-style document with headings, lists, code fence |
| `JavaEditorSample` | `java` | `HelloWorld.java` with class, main method, comments |
| `JavaScriptEditorSample` | `javascript` | ES module with import, arrow functions, template literals |
| `JsonEditorSample` | `json` | Package manifest with nested objects and arrays |

## 5. Theme Integration

- `SamplesApp` owns `ObjectProperty<Theme> themeProperty` initialized to `Theme.DARK`.
- The top-bar toggle switches between `Theme.DARK` and `Theme.LIGHT`.
- Every sample receives `themeProperty` and passes it to its `DockManager`.
- Code editor samples additionally call `editor.bindThemeProperty(themeProperty)`.

## 6. Run Configuration

`javafx-maven-plugin` in `papiflyfx-docking-samples/pom.xml`:
```xml
<mainClass>org.metalib.papifly.fx.samples.SamplesApp</mainClass>
```

`SampleLauncher` provides a plain `main(String[])` trampoline so the app is also launchable from IDE run configurations without module-path issues.

## 7. Acceptance Criteria

| # | Criterion | How verified |
|---|---|---|
| AC-1 | Application starts and displays sample catalog without errors | Manual run / smoke test |
| AC-2 | Each of the 10 samples loads without exception | `SamplesSmokeTest` iterates catalog |
| AC-3 | Theme toggle switches all open `DockManager` panels | Manual / headless TestFX assertion |
| AC-4 | `PersistSample` round-trips session JSON without layout change | `SamplesSmokeTest` assertion |
| AC-5 | All four code editors display syntax-highlighted sample text | `SamplesSmokeTest` canvas non-null check |
| AC-6 | Headless smoke test completes under 60 s | CI time gate |

## 8. Test Strategy

- `SamplesSmokeTest` (TestFX + JUnit 5): iterates `SampleCatalog.all()`, selects each sample, waits for render, asserts no uncaught exceptions.
- No separate unit tests for sample builder classes — they are thin wiring code.
- Headless execution enabled via `-Dtestfx.headless=true` (Monocle), same JVM args as sibling modules.

## 9. Implementation Phases

| Phase | Focus | Deliverable |
|---|---|---|
| 0 | Module bootstrap | `pom.xml`, empty `SamplesApp`, root aggregator updated |
| 1 | Shell + catalog | `SamplesApp` with `ListView`, `SampleScene` contract, `SampleCatalog` skeleton |
| 2 | Docks samples | All six docking layout samples functional |
| 3 | Code samples | All four code editor samples functional with theme binding |
| 4 | Smoke test | `SamplesSmokeTest` headless, all AC passing |
