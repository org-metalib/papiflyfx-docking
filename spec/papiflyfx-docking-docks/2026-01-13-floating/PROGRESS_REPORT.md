# Progress Report: Floating / Minimize / Maximize Implementation

**Date:** 2026-01-13
**Status:** Core Implementation Complete (Phases A-D)

---

## Summary

The floating, minimize, and maximize functionality has been implemented according to the `IMPLEMENTATION_PLAN.md`. All core phases (A-D) are complete. The DemoApp has been updated to demonstrate the new features.

---

## Implementation Status

| Phase | Description | Status |
|-------|-------------|--------|
| **A** | Infrastructure & API | ✅ Complete |
| **B** | UI Controls (title bar buttons) | ✅ Complete |
| **C** | Minimized UI (MinimizedBar) | ✅ Complete |
| **D** | Maximize Toggle | ✅ Complete |
| **E** | Persistence (session DTO) | ⏳ Not started |

---

## Files Created

### Floating System (`org.metalib.papifly.fx.docks.floating`)

| File | Description |
|------|-------------|
| `FloatingDockWindow.java` | Hosts a single `DockLeaf` in a decorated JavaFX `Stage`. Handles window lifecycle, position memory, and close-to-dock behavior. |
| `FloatingWindowManager.java` | Manages all floating windows by leaf ID. Creates/destroys windows and handles dock-back callbacks. |

### Minimize System (`org.metalib.papifly.fx.docks.minimize`)

| File | Description |
|------|-------------|
| `RestoreHint.java` | Record storing restore location (parent ID, drop zone, tab index, split position) for precise restore. |
| `MinimizedStore.java` | Stores minimized leaves with their restore hints. Captures location before minimizing. |
| `MinimizedBar.java` | Auto-hiding horizontal bar at the bottom of the dock area. Shows minimized panels as clickable items with icons and titles. |

---

## Files Modified

### `Theme.java`

Added new properties for button styling and minimized bar:

```java
Paint buttonHoverBackground,      // Button hover state
Paint buttonPressedBackground,    // Button pressed state
Paint minimizedBarBackground,     // Minimized bar background
double buttonSpacing,             // 8.0px between buttons
double minimizedBarHeight         // 24.0px bar height
```

### `DockPane.java`

Major changes:
- Added three new title bar buttons: Float/Dock, Minimize, Maximize/Restore
- Button order follows IDE conventions: `[Icon] [Title] ─── [Float/Dock] [Minimize] [Maximize] [Close]`
- Double-click on title bar toggles maximize
- Icons dynamically update based on state:
  - Float button shows dock-back icon when floating
  - Maximize button shows restore icon when maximized
- New public API:
  - `setOnFloat(Runnable)`
  - `setOnMinimize(Runnable)`
  - `setOnMaximize(Runnable)`
  - `setFloating(boolean)` / `isFloating()`
  - `setMaximized(boolean)` / `isMaximized()`
  - `setFloatButtonVisible(boolean)`
  - `setMinimizeButtonVisible(boolean)`
  - `setMaximizeButtonVisible(boolean)`

### `DockManager.java`

Major changes:
- Added `mainContainer` (BorderPane) wrapping dock area + minimized bar
- Added `FloatingWindowManager` and `MinimizedStore` instances
- Added maximize state tracking (`maximizedLeaf`, `savedRootBeforeMaximize`)

New public API:

```java
// Setup
void setOwnerStage(Stage stage)      // Required before floating

// Floating
void floatLeaf(DockLeaf leaf)
void floatLeaf(DockLeaf leaf, double x, double y)
void dockLeaf(DockLeaf leaf)

// Minimize
void minimizeLeaf(DockLeaf leaf)
void restoreLeaf(DockLeaf leaf)
void restoreLeaf(String leafId)

// Maximize
void maximizeLeaf(DockLeaf leaf)
void restoreMaximized()
boolean isMaximized()
DockLeaf getMaximizedLeaf()
```

Internal helpers:
- `removeLeafFromDock(DockLeaf)` - Remove without disposing
- `insertLeafIntoDock(DockLeaf)` - Insert at default position
- `tryRestoreWithHint(DockLeaf, RestoreHint)` - Precise restore
- `findElementById(DockElement, String)` - Tree traversal
- `updateLeafState(DockLeaf, DockState)` - State metadata update

### `DemoApp.java`

- Added `dockManager.setOwnerStage(primaryStage)` to enable floating
- Added "Restore Maximized" menu item under View menu

---

## Feature Behavior

### Floating

| Action | Behavior |
|--------|----------|
| Click float button | Leaf moves to new decorated window at stored position (or default) |
| Close floating window | Leaf docks back to main layout (not disposed) |
| Click dock button (in floating) | Leaf returns to dock tree |
| Position memory | Window position remembered per leaf ID |

### Minimize

| Action | Behavior |
|--------|----------|
| Click minimize button | Leaf removed from dock, added to bottom bar |
| Click item in minimized bar | Leaf restored to original position (precise restore) |
| Empty bar | Auto-hides with slide-out animation |
| Bar visible | Auto-shows with slide-in animation |

### Maximize

| Action | Behavior |
|--------|----------|
| Click maximize button | Leaf fills entire dock area, previous layout saved |
| Double-click title bar | Toggle maximize |
| Click restore button | Previous layout restored, leaf reinserted |
| Maximize icon | Changes to overlapping windows when maximized |

---

## State Transitions Implemented

| From | To | Trigger |
|------|-----|---------|
| `DOCKED` | `FLOATING` | Float button |
| `FLOATING` | `DOCKED` | Dock button or close window |
| `DOCKED` | `MINIMIZED` | Minimize button |
| `MINIMIZED` | `DOCKED` | Click in minimized bar |
| `DOCKED` | `MAXIMIZED` | Maximize button or double-click |
| `MAXIMIZED` | `DOCKED` | Restore button or double-click |
| `FLOATING` | `MINIMIZED` | ✅ Supported (unfloats then minimizes) |
| `FLOATING` | `MAXIMIZED` | ✅ Supported (unfloats then maximizes) |

---

## UX Decisions Made

Per the UX review in `IMPLEMENTATION_PLAN.md` Section 11:

| Decision | Implementation |
|----------|----------------|
| Close floating = dock back | ✅ Prevents accidental data loss |
| Precise restore for minimize | ✅ RestoreHint captures tab group + index |
| Minimized bar auto-hide | ✅ Slides in/out with 200ms animation |
| Double-click to maximize | ✅ Standard OS pattern |
| Button spacing 8px | ✅ Configured in Theme |

---

## Known Limitations

1. **Drag-to-float not implemented**: Dragging outside dock bounds does not auto-float (requires Phase B enhancement per UX review 11.3)
2. **Persistence not implemented**: Floating positions, minimized leaves, and maximize state not serialized (Phase E)
3. **Restore hint fallback**: If original parent deleted, falls back to default position (add as tab or split)
4. **No hidden tab indicator**: When maximizing from tab group, no badge showing "N tabs hidden"
5. **No keyboard shortcuts**: F11/Ctrl+M for maximize not implemented

---

## Testing

Build status: ✅ `mvn compile -pl papiflyfx-docks` succeeds

To test manually:
```bash
mvn javafx:run -pl papiflyfx-docks
```

Test scenarios:
1. Click float button on any panel → opens in new window
2. Close floating window → docks back
3. Click minimize → panel appears in bottom bar
4. Click bar item → panel restores to original position
5. Click maximize → panel fills dock area
6. Double-click title bar → toggles maximize
7. Toggle theme → all UI updates correctly

---

## Next Steps (Phase E)

1. Introduce `LayoutSessionData` DTO containing:
   - `LayoutNode dockedRoot`
   - `List<FloatingLeafData>` with bounds
   - `List<MinimizedLeafData>` with restore hints
   - `String maximizedLeafId`

2. Update `LayoutSerializer` with new entry point

3. Implement save/restore flow in `DockManager`

4. Add backward compatibility for old dock-only layouts
