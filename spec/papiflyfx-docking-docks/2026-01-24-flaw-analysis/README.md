# PapiflyFX Docks Flaw Analysis (2026-01-24)

## Scope
- Module: papiflyfx-docks
- Reviewed: docking tree (core), drag/drop, floating/minimize/maximize, layout serialization, theme wiring

## Findings

### High
1) Layout restore cannot rebuild content factory, and layout save drops contentFactoryId
- Evidence: `DockManager.setContentFactory` stores a field but `LayoutFactory` is constructed without it and never updated, so restore builds leaves with null content. `DockLeaf.serialize` always returns `LeafData.of(id, title)` and discards `contentFactoryId`, so even if layouts carry factory IDs they cannot round-trip.
- Impact: layouts restored from JSON lose content; content factories never run.
- Files: `papiflyfx-docks/src/main/java/org/metalib/papifly/fx/docks/DockManager.java`, `papiflyfx-docks/src/main/java/org/metalib/papifly/fx/docks/layout/LayoutFactory.java`, `papiflyfx-docks/src/main/java/org/metalib/papifly/fx/docks/core/DockLeaf.java`, `papiflyfx-docks/src/main/java/org/metalib/papifly/fx/docks/layout/LeafData.java`

2) Tab bar hit testing uses the wrong node list, producing bad insert indices and possible IndexOutOfBounds
- Evidence: `HitTester.calculateTabInsertInfo` iterates `tabBar.getChildrenUnmodifiable()` which contains the tabs container, spacer, and buttons (not the individual tab nodes). This limits `tabInsertIndex` to 0..2 regardless of tab count, and when a group is temporarily empty it can return an index larger than `tabs.size()`. `DragManager.dropAsTab` then calls `DockTabGroup.addLeaf(index, leaf)` with no clamping.
- Impact: tab reorder/drop targets are incorrect; dropping on the tab bar can throw and abort drag operations.
- Files: `papiflyfx-docks/src/main/java/org/metalib/papifly/fx/docks/drag/HitTester.java`, `papiflyfx-docks/src/main/java/org/metalib/papifly/fx/docks/drag/DragManager.java`, `papiflyfx-docks/src/main/java/org/metalib/papifly/fx/docks/core/DockTabGroup.java`

### Medium
3) Drag release events are never consumed after a real drag
- Evidence: `DockManager.setupTabGroupDragHandlers` calls `dragManager.endDrag(event)` and then checks `dragManager.isDragging()`. `endDrag` resets `isDragging` to false, so the release event is never consumed.
- Impact: a drag can still trigger click actions (tab selection, close button) on mouse release, leading to accidental UI actions.
- File: `papiflyfx-docks/src/main/java/org/metalib/papifly/fx/docks/DockManager.java`

4) Detached containers and floating windows leak via theme listeners
- Evidence: `DockSplitGroup` registers a theme listener but never removes it (not even in `dispose`). `FloatingDockWindow` also registers a theme listener and never removes it on close. `DockManager.removeElementWithoutDispose`, `FloatingWindowManager.unfloatLeaf`, and `DragManager.cleanupRecursive` remove tab groups/splits without disposing them, leaving listeners attached to the `themeProperty`.
- Impact: repeated docking/floating/minimizing grows retained objects and listeners, increasing memory usage over time.
- Files: `papiflyfx-docks/src/main/java/org/metalib/papifly/fx/docks/core/DockSplitGroup.java`, `papiflyfx-docks/src/main/java/org/metalib/papifly/fx/docks/floating/FloatingDockWindow.java`, `papiflyfx-docks/src/main/java/org/metalib/papifly/fx/docks/DockManager.java`, `papiflyfx-docks/src/main/java/org/metalib/papifly/fx/docks/floating/FloatingWindowManager.java`, `papiflyfx-docks/src/main/java/org/metalib/papifly/fx/docks/drag/DragManager.java`

5) Floating a maximized leaf or re-floating an already floating leaf can corrupt state
- Evidence: `DockManager.floatLeaf(DockLeaf)` does not restore from maximized state, so floating a maximized leaf can clear the root and lose the saved layout. The overload `floatLeaf(leaf, x, y)` lacks the already-floating guard; it removes the leaf from its floating tab group and then just raises the existing window, leaving the leaf detached.
- Impact: layout can be lost or a floating window can end up empty with the leaf orphaned.
- File: `papiflyfx-docks/src/main/java/org/metalib/papifly/fx/docks/DockManager.java`

6) `DockTabGroup.getTabs()` returns the live mutable list despite the "unmodifiable" contract
- Evidence: method returns the internal `ObservableList` directly, but the Javadoc says "unmodifiable view." External mutation bypasses `addLeaf/removeLeaf` and can leave parent pointers and close handling inconsistent.
- Impact: callers can accidentally corrupt the docking tree or leave leaves without parents.
- File: `papiflyfx-docks/src/main/java/org/metalib/papifly/fx/docks/core/DockTabGroup.java`

### Low
7) Deprecated `OverlayCanvas.showDropHint(Bounds)` can throw NPE
- Evidence: it creates a `HitTestResult` with a null `DropZone`, and `redraw` assumes `zone` is non-null.
- Impact: any remaining caller of the deprecated method can crash drag rendering.
- File: `papiflyfx-docks/src/main/java/org/metalib/papifly/fx/docks/render/OverlayCanvas.java`

8) Floating restore hints are never cleared on close
- Evidence: `DockManager.floatLeaf` stores a hint in `floatingRestoreHints`, but `closeLeaf` does not remove the entry if the floating leaf is closed.
- Impact: minor memory leak and stale hints for closed leaves.
- File: `papiflyfx-docks/src/main/java/org/metalib/papifly/fx/docks/DockManager.java`

## Test Gaps / Suggested Coverage
- Layout save/restore with `contentFactoryId` and content reconstruction (including round-trip persistence).
- Tab reorder and insert behavior with 3+ tabs; verify no exceptions on empty-group drop targets.
- Drag-and-drop release behavior (ensure no stray click actions after dragging).
- Floating/minimize scenarios that create and remove splits/groups repeatedly (listener leak detection).
- Floating while maximized and re-floating an already floating leaf.

## Notes
- This review is based on static source inspection of the `papiflyfx-docks` module; no runtime profiling or leak detection was executed.
