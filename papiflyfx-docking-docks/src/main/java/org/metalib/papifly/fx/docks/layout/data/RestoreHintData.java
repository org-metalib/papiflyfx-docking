package org.metalib.papifly.fx.docks.layout.data;

import org.metalib.papifly.fx.docks.drag.DropZone;

/**
 * DTO representing a restore hint for serialization.
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
     */
    public static RestoreHintData forTab(String parentId, int tabIndex) {
        return new RestoreHintData(parentId, DropZone.TAB_BAR.name(), tabIndex, 0.5, null);
    }

    /**
     * Creates a RestoreHintData for a split position.
     */
    public static RestoreHintData forSplit(String parentId, String zone, double splitPosition, String siblingId) {
        return new RestoreHintData(parentId, zone, -1, splitPosition, siblingId);
    }
}
