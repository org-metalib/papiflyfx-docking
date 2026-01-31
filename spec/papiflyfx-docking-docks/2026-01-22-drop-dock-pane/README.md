# Drop DockPane (Model B)

## Goal
Make `DockTabGroup` the only renderable `DockElement`. A `DockLeaf` becomes a pure content model and is always rendered inside a (possibly single‑tab) tab group.

## Proposed Changes
- **Remove DockPane**: eliminate the standalone leaf chrome.
- **Render only via DockTabGroup**: every leaf is wrapped in a tab group (even single‑tab cases).
- **Model shift**: `DockLeaf` is no longer a `DockElement` and does not expose UI nodes.
- **Layout loading**: `LeafData` is still accepted, but the layout factory wraps it into a single‑tab `TabGroupData` on build.
- **Interactions**: drag, float, minimize, maximize operate on tab groups and/or active tabs only.

## Implementation Details
### Core Model + Rendering
- `DockLeaf` becomes a content holder (metadata + content) and no longer implements `DockElement`.
- `DockTabGroup` renders all content; tab close now delegates to `DockLeaf.requestClose()` to keep manager‑level close behavior consistent.
- `DockPane` is removed from the codebase.

### Layout Build/Serialize
- `LayoutFactory.build(LeafData)` returns a single‑tab `DockTabGroup`.
- Serialization still emits `TabGroupData` for renderable containers; `LeafData` remains a DTO for tabs inside a tab group.

### Drag + Hit Testing
- Hit testing targets `DockTabGroup`/`DockSplitGroup` only; leaf nodes are no longer hit targets.
- Drag start/drag cursor behavior is based on the leaf’s parent tab group.

### Float/Minimize/Maximize
- **Floating**: floating windows host a tab group with the leaf as its single tab.
- **Minimize**: restore hints preserve split placement for single‑tab groups where possible.
- **Maximize**: a temporary single‑tab `DockTabGroup` is created for maximized display; restoration re‑inserts the leaf into its prior group or split.

### Handler Wiring
- Layout restore now walks the tree to attach tab group drag handlers and leaf close handlers to restored elements.

## API Changes
- `DockLeaf` no longer implements `DockElement`; remove any calls to `getNode()` or `getPane()`.
- `Leaf` static convenience methods renamed to avoid collisions:
  - `Leaf.createWithTitle(String)`
  - `Leaf.createWithContent(String, Node)`

## Files Touched (Key)
- Core:
  - `papiflyfx-docks/src/main/java/org/metalib/papifly/fx/docks/core/DockLeaf.java`
  - `papiflyfx-docks/src/main/java/org/metalib/papifly/fx/docks/core/DockTabGroup.java`
  - `papiflyfx-docks/src/main/java/org/metalib/papifly/fx/docks/core/DockPane.java` (removed)
- Layout:
  - `papiflyfx-docks/src/main/java/org/metalib/papifly/fx/docks/layout/LayoutFactory.java`
- Manager + interactions:
  - `papiflyfx-docks/src/main/java/org/metalib/papifly/fx/docks/DockManager.java`
  - `papiflyfx-docks/src/main/java/org/metalib/papifly/fx/docks/drag/DragManager.java`
  - `papiflyfx-docks/src/main/java/org/metalib/papifly/fx/docks/drag/HitTester.java`
  - `papiflyfx-docks/src/main/java/org/metalib/papifly/fx/docks/drag/DragContext.java`
- Floating/minimize:
  - `papiflyfx-docks/src/main/java/org/metalib/papifly/fx/docks/floating/FloatingDockWindow.java`
  - `papiflyfx-docks/src/main/java/org/metalib/papifly/fx/docks/floating/FloatingWindowManager.java`
  - `papiflyfx-docks/src/main/java/org/metalib/papifly/fx/docks/minimize/MinimizedStore.java`
- Builders:
  - `papiflyfx-docks/src/main/java/org/metalib/papifly/fx/docks/Leaf.java`

## Test Updates
- Demo/layout expectations updated to reflect single‑tab groups.
- Drag/maximize tests now locate buttons on tab group chrome.
- Layout factory test now expects `DockTabGroup` for `LeafData`.

## Follow‑Ups / Notes
- Any external integration that relied on `DockLeaf.getNode()` / `DockLeaf.getPane()` must be updated.
- Layout JSON that contains top‑level `leaf` entries will restore into single‑tab groups.
