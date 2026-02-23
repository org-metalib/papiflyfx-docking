package org.metalib.papifly.fx.docks.drag;

import org.metalib.papifly.fx.docks.core.DockElement;
import org.metalib.papifly.fx.docks.core.DockLeaf;
import org.metalib.papifly.fx.docks.core.DockTabGroup;

/**
 * Context object holding the state of a drag operation.
 */
public class DragContext {

    private final DockLeaf sourceLeaf;
    private final DockTabGroup sourceParent;
    private final int sourceTabIndex; // Original tab index if source was in a tab group
    private DockElement targetElement;
    private DropZone dropZone;
    private int tabInsertIndex = -1;
    private double startX;
    private double startY;

    /**
     * Creates drag context for a drag operation.
     *
     * @param sourceLeaf dragged source leaf
     * @param startX drag start x coordinate in scene space
     * @param startY drag start y coordinate in scene space
     */
    public DragContext(DockLeaf sourceLeaf, double startX, double startY) {
        this.sourceLeaf = sourceLeaf;
        this.sourceParent = sourceLeaf.getParent();
        this.startX = startX;
        this.startY = startY;
        this.dropZone = DropZone.NONE;

        // Track original tab index
        if (sourceParent != null) {
            this.sourceTabIndex = sourceParent.getTabs().indexOf(sourceLeaf);
        } else {
            this.sourceTabIndex = -1;
        }
    }

    /**
     * Gets the dragged source leaf.
     *
     * @return source leaf
     */
    public DockLeaf getSourceLeaf() {
        return sourceLeaf;
    }

    /**
     * Gets the original parent tab group of the source leaf.
     *
     * @return source parent tab group, or {@code null}
     */
    public DockTabGroup getSourceParent() {
        return sourceParent;
    }

    /**
     * Gets the current drop target element.
     *
     * @return current target element
     */
    public DockElement getTargetElement() {
        return targetElement;
    }

    /**
     * Sets the current drop target element.
     *
     * @param target target element
     */
    public void setTargetElement(DockElement target) {
        this.targetElement = target;
    }

    /**
     * Gets the current drop zone.
     *
     * @return current drop zone
     */
    public DropZone getDropZone() {
        return dropZone;
    }

    /**
     * Sets the current drop zone.
     *
     * @param zone drop zone
     */
    public void setDropZone(DropZone zone) {
        this.dropZone = zone;
    }

    /**
     * Gets tab insertion index for tab-bar drops.
     *
     * @return insertion index, or {@code -1}
     */
    public int getTabInsertIndex() {
        return tabInsertIndex;
    }

    /**
     * Sets tab insertion index for tab-bar drops.
     *
     * @param index insertion index
     */
    public void setTabInsertIndex(int index) {
        this.tabInsertIndex = index;
    }

    /**
     * Gets source tab index in the original parent group.
     *
     * @return source tab index, or {@code -1}
     */
    public int getSourceTabIndex() {
        return sourceTabIndex;
    }

    /**
     * Gets drag start x coordinate.
     *
     * @return drag start x coordinate
     */
    public double getStartX() {
        return startX;
    }

    /**
     * Gets drag start y coordinate.
     *
     * @return drag start y coordinate
     */
    public double getStartY() {
        return startY;
    }

    /**
     * Checks if this is a valid drop (target exists and zone is not NONE).
     *
     * @return {@code true} when drop target and drop zone are valid
     */
    public boolean isValidDrop() {
        return targetElement != null && dropZone != DropZone.NONE;
    }

    /**
     * Checks if attempting to drop on self (same leaf or same position in same tab group).
     *
     * @return {@code true} when drop would be a self-drop
     */
    public boolean isDropOnSelf() {
        if (targetElement == null || sourceParent == null) {
            return false;
        }

        if (targetElement == sourceParent
            && (dropZone == DropZone.NORTH || dropZone == DropZone.SOUTH
                || dropZone == DropZone.EAST || dropZone == DropZone.WEST)) {
            return sourceParent.getTabs().size() <= 1;
        }

        return false;
    }

    /**
     * Checks if this is a no-op tab reorder (same position within same tab group).
     *
     * @return {@code true} when reorder has no effect
     */
    public boolean isNoOpTabReorder() {
        if (dropZone != DropZone.TAB_BAR && dropZone != DropZone.CENTER) {
            return false;
        }

        // Check if target is same tab group as source
        if (targetElement == sourceParent) {
            // For TAB_BAR, check if insert position is same as current
            if (dropZone == DropZone.TAB_BAR) {
                // Account for removal: if source is before insert point, effective insert is index-1
                int effectiveInsert = tabInsertIndex;
                if (sourceTabIndex >= 0 && sourceTabIndex < tabInsertIndex) {
                    effectiveInsert--;
                }
                return sourceTabIndex == effectiveInsert;
            }
            // For CENTER drop on own tab group, it's a no-op
            return true;
        }
        return false;
    }

    /**
     * Checks if this is a tab reorder within the same group.
     *
     * @return {@code true} when this drag is reordering tabs within one group
     */
    public boolean isSameGroupReorder() {
        return dropZone == DropZone.TAB_BAR
            && targetElement == sourceParent;
    }
}
