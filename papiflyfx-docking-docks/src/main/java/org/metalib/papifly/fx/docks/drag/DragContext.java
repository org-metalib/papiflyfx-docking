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

    public DockLeaf getSourceLeaf() {
        return sourceLeaf;
    }

    public DockTabGroup getSourceParent() {
        return sourceParent;
    }

    public DockElement getTargetElement() {
        return targetElement;
    }

    public void setTargetElement(DockElement target) {
        this.targetElement = target;
    }

    public DropZone getDropZone() {
        return dropZone;
    }

    public void setDropZone(DropZone zone) {
        this.dropZone = zone;
    }

    public int getTabInsertIndex() {
        return tabInsertIndex;
    }

    public void setTabInsertIndex(int index) {
        this.tabInsertIndex = index;
    }

    public int getSourceTabIndex() {
        return sourceTabIndex;
    }

    public double getStartX() {
        return startX;
    }

    public double getStartY() {
        return startY;
    }

    /**
     * Checks if this is a valid drop (target exists and zone is not NONE).
     */
    public boolean isValidDrop() {
        return targetElement != null && dropZone != DropZone.NONE;
    }

    /**
     * Checks if attempting to drop on self (same leaf or same position in same tab group).
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
     */
    public boolean isSameGroupReorder() {
        return dropZone == DropZone.TAB_BAR
            && targetElement == sourceParent;
    }
}
