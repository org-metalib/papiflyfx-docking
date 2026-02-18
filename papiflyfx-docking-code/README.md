# papiflyfx-docking-code

A dockable JavaFX code editor content type for the PapiflyFX docking framework. Pure programmatic JavaFX — no FXML or CSS dependencies.

## Features

- Canvas-based virtualized text rendering for large files (100k+ lines)
- Single-caret editing with undo/redo, copy/paste, and selection
- Incremental syntax highlighting for Java, JSON, JavaScript, Markdown, and plain text
- Line number gutter with marker lane (errors, warnings, breakpoints, bookmarks)
- Find/replace overlay with regex support and go-to-line navigation
- Full docking integration via `ContentFactory` and `ContentStateAdapter`
- Runtime theme switching through composition with docking `Theme`

## Maven Dependency

```xml
<dependency>
    <groupId>org.metalib.papifly.docking</groupId>
    <artifactId>papiflyfx-docking-code</artifactId>
    <version>${papiflyfx.version}</version>
</dependency>
```

## Quick Start

### 1. Register the factory and adapter

```java
DockManager dockManager = new DockManager();

// Register content state adapter for session persistence
ContentStateRegistry registry = new ContentStateRegistry();
registry.register(new CodeEditorStateAdapter());
dockManager.setContentStateRegistry(registry);

// Register content factory for leaf creation
dockManager.setContentFactory(new CodeEditorFactory());
```

Alternatively, use ServiceLoader auto-discovery (the module ships a `META-INF/services` descriptor):

```java
ContentStateRegistry registry = ContentStateRegistry.fromServiceLoader();
dockManager.setContentStateRegistry(registry);
```

### 2. Create an editor leaf

```java
CodeEditor editor = new CodeEditor();
editor.setFilePath("/path/to/file.java");
editor.setText(Files.readString(Path.of("/path/to/file.java")));
editor.setLanguageId("java");

DockLeaf leaf = dockManager.createLeaf("file.java", editor);
leaf.setContentFactoryId(CodeEditorFactory.FACTORY_ID);
```

### 3. Bind to docking theme

```java
editor.bindThemeProperty(dockManager.themeProperty());
```

Theme changes propagate automatically to the viewport, gutter, and search overlay.

## Session Persistence Flow

1. **Save**: `DockManager.captureSession()` calls `CodeEditorStateAdapter.saveState()` on each editor leaf, producing a `LeafContentData` with cursor, scroll, language, and file path.
2. **Restore**: `DockManager.restoreSession()` calls `CodeEditorStateAdapter.restore()`, which creates a new `CodeEditor`, rehydrates document text from `filePath` (falling back to empty document if unreadable), and applies saved state.
3. **Fallback chain**: adapter restore → factory create → placeholder content. Session structure is always preserved.

## Editor API Highlights

| Method | Description |
|--------|-------------|
| `setText(String)` | Sets document text content |
| `getText()` | Returns document text |
| `setLanguageId(String)` | Sets syntax language (`java`, `json`, `javascript`, `markdown`, `plain-text`) |
| `bindThemeProperty(ObjectProperty<Theme>)` | Binds to docking theme for live updates |
| `setEditorTheme(CodeEditorTheme)` | Sets editor palette directly |
| `captureState()` | Captures current state as `EditorStateData` |
| `applyState(EditorStateData)` | Applies saved state (cursor, scroll, language) |
| `getMarkerModel()` | Access marker model for line annotations |
| `openSearch()` | Opens the search/replace overlay |
| `goToLine(int)` | Navigates to a 1-based line number |
| `dispose()` | Releases listeners, stops workers, cleans up resources |

## Supported Languages

| Language | ID | Highlights |
|----------|-----|------------|
| Java | `java` | Keywords, strings, comments, numbers, annotations |
| JSON | `json` | Keys, strings, numbers, booleans, null |
| JavaScript | `javascript` | Keywords, strings, template literals, comments, numbers |
| Markdown | `markdown` | Headlines, lists, code blocks |
| Plain Text | `plain-text` | No highlighting (default fallback) |

## Acceptance Metrics

Measured on macOS (Apple Silicon), headless mode, 100k-line synthetic Java file:

| Metric | Threshold | Measured | Status |
|--------|-----------|----------|--------|
| Large file open + first render | ≤ 2000ms | 218ms | PASS |
| Typing latency (p95, single char) | ≤ 16ms | 3.27ms | PASS |
| Scroll rendering (p95) | ≤ 16ms | 0.18ms | PASS |
| Memory overhead (100k lines) | ≤ 350MB | 63MB | PASS |

## Running Tests

```bash
# Regular tests (excludes benchmarks)
mvn -pl papiflyfx-docking-code,papiflyfx-docking-docks -am -Dtestfx.headless=true test

# Benchmarks only
mvn -pl papiflyfx-docking-code -am -Dtestfx.headless=true -Dgroups=benchmark -Dsurefire.excludedGroups= test
```

## Further Reading

- [Specification](../spec/papiflyfx-docking-code/spec.md)
- [Implementation Plan](../spec/papiflyfx-docking-code/implementation.md)
- [Progress Report](../spec/papiflyfx-docking-code/PROGRESS.md)
