# papiflyfx-docks

IDE-style docking framework for JavaFX. Build split/tabbed layouts with drag-and-drop, floating windows, minimize/maximize controls, and JSON session persistence.

## Features

- Drag-and-drop docking with drop-zone overlay hints
- Horizontal/vertical split groups with resizable dividers
- Tab groups with close, float, minimize, and maximize controls
- Floating windows (requires owner stage)
- Minimized bar for restoring hidden panels
- Session capture and JSON persistence
- Content state adapters with versioned leaf content payloads
- Programmatic theming (dark/light)

## Quick Start

### Prerequisites

- Java 25 (SDKMAN: `sdk use java 25.0.1.fx-zulu`)
- Maven 3.9+

### Run the demo

```bash
mvn javafx:run -pl papiflyfx-docks
```

### Basic usage

```java
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.metalib.papifly.fx.docks.DockManager;
import org.metalib.papifly.fx.docks.core.DockLeaf;
import org.metalib.papifly.fx.docks.core.DockTabGroup;

DockManager dockManager = new DockManager();

dockManager.setOwnerStage(stage); // enables floating windows

DockLeaf editor = dockManager.createLeaf("Editor", editorNode);
DockLeaf console = dockManager.createLeaf("Console", consoleNode);

DockTabGroup editorTabs = dockManager.createTabGroup();
editorTabs.addLeaf(editor);

DockTabGroup consoleTabs = dockManager.createTabGroup();
consoleTabs.addLeaf(console);

var root = dockManager.createVerticalSplit(editorTabs, consoleTabs, 0.7);
dockManager.setRoot(root);

Scene scene = new Scene(dockManager.getRootPane(), 1200, 800);
stage.setScene(scene);
stage.show();
```

## Session Persistence

### Save and restore

```java
// Save
String json = dockManager.saveSessionToString();
dockManager.saveSessionToFile(Paths.get("session.json"));

// Restore
DockManager dockManager = new DockManager();
dockManager.restoreSessionFromString(json);
dockManager.loadSessionFromFile(Paths.get("session.json"));
```

### Restoring content with a factory

If you want content nodes recreated on restore, set a `ContentFactory` and store a factory id per leaf.
Use this when the content has no custom state to persist beyond its identity.

```java
import org.metalib.papifly.fx.docks.Leaf;

DockManager dockManager = new DockManager();

dockManager.setContentFactory(factoryId -> switch (factoryId) {
    case "chart" -> createChart();
    case "orders" -> createOrders();
    default -> new Label("Unknown: " + factoryId);
});

DockLeaf chart = Leaf.create()
    .withTitle("Chart")
    .withContentFactoryId("chart")
    .content(createChart())
    .build();
```

### Persisting content state

For content that owns its own state (forms, charts, editors), register a `ContentStateAdapter`
and provide `LeafContentData` so the adapter can save/restore a state map. The dock manager
captures state automatically during session save. Use `ContentStateRegistry.fromServiceLoader()`
if you want adapters discovered via `ServiceLoader`.

```java
import org.metalib.papifly.fx.docks.layout.ContentStateAdapter;
import org.metalib.papifly.fx.docks.layout.ContentStateRegistry;
import org.metalib.papifly.fx.docks.layout.data.LeafContentData;

ContentStateRegistry registry = new ContentStateRegistry();
registry.register(new ContentStateAdapter() {
    @Override public String getTypeKey() { return "editor"; }
    @Override public int getVersion() { return 1; }
    @Override public Map<String, Object> saveState(String contentId, Node content) {
        TextArea editor = (TextArea) content;
        return Map.of("text", editor.getText(), "caret", editor.getCaretPosition());
    }
    @Override public Node restore(LeafContentData content) {
        TextArea editor = new TextArea();
        Map<String, Object> state = content.state();
        if (state != null && state.get("text") instanceof String text) {
            editor.setText(text);
        }
        return editor;
    }
});

dockManager.setContentStateRegistry(registry);

DockLeaf editor = dockManager.createLeaf("Editor", new TextArea());
editor.setContentFactoryId("editor:main.java");
editor.setContentData(LeafContentData.of("editor", "main.java", 1));
```

Persisted content JSON looks like:

```json
{
  "content": {
    "typeKey": "editor",
    "contentId": "main.java",
    "version": 1,
    "state": { "text": "...", "caret": 120 }
  }
}
```

If a leaf has content data but no adapter is registered for its `typeKey`, the restore path
uses a placeholder node while preserving the state for round-trips. When content data is absent,
the restore path falls back to the `ContentFactory`.

## Module Structure

```
papiflyfx-docks/
├── src/main/java/org/metalib/papifly/fx/docks/
│   ├── DockManager.java        # Main entry point
│   ├── core/                   # Dock elements (leaf/tab/split)
│   ├── drag/                   # Drag-and-drop system
│   ├── floating/               # Floating window support
│   ├── layout/                 # Layout DTOs + factory
│   ├── minimize/               # Minimized bar/state
│   ├── render/                 # Overlay rendering
│   ├── serial/                 # JSON session persistence
│   └── theme/                  # Theme definitions
└── src/test/java/...            # TestFX demo and UI tests
```

## Tests

```bash
mvn test -pl papiflyfx-docks

# Headless UI tests (Monocle)
mvn -Dtestfx.headless=true test -pl papiflyfx-docks
```