package org.metalib.papifly.fx.docks.minimize;

import org.metalib.papifly.fx.docks.drag.DropZone;

/**
 * Stores information about where a minimized/floated leaf should be restored.
 * Used for precise restore functionality.
 *
 * @param parentId id of the original parent container
 * @param zone drop zone used for restore placement
 * @param tabIndex tab index used when restoring into a tab group
 * @param splitPosition divider position used when restoring into a split
 * @param siblingId sibling element id used when the original parent no longer exists
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
     *
     * @param parentId id of the target tab group
     * @param tabIndex tab index to restore into
     * @return restore hint targeting a tab position
     */
    public static RestoreHint forTab(String parentId, int tabIndex) {
        return new RestoreHint(parentId, DropZone.TAB_BAR, tabIndex, 0.5, null);
    }

    /**
     * Creates a restore hint for a split position.
     *
     * @param parentId id of the target split container
     * @param zone split side used to place the restored leaf
     * @param splitPosition divider position used for restore
     * @param siblingId sibling element id used as fallback
     * @return restore hint targeting a split position
     */
    public static RestoreHint forSplit(String parentId, DropZone zone, double splitPosition, String siblingId) {
        return new RestoreHint(parentId, zone, -1, splitPosition, siblingId);
    }

    /**
     * Creates a default restore hint (add as tab in root-most container).
     *
     * @return default restore hint
     */
    public static RestoreHint defaultRestore() {
        return new RestoreHint(null, DropZone.CENTER, -1, 0.5, null);
    }
}
