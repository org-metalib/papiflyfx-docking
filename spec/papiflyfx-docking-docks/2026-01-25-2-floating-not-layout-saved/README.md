# Floatings are not layout saved

## Problem

```text
Given initial DempApp layout
When I make "Editor2" tab floating
 and save the layout as "editor2-floating.json"
Then the layout does not contain "Editor2" dock leaf. 
```

## Analysis

- Floating removes the leaf from the dock tree: `DockManager.floatLeaf` calls `removeLeafFromDock`, so the floated tab no longer lives under `rootElement` when the layout is captured.
- Persistence only serializes the dock tree: `DockManager.saveTo*` calls `capture()` and `LayoutPersistence`, which uses `LayoutSerializer` that knows only `leaf/split/tabGroup` nodes.
- Floating state (and bounds/restore hints) lives outside the dock tree in `FloatingWindowManager` and `floatingRestoreHints`, so it is never written to JSON.

## Proposed solution

- Introduce a session-level DTO (e.g., `DockSessionData`) that wraps the docked tree plus floating/minimized/maximized state.
- When saving:
  - Serialize the docked tree with the current `LayoutSerializer`.
  - Add floating leaves as a list of `LeafData` entries plus bounds from `FloatingDockWindow.getBounds()` and restore hints from `floatingRestoreHints`.
  - Add minimized leaves from `MinimizedStore` with their `RestoreHint`.
- When restoring:
  - Build the docked tree first.
  - Recreate floating/minimized leaves via `LayoutFactory` (using `contentFactoryId`) and re-apply their stored restore hints.
  - Apply floating bounds (position/size) and avoid overwriting stored hints when re-floating.

## Session JSON schema (draft)

```json
{
  "type": "dockSession",
  "version": 1,
  "layout": {
    "type": "split",
    "id": "root",
    "orientation": "HORIZONTAL",
    "dividerPosition": 0.8,
    "first": { "type": "tabGroup", "id": "group-1", "tabs": [], "activeTabIndex": 0 },
    "second": { "type": "leaf", "id": "leaf-1", "title": "Properties", "contentFactoryId": "properties" }
  },
  "floating": [
    {
      "leaf": { "type": "leaf", "id": "leaf-2", "title": "Editor 2", "contentFactoryId": "editor:Editor 2 - utils.java" },
      "bounds": { "x": 120, "y": 80, "width": 520, "height": 360 },
      "restoreHint": { "parentId": "group-1", "zone": "TAB_BAR", "tabIndex": 1, "splitPosition": 0.5, "siblingId": null }
    }
  ],
  "minimized": [
    {
      "leaf": { "type": "leaf", "id": "leaf-3", "title": "Console", "contentFactoryId": "console" },
      "restoreHint": { "parentId": "split-2", "zone": "SOUTH", "tabIndex": -1, "splitPosition": 0.7, "siblingId": "leaf-1" }
    }
  ],
  "maximized": {
    "leaf": { "type": "leaf", "id": "leaf-4", "title": "Files", "contentFactoryId": "files" },
    "restoreHint": { "parentId": "split-1", "zone": "WEST", "tabIndex": -1, "splitPosition": 0.2, "siblingId": "group-1" }
  }
}
```

Notes:
- `layout` reuses the existing `LayoutSerializer` format.
- `bounds` is optional if the window has never been shown; when missing, use defaults.
- `restoreHint.zone` uses the `DropZone` enum names (`CENTER`, `WEST`, `EAST`, `NORTH`, `SOUTH`, `TAB_BAR`).
- If no leaf is maximized, `maximized` is null or absent.
## Minimal API changes (proposed)

- Add session DTOs (package `org.metalib.papifly.fx.docks.layout` or `...serial`):
  - `DockSessionData` with `LayoutNode layout`, `List<FloatingLeafData> floating`, `List<MinimizedLeafData> minimized`, `MaximizedLeafData maximized`.
  - `FloatingLeafData` with `LeafData leaf`, `Rectangle2D bounds`, `RestoreHint restoreHint`.
  - `MinimizedLeafData` with `LeafData leaf`, `RestoreHint restoreHint`.
  - `MaximizedLeafData` with `LeafData leaf`, `RestoreHint restoreHint`.
- Add a session serializer/persistence wrapper (e.g., `DockSessionSerializer` + `DockSessionPersistence`) that:
  - Delegates leaf and layout nodes to the existing `LayoutSerializer`.
- Add `DockManager` session APIs without breaking existing methods:
  - `DockSessionData captureSession()`
  - `void restoreSession(DockSessionData session)`
  - `String saveSessionToString()` / `void saveSessionToFile(Path path)`
  - `void restoreSessionFromString(String json)` / `void loadSessionFromFile(Path path)`
- Add a helper to restore floating without overwriting hints:
  - Example: `floatLeaf(DockLeaf leaf, RestoreHint hint, Rectangle2D bounds)` or `restoreFloating(DockLeaf leaf, RestoreHint hint, Rectangle2D bounds)`
