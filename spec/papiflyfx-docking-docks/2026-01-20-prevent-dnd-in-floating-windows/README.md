To forbid dragging `DockLeaf` panels from floating windows, the implementation should focus on the `DragManager` as the central gatekeeper and `DockPane`/`DockTabGroup` for visual feedback.

### Analysis of Current Implementation

1.  **Drag Initiation**: Dragging is initiated via mouse event filters set up in `DockManager.setupLeafDragHandlers` and `DockManager.setupTabGroupDragHandlers`. These filters call `dragManager.startDrag(leaf, event)`.
2.  **Floating State**: When a `DockLeaf` is floated, its associated `DockPane` has its `isFloating` flag set to `true` via `leaf.getPane().setFloating(true)`.
3.  **Coordinate Space**: Current hit testing in `DragManager` and `HitTester` is anchored to the main window's root pane. Dragging from a separate `Stage` (floating window) would require screen-to-local coordinate conversion which is not fully implemented for cross-window scenarios.

---

### Change Roadmap

#### 1. Centralized Restriction in `DragManager`
The most robust way to forbid dragging is to add a check at the entry point of the drag operation.
*   **Target**: `papiflyfx-docks/src/main/java/org/metalib/papifly/fx/docks/drag/DragManager.java`
*   **Action**: Modify `startDrag(DockLeaf leaf, MouseEvent event)` to return early if the leaf is floating.
    ```java
    public void startDrag(DockLeaf leaf, MouseEvent event) {
        if (currentDrag != null || leaf == null) return;
        
        // Forbid dragging from floating windows
        if (leaf.getPane().isFloating()) {
            return;
        }
        // ... rest of the method
    }
    ```

#### 2. Visual Feedback in `DockPane`
The cursor should indicate that the title bar is no longer a drag handle when floating.
*   **Target**: `papiflyfx-docks/src/main/java/org/metalib/papifly/fx/docks/core/DockPane.java`
*   **Action**: Update `setFloating(boolean floating)` to toggle the title bar cursor.
    ```java
    public void setFloating(boolean floating) {
        this.isFloating = floating;
        if (floating) {
            titleBar.setCursor(Cursor.DEFAULT); // Disable MOVE cursor
            // ... icon update
        } else {
            titleBar.setCursor(Cursor.MOVE); // Restore MOVE cursor
            // ... icon update
        }
    }
    ```

#### 3. Consistency in `DockTabGroup`
If multiple tabs are allowed in a floating window (future feature), the tab group should also disable dragging.
*   **Target**: `papiflyfx-docks/src/main/java/org/metalib/papifly/fx/docks/core/DockTabGroup.java`
*   **Action**: Update `setFloating(boolean floating)` and potentially `createTab` to adjust cursors.
    ```java
    public void setFloating(boolean floating) {
        this.isFloating = floating;
        // Logic to update cursors on all tabs and the tabBar itself
    }
    ```

#### 4. Guarding Event Handlers in `DockManager`
To be more efficient, we can prevent the drag start attempt earlier in the event chain.
*   **Target**: `papiflyfx-docks/src/main/java/org/metalib/papifly/fx/docks/DockManager.java`
*   **Action**: Update `setupLeafDragHandlers` and `setupTabGroupDragHandlers` to check the floating state before calling `startDrag`.

---

### Implementation Plan

1.  **Step 1: Modify `DragManager.startDrag`**
    *   Add the `leaf.getPane().isFloating()` check as the primary safety mechanism.
2.  **Step 2: Update `DockPane` Visuals**
    *   Modify `setFloating` to switch between `Cursor.MOVE` and `Cursor.DEFAULT`.
3.  **Step 3: Update `DockTabGroup` Visuals**
    *   Ensure that if a group is floating, its tabs don't show the `HAND` cursor or trigger drag starts.
4.  **Step 4: Verification**
    *   Run `DemoApp` and float a panel.
    *   Verify that the title bar of the floating window no longer shows the "move" cursor.
    *   Verify that clicking and dragging the title bar or tab of the floating window does not initiate a drag operation.
    *   Verify that docked panels in the main window still behave as expected.