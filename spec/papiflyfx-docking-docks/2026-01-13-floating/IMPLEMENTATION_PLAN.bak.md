# Floating / Minimize / Maximize — Implementation Plan

Scope: `papiflyfx-docks` module.

This plan proposes how to implement `FLOATING`, `MINIMIZED`, and `MAXIMIZED` behavior that currently exists only as enum values in `DockState`.

---

## 1) What exists today (baseline)

### Implemented

- `DockState` enum defines the states.
- `DockData` carries `DockState` and has `withState(...)`.
- Docking model supports:
  - `DockLeaf` (content)
  - `DockSplitGroup` (split)
  - `DockTabGroup` (tabs)
  - Drag-and-drop between docked elements (`DragManager`, `HitTester`, `OverlayCanvas`).
- Layout persistence exists for the dock tree shape and basic leaf identity:
  - `LeafData`: `id`, `title`, optional `contentFactoryId`
  - `SplitData`: `orientation`, `dividerPosition`
  - `TabGroupData`: `tabs`, `activeTabIndex`
  - `LayoutSerializer` reads/writes maps/JSON.

### Not implemented

- No `Stage`/window concept or “floating host”.
- No “minimized area” / restore UI.
- No maximize toggle.
- No serialization of leaf state, floating geometry, or “where to restore”.

---

## 2) Goals & non-goals

### Goals

1. Add functional support for:
   - **Float**: move a leaf from the dock tree into a dedicated floating `Stage`.
   - **Minimize**: remove leaf from dock tree into a minimized collection; restore later.
   - **Maximize**: toggle a leaf to take over the dock area; restore to previous layout.
2. Keep the “pure code” approach: no FXML/CSS additions.
3. Provide a clean public API on `DockManager`.
4. Persist enough state to restore layouts including floating/minimized/maximized.

### Non-goals (first iteration)

- Multi-monitor / DPI-perfect window placement.
- Advanced OS-style snapping.
- Cross-process persistence.
- Complex “tool window” sidebars (can be extended later).

---

## 3) Behavioral spec (state transitions)

### State meanings

- `DOCKED`: Leaf is part of the dock tree (tabs/splits).
- `FLOATING`: Leaf content is hosted in a separate `Stage`.
- `MINIMIZED`: Leaf is removed from main tree; available in a minimized strip/menu.
- `MAXIMIZED`: Leaf is shown exclusively (fills dock area) while the previous dock tree is preserved for restore.

### Allowed transitions

| From | To | Trigger |
|---|---|---|
| `DOCKED` | `FLOATING` | “float” button OR drag out of dock area |
| `FLOATING` | `DOCKED` | drag back into dock overlay OR “dock” button |
| `DOCKED` | `MINIMIZED` | “minimize” button |
| `MINIMIZED` | `DOCKED` | restore from minimized UI |
| `DOCKED` | `MAXIMIZED` | “maximize” button |
| `MAXIMIZED` | `DOCKED` | toggle maximize again / close maximize |
| `FLOATING` | `MINIMIZED` | optional (can be deferred) |
| `MAXIMIZED` | `FLOATING` | optional (can be deferred) |

### Invariants

- A `DockLeaf` must have exactly one host at a time:
  - dock tree host OR floating host OR minimized store OR maximized host.
- Closing a leaf disposes it regardless of current state.
- A leaf `id` remains stable across transitions for persistence.

---

## 4) Proposed internal architecture

### 4.1 Floating windows

Introduce a small internal subsystem to host floating leaves:

- `FloatingDockWindow` (internal):
  - Owns a JavaFX `Stage` and a root container.
  - Hosts one `DockLeaf` (first iteration). (Later extension: allow tab groups within a floating window.)
  - Wires window close request to either minimize, dock, or close leaf (configurable).

- `FloatingWindowManager` (internal, owned by `DockManager`):
  - Tracks open floating windows by `leafId`.
  - Creates/destroys windows.
  - Handles restoring a floating leaf into the dock tree.

### 4.2 Minimize

Add a “minimized area” UI container and model:

- `MinimizedBar` (internal node, attached to `DockManager` root):
  - A horizontal strip (or a menu button) that lists minimized leaves.
  - Clicking an item restores the leaf.

- `MinimizedStore` (internal model):
  - Stores minimized leaves and their restore hints.

Restore hint options (choose one for v1):

1. **Simple restore**: restore as a new tab in the root-most `DockTabGroup` (create if needed).
2. **Precise restore**: store a “last docked location” pointer (parent group id + index + zone).

### 4.3 Maximize

Implement maximize as a reversible layout override:

- `DockManager` keeps:
  - `LayoutNode lastDockedLayoutSnapshot` (or a direct `DockElement` tree reference) before maximizing.
  - `DockLeaf maximizedLeaf`.

Maximize behavior:

- On maximize: capture current root layout, then set root to contain only the selected leaf (or a tab group containing it).
- On restore: restore previous layout and reinsert leaf if needed.

Note: If the leaf was in a tab group, maximize should maximize the active leaf only (not the entire tab group), unless explicitly desired.

---

## 5) Public API proposal (DockManager)

Add methods (names are suggestions; align to existing code style):

```java
public void floatLeaf(DockLeaf leaf);
public void dockLeaf(DockLeaf leaf);
public void minimizeLeaf(DockLeaf leaf);
public void restoreLeaf(String leafId); // or restoreLeaf(DockLeaf)
public void maximizeLeaf(DockLeaf leaf);
public void restoreMaximized();
```

Also consider a single entry point:

```java
public void setLeafState(DockLeaf leaf, DockState state);
```

with internal guards for invalid transitions.

---

## 6) UI/UX plan (DockPane controls)

Extend `DockPane` title bar to include optional buttons:

- Float / Dock toggle
- Minimize
- Maximize / Restore
- Close (already exists)

UX details:

- Buttons should be small, consistent with current close button style (SVGPath icons).
- Hover/active coloring should reuse `Theme` (add colors if needed).
- Title bar remains the drag handle.

DnD to float (optional for v1):

- If a drag ends with `DropZone.NONE` (no hit), interpret as “float leaf at mouse position”.
- Requires `DragManager.endDrag(...)` to detect “no hit but dragged” and call `DockManager.floatLeaf(...)`.

---

## 7) Persistence plan

### 7.1 Data model changes

Add state/placement data to layout DTOs.

Options:

1. Extend `LeafData` with:
   - `DockState state`
   - `FloatingBounds bounds` (x,y,w,h) when `FLOATING`
   - `String dockRestoreHint` or structured hint

2. Introduce a top-level `LayoutSessionData` DTO that contains:
   - `LayoutNode dockedRoot`
   - `List<FloatingLeafData>`
   - `List<MinimizedLeafData>`
   - `String maximizedLeafId` (optional)

Recommendation: **Option 2**. It avoids forcing `LeafData` to represent non-docked states and keeps the dock tree “pure”.

### 7.2 Serializer changes

- Add a new serializer entry point to read/write the session DTO.
- Keep the existing `LayoutSerializer` for backward compatibility.

---

## 8) Implementation phases

### Phase A — Infrastructure & API

1. Add internal `FloatingWindowManager` and `FloatingDockWindow`.
2. Add new `DockManager` methods for float/dock/minimize/maximize.
3. Ensure `DockData.state` updates on transitions.

### Phase B — UI controls

1. Add title bar buttons to `DockPane`.
2. Wire buttons to `DockManager` actions.

### Phase C — Minimized UI

1. Implement `MinimizedBar` (simple restore strategy v1).
2. Add `DockManager` hooks to show/hide and populate it.

### Phase D — Maximize

1. Implement maximize toggle preserving previous root.
2. Ensure drag-and-drop still works while maximized (or explicitly disable).

### Phase E — Persistence

1. Introduce session DTO (docked + floating + minimized + maximized).
2. Implement serialization and restore flow.
3. Migration: allow loading old dock-only layouts.

---

## 9) Acceptance criteria

### Floating

- A docked leaf can be floated into a new window.
- Floating window shows the same `DockPane` UI (title + buttons).
- Closing the floating window either:
  - closes the leaf (v1 acceptable), or
  - docks it back (configurable; pick one and document).
- Leaf can be docked back into the main layout.

### Minimize

- Clicking minimize removes the leaf from dock layout without disposing it.
- Leaf appears in minimized UI.
- Clicking minimized entry restores leaf into dock layout.

### Maximize

- Clicking maximize expands leaf to fill dock area.
- Clicking restore returns the previous layout intact.

### Persistence

- Saving and loading restores:
  - docked tree
  - floating leaves (at least leaf ids + titles; bounds optional in v1)
  - minimized leaves
  - maximized state (if supported)

---

## 10) Risks & open questions

1. **Drag out to float**: needs a clear rule for “outside dock area”.
2. **Restore target for minimized**: simple restore is easy but may be surprising.
3. **Maximize interactions**: decide whether to allow DnD while maximized.
4. **Content ownership**: JavaFX `Node` must not be attached to two parents; ensure reparenting is always performed safely.
