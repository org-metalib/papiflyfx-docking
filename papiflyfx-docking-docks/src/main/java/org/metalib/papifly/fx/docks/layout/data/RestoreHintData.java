package org.metalib.papifly.fx.docks.layout.data;

import org.metalib.papifly.fx.docks.drag.DropZone;

/**
 * DTO representing a restore hint for serialization.
 *
 * @param parentId id of the parent container where the leaf should return
 * @param zone drop zone name for restore placement
 * @param tabIndex tab index used when restoring to a tab group
 * @param splitPosition divider position used for split restoration
 * @param siblingId sibling element id used as fallback when parent no longer exists
 */
public record RestoreHintData(
    String parentId,
    String zone,
    int tabIndex,
    double splitPosition,
    String siblingId
) {
    /**
     * Creates a RestoreHintData for a tab position.
     *
     * @param parentId id of the target tab group
     * @param tabIndex tab index to restore into
     * @return restore hint targeting a tab position
     */
    public static RestoreHintData forTab(String parentId, int tabIndex) {
        return new RestoreHintData(parentId, DropZone.TAB_BAR.name(), tabIndex, 0.5, null);
    }

    /**
     * Creates a RestoreHintData for a split position.
     *
     * @param parentId id of the target split container
     * @param zone drop zone name describing split side
     * @param splitPosition divider position used for restore
     * @param siblingId sibling element id used as fallback
     * @return restore hint targeting a split position
     */
    public static RestoreHintData forSplit(String parentId, String zone, double splitPosition, String siblingId) {
        return new RestoreHintData(parentId, zone, -1, splitPosition, siblingId);
    }
}
