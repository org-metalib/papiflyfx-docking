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
- Ribbon shell host (`RibbonDockHost`) with ServiceLoader-driven tabs/groups
- Ribbon state persistence (minimized state, selected tab, QAT command ids)
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

### Ribbon host persistence

If you mount dock content through `RibbonDockHost`, ribbon shell state is
captured/restored through the same dock session payload automatically.

```java
DockManager dockManager = new DockManager();
RibbonManager ribbonManager = new RibbonManager(); // ServiceLoader providers
RibbonDockHost host = new RibbonDockHost(dockManager, ribbonManager, new Ribbon());

String json = dockManager.saveSessionToString();
dockManager.restoreSessionFromString(json);
```

Persisted ribbon state now lives under the namespaced dock-session extension
payload `extensions.ribbon`:

- `extensions.ribbon.minimized`
- `extensions.ribbon.selectedTabId`
- `extensions.ribbon.quickAccessCommandIds`

Ribbon contributors and hosts should keep tab and command identifiers stable
once published. Selected-tab restore and Quick Access Toolbar persistence are
ID-first and depend on those stable identifiers.

### Extension contributors

Dock-session extensions are contributor-owned and persist under
`extensions.<namespace>`.

- Choose one stable namespace per contributor and keep it unchanged once sessions may be persisted.
- Encode and decode only your own payload through `DockSessionExtensionCodec`; do not add bespoke top-level session fields.
- Malformed extension payloads are isolated to the owning contributor during restore and do not block unrelated core dock state.

Typical contributor shape:

```java
class ExampleSessionContributor implements DockSessionStateContributor<ExampleState> {
    @Override
    public String extensionNamespace() {
        return "example";
    }

    @Override
    public DockSessionExtensionCodec<ExampleState> codec() {
        return new ExampleSessionCodec();
    }
}
```

### Ribbon provider integration

Ribbon providers should use typed capabilities for executable integrations and
attributes for metadata/visibility decisions.

- `RibbonContext#capability(Class)` resolves action interfaces such as `GitHubRibbonActions` and `HugoRibbonActions`.
- `RibbonContextAttributes` are for dock title, content-factory id, floating/maximized state, and similar metadata.
- `RibbonContextAttributes.ACTIVE_CONTENT_NODE` is a temporary compatibility bridge in the docks runtime and should not be used by new providers.

### Command registry invariants

`RibbonManager` owns the runtime `CommandRegistry`.

- Command identity is canonical and registry-owned. Reuse the same command id for the same semantic action.
- Mutate registry-backed command state on the FX thread or the single thread that drives `RibbonManager.refresh()`.
- Steady-state refreshes rely on cache reuse and telemetry-backed invariants rather than rebuilding every tab/group/control surface.

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
