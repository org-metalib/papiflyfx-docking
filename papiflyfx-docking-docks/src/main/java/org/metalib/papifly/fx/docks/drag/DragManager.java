package org.metalib.papifly.fx.docks.drag;

import javafx.beans.property.ObjectProperty;
import javafx.geometry.Orientation;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import org.metalib.papifly.fx.docks.core.DockElement;
import org.metalib.papifly.fx.docks.core.DockLeaf;
import org.metalib.papifly.fx.docks.core.DockSplitGroup;
import org.metalib.papifly.fx.docks.core.DockTabGroup;
import org.metalib.papifly.fx.docks.render.OverlayCanvas;
import org.metalib.papifly.fx.docks.theme.Theme;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Manages drag-and-drop operations for dock elements.
 * Handles mouse events, hit testing, and drop execution.
 */
public class DragManager {

    private static final double DRAG_THRESHOLD = 5.0;
    private static final long DEBOUNCE_MS = 50; // Hysteresis to prevent zone flicker

    private final Supplier<DockElement> rootSupplier;
    private final OverlayCanvas overlay;
    private final Consumer<DockElement> rootUpdater;
    private final ObjectProperty<Theme> themeProperty;
    private final Supplier<DockTabGroup> tabGroupFactory;

    private DragContext currentDrag;
    private HitTester hitTester;
    private boolean isDragging;
    private Cursor previousCursor;

    // Hover stabilization state
    private HitTestResult lastHitResult;
    private long lastHitChangeTime;

    public DragManager(Supplier<DockElement> rootSupplier, OverlayCanvas overlay,
                       Consumer<DockElement> rootUpdater, ObjectProperty<Theme> themeProperty,
                       Supplier<DockTabGroup> tabGroupFactory) {
        this.rootSupplier = rootSupplier;
        this.overlay = overlay;
        this.rootUpdater = rootUpdater;
        this.themeProperty = themeProperty;
        this.tabGroupFactory = tabGroupFactory;
    }

    /**
     * Initiates a drag operation from a leaf's title bar or tab.
     */
    public void startDrag(DockLeaf leaf, MouseEvent event) {
        if (currentDrag != null) {
            return; // Already dragging
        }

        DockTabGroup parent = leaf.getParent();
        if (parent == null) {
            return;
        }

        // Forbid dragging from floating windows
        if (parent.isFloating()) {
            return;
        }

        currentDrag = new DragContext(leaf, event.getSceneX(), event.getSceneY());
        isDragging = false;
        hitTester = new HitTester(rootSupplier.get());

        // Save current cursor
        Scene scene = parent.getNode().getScene();
        if (scene != null) {
            previousCursor = scene.getCursor();
        }
    }

    /**
     * Handles mouse drag during a drag operation.
     */
    public void onDrag(MouseEvent event) {
        if (currentDrag == null) {
            return;
        }

        double dx = event.getSceneX() - currentDrag.getStartX();
        double dy = event.getSceneY() - currentDrag.getStartY();

        // Check if we've moved past the drag threshold
        if (!isDragging && (Math.abs(dx) > DRAG_THRESHOLD || Math.abs(dy) > DRAG_THRESHOLD)) {
            isDragging = true;
            DockTabGroup parent = currentDrag.getSourceLeaf().getParent();
            Scene scene = parent != null ? parent.getNode().getScene() : null;
            if (scene != null) {
                scene.setCursor(Cursor.MOVE);
            }
        }

        if (!isDragging) {
            return;
        }

        // Perform hit testing
        HitTestResult result = hitTester.hitTest(
            event.getSceneX(),
            event.getSceneY(),
            currentDrag.getSourceLeaf()
        );

        // Stabilize hover: only update if result actually changed
        if (hasHitResultChanged(result)) {
            long now = System.currentTimeMillis();

            // Apply debounce: only accept change if enough time has passed
            // or if this is the first result
            if (lastHitResult == null || (now - lastHitChangeTime) >= DEBOUNCE_MS) {
                lastHitResult = result;
                lastHitChangeTime = now;

                currentDrag.setTargetElement(result.element());
                currentDrag.setDropZone(result.zone());
                currentDrag.setTabInsertIndex(result.tabInsertIndex());

                // Update overlay only when result actually changes
                if (result.isHit()) {
                    overlay.showDropHint(result);
                } else {
                    overlay.clearDropHint();
                }
            }
        }
    }

    /**
     * Checks if the hit result has meaningfully changed.
     */
    private boolean hasHitResultChanged(HitTestResult newResult) {
        if (lastHitResult == null) {
            return true;
        }
        // Compare element, zone, and tab insert index (for tab reordering)
        return lastHitResult.element() != newResult.element()
            || lastHitResult.zone() != newResult.zone()
            || lastHitResult.tabInsertIndex() != newResult.tabInsertIndex();
    }

    /**
     * Handles mouse release to complete or cancel the drag.
     */
    public void endDrag(MouseEvent event) {
        if (currentDrag == null) {
            return;
        }

        overlay.clearDropHint();

        // Restore cursor
        DockTabGroup parent = currentDrag.getSourceLeaf().getParent();
        Scene scene = parent != null ? parent.getNode().getScene() : null;
        if (scene != null) {
            scene.setCursor(previousCursor);
        }

        if (isDragging && currentDrag.isValidDrop() && !currentDrag.isDropOnSelf() && !currentDrag.isNoOpTabReorder()) {
            executeDrop();
        }

        currentDrag = null;
        isDragging = false;
        hitTester = null;
        lastHitResult = null;
        lastHitChangeTime = 0;
    }

    /**
     * Cancels the current drag operation.
     */
    public void cancelDrag() {
        if (currentDrag != null) {
            overlay.clearDropHint();
            DockTabGroup parent = currentDrag.getSourceLeaf().getParent();
            Scene scene = parent != null ? parent.getNode().getScene() : null;
            if (scene != null) {
                scene.setCursor(previousCursor);
            }
            currentDrag = null;
            isDragging = false;
            lastHitResult = null;
            lastHitChangeTime = 0;
        }
    }

    /**
     * Checks if a drag operation is in progress (threshold crossed).
     */
    public boolean isDragging() {
        return isDragging;
    }

    /**
     * Checks if a drag context exists (mouse pressed on draggable element).
     */
    public boolean hasDragContext() {
        return currentDrag != null;
    }

    private static final double MIN_SPLIT_SIZE = 50.0; // Minimum size for split panes

    private void executeDrop() {
        DockLeaf source = currentDrag.getSourceLeaf();
        DockElement target = currentDrag.getTargetElement();
        DropZone zone = currentDrag.getDropZone();

        // Handle same-group tab reorder specially
        if (currentDrag.isSameGroupReorder()) {
            reorderTabInGroup(source, (DockTabGroup) target);
            return;
        }

        // First, remove source from its parent
        removeFromParent(source);

        // Then, execute the drop based on zone
        switch (zone) {
            case CENTER, TAB_BAR -> dropAsTab(source, target);
            case NORTH -> dropAsSplit(source, target, Orientation.VERTICAL, true);
            case SOUTH -> dropAsSplit(source, target, Orientation.VERTICAL, false);
            case WEST -> dropAsSplit(source, target, Orientation.HORIZONTAL, true);
            case EAST -> dropAsSplit(source, target, Orientation.HORIZONTAL, false);
            default -> { } // NONE - shouldn't happen
        }

        // Clean up empty containers
        cleanupHierarchy();
    }

    /**
     * Reorders a tab within the same tab group.
     */
    private void reorderTabInGroup(DockLeaf source, DockTabGroup tabGroup) {
        int currentIndex = tabGroup.getTabs().indexOf(source);
        int targetIndex = currentDrag.getTabInsertIndex();

        if (currentIndex < 0 || currentIndex == targetIndex) {
            return; // No-op
        }

        // Adjust target index after removal
        if (currentIndex < targetIndex) {
            targetIndex--;
        }

        tabGroup.moveLeaf(currentIndex, targetIndex);
    }

    private void removeFromParent(DockLeaf leaf) {
        DockTabGroup parent = leaf.getParent();
        if (parent != null) {
            parent.removeLeaf(leaf);
        }
    }

    private void dropAsTab(DockLeaf source, DockElement target) {
        if (target instanceof DockTabGroup tabGroup) {
            // Insert at specific index if dropping on tab bar
            int insertIndex = currentDrag.getTabInsertIndex();
            if (insertIndex >= 0 && currentDrag.getDropZone() == DropZone.TAB_BAR) {
                int clampedIndex = Math.min(Math.max(0, insertIndex), tabGroup.getTabs().size());
                tabGroup.addLeaf(clampedIndex, source);
            } else {
                tabGroup.addLeaf(source);
            }
        }
    }

    private void dropAsSplit(DockLeaf source, DockElement target, Orientation orientation, boolean sourceFirst) {
        DockElement parent = target.getParent();
        DockSplitGroup newSplit = createSplitGroup(orientation);

        // IMPORTANT: First replace target with newSplit in the parent hierarchy
        // This must be done BEFORE adding children to newSplit, otherwise
        // setParent calls get corrupted when replaceChild sets old child's parent to null
        if (parent instanceof DockSplitGroup parentSplit) {
            // Detach target from parent first
            if (parentSplit.getFirst() == target) {
                parentSplit.setFirst(null);
            } else {
                parentSplit.setSecond(null);
            }
            // Insert newSplit in place of target
            if (parentSplit.getFirst() == null && parentSplit.getSecond() != null) {
                parentSplit.setFirst(newSplit);
            } else if (parentSplit.getSecond() == null) {
                parentSplit.setSecond(newSplit);
            }
        } else if (parent == null) {
            // Target was root - will set newSplit as root after adding children
        }

        // Now add children to newSplit (this correctly sets parent references)
        if (sourceFirst) {
            DockTabGroup sourceGroup = tabGroupFactory.get();
            sourceGroup.addLeaf(source);
            newSplit.setFirst(sourceGroup);
            newSplit.setSecond(target);
        } else {
            DockTabGroup sourceGroup = tabGroupFactory.get();
            sourceGroup.addLeaf(source);
            newSplit.setFirst(target);
            newSplit.setSecond(sourceGroup);
        }

        // If target was root, set newSplit as new root
        if (parent == null) {
            rootUpdater.accept(newSplit);
        }
    }

    private DockSplitGroup createSplitGroup(Orientation orientation) {
        return new DockSplitGroup(orientation, themeProperty);
    }

    private void cleanupHierarchy() {
        // Remove empty tab groups and unnecessary splits
        // This is called after a drop to maintain a clean tree structure
        DockElement root = rootSupplier.get();
        if (root != null) {
            cleanupRecursive(root, null);
        }
    }

    private void cleanupRecursive(DockElement element, DockElement parent) {
        if (element instanceof DockSplitGroup split) {
            // Clean children first
            if (split.getFirst() != null) {
                cleanupRecursive(split.getFirst(), split);
            }
            if (split.getSecond() != null) {
                cleanupRecursive(split.getSecond(), split);
            }

            // Check if split has only one child
            boolean firstEmpty = split.getFirst() == null;
            boolean secondEmpty = split.getSecond() == null;

            if (firstEmpty && secondEmpty) {
                // Both empty - remove split
                removeElement(split, parent);
            } else if (firstEmpty || secondEmpty) {
                // One empty - replace with remaining child
                DockElement remaining = firstEmpty ? split.getSecond() : split.getFirst();
                replaceElement(split, remaining, parent);
            }
        } else if (element instanceof DockTabGroup tabGroup) {
            if (tabGroup.getTabs().isEmpty()) {
                removeElement(tabGroup, parent);
            }
        }
    }

    private void removeElement(DockElement element, DockElement parent) {
        if (parent instanceof DockSplitGroup split) {
            if (split.getFirst() == element) {
                split.setFirst(null);
            } else if (split.getSecond() == element) {
                split.setSecond(null);
            }
        } else if (parent == null) {
            rootUpdater.accept(null);
        }

        if (element instanceof DockSplitGroup splitElement) {
            detachChild(splitElement, splitElement.getFirst());
            detachChild(splitElement, splitElement.getSecond());
            splitElement.dispose();
        } else if (element instanceof DockTabGroup tabGroup) {
            tabGroup.dispose();
        }
    }

    private void replaceElement(DockElement oldElement, DockElement newElement, DockElement parent) {
        if (oldElement instanceof DockSplitGroup splitElement) {
            detachChild(splitElement, newElement);
            splitElement.dispose();
        }
        if (parent instanceof DockSplitGroup split) {
            split.replaceChild(oldElement, newElement);
        } else if (parent == null) {
            rootUpdater.accept(newElement);
        }
    }

    private void detachChild(DockSplitGroup split, DockElement child) {
        if (child == null) {
            return;
        }
        if (split.getFirst() == child) {
            split.setFirst(null);
        } else if (split.getSecond() == child) {
            split.setSecond(null);
        }
    }
}
