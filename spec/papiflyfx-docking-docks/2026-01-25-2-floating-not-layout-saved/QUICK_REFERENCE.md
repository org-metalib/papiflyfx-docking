# Quick Reference: Session Save/Restore API

## Overview
Session save/restore extends the existing layout persistence to include floating windows, minimized leaves, and maximized state.

## Key Differences: Layout vs Session

| Feature | Layout APIs | Session APIs |
|---------|-------------|--------------|
| What's saved | Docked tree only | Complete state: docked + floating + minimized + maximized |
| Floating windows | ❌ Not saved | ✅ Saved with bounds and restore hints |
| Minimized leaves | ❌ Not saved | ✅ Saved with restore hints |
| Maximized state | ❌ Not saved | ✅ Captured (restoration TBD) |
| File format | Layout JSON | Session JSON (wraps layout) |
| Use when | Simple layouts | Complete application state |

## Session API Methods

### DockManager Methods

```java
// Capture current session
DockSessionData captureSession()

// Restore session
void restoreSession(DockSessionData session)

// Save to string
String saveSessionToString()

// Restore from string
void restoreSessionFromString(String json)

// Save to file
void saveSessionToFile(Path path)

// Load from file
void loadSessionFromFile(Path path)
```

## Usage Examples

### Basic Save/Restore

```java
DockManager dockManager = new DockManager();
dockManager.setOwnerStage(primaryStage);

// Setup content factory (required for restoration)
dockManager.setContentFactory(id -> createContent(id));

// Create layout and float a window
DockLeaf editor = dockManager.createLeaf("Editor", content);
editor.setContentFactoryId("editor:Main.java");
dockManager.floatLeaf(editor);

// Save session
Path sessionFile = Paths.get("my-session.json");
dockManager.saveSessionToFile(sessionFile);

// Later, restore
dockManager.loadSessionFromFile(sessionFile);
```

### Save to String

```java
// Save
String sessionJson = dockManager.saveSessionToString();
preferences.put("dock-session", sessionJson);

// Restore
String sessionJson = preferences.get("dock-session", "");
if (!sessionJson.isEmpty()) {
    dockManager.restoreSessionFromString(sessionJson);
}
```

### Programmatic Session Creation

```java
// Capture current state
DockSessionData session = dockManager.captureSession();

// Inspect session
System.out.println("Floating windows: " + session.floating().size());
System.out.println("Minimized leaves: " + session.minimized().size());

// Modify if needed
// ...

// Restore
dockManager.restoreSession(session);
```

## Session JSON Structure

```json
{
  "type": "dockSession",
  "version": 1,
  "layout": {
    /* Standard layout tree */
  },
  "floating": [
    {
      "leaf": { /* LeafData */ },
      "bounds": { "x": 100, "y": 100, "width": 400, "height": 300 },
      "restoreHint": { /* Where to dock when restored */ }
    }
  ],
  "minimized": [ /* ... */ ],
  "maximized": null
}
```

## Important Notes

### ContentFactory Requirement

Leaves must have a `contentFactoryId` and a `ContentFactory` must be set:

```java
// When creating leaves
leaf.setContentFactoryId("editor:MyFile.java");

// Before restoring
dockManager.setContentFactory(id -> {
    if (id.startsWith("editor:")) {
        String filename = id.substring("editor:".length());
        return createEditor(filename);
    }
    return new Label(id);
});
```

### Floating Window Bounds

Bounds are stored as absolute screen coordinates. Consider:
- Multi-monitor setups
- Different screen resolutions
- Window positioning validation

### Backward Compatibility

Session files are a superset of layout files:
- Old layout files can't be loaded as sessions (use layout APIs)
- Session files include complete state
- Both APIs coexist without interference

## File Locations

### New Classes
- DTOs: `org.metalib.papifly.fx.docks.layout`
  - `DockSessionData`, `FloatingLeafData`, `MinimizedLeafData`, etc.
- Serialization: `org.metalib.papifly.fx.docks.serial`
  - `DockSessionSerializer`, `DockSessionPersistence`

### Modified Classes
- `DockManager` - Added session methods
- `LayoutFactory` - Made `buildLeaf()` public

## Exception Handling

```java
try {
    dockManager.saveSessionToFile(path);
} catch (DockSessionPersistence.SessionSerializationException e) {
    // JSON serialization failed
} catch (DockSessionPersistence.SessionFileIOException e) {
    // File I/O failed (permissions, disk space, etc.)
}
```

## Demo Application

Run `SessionDemo` to see interactive demo:
```bash
# From project root
./mvnw test-compile exec:java -pl papiflyfx-docks \
  -Dexec.mainClass="org.metalib.papifly.fx.docks.DemoAppSession" \
  -Dexec.classpathScope=test
```

## Migration from Layout APIs

If currently using layout persistence:

```java
// Old way - only docked tree
dockManager.saveToFile(Paths.get("layout.json"));
dockManager.loadFromFile(Paths.get("layout.json"));

// New way - complete session
dockManager.saveSessionToFile(Paths.get("session.json"));
dockManager.loadSessionFromFile(Paths.get("session.json"));
```

No code changes needed if you only use docked layouts (no floating/minimizing).

## Testing

See `DockManagerSessionFxTest` for comprehensive examples:
- `testSessionSaveRestoreWithFloating` - Floating windows with bounds
- `testSessionSaveRestoreToString` - String serialization
- `testSessionWithMinimizedAndFloating` - Combined states

All tests use JavaFX application thread properly with `Platform.runLater()` and `WaitForAsyncUtils`.
