package org.metalib.papifly.fx.docks.minimize;

import org.metalib.papifly.fx.docks.drag.DropZone;

/**
 * Stores information about where a minimized/floated leaf should be restored.
 * Used for precise restore functionality.
 */
public record RestoreHint(
    String parentId,
    DropZone zone,
    int tabIndex,
    double splitPosition,
    String siblingId  // For splits: the sibling element ID (used as fallback when parent is gone)
) {
    /**
     * Creates a restore hint for a tab position.
     */
    public static RestoreHint forTab(String parentId, int tabIndex) {
        return new RestoreHint(parentId, DropZone.TAB_BAR, tabIndex, 0.5, null);
    }

    /**
     * Creates a restore hint for a split position.
     */
    public static RestoreHint forSplit(String parentId, DropZone zone, double splitPosition, String siblingId) {
        return new RestoreHint(parentId, zone, -1, splitPosition, siblingId);
    }

    /**
     * Creates a default restore hint (add as tab in root-most container).
     */
    public static RestoreHint defaultRestore() {
        return new RestoreHint(null, DropZone.CENTER, -1, 0.5, null);
    }
}
