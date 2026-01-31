package org.metalib.papifly.fx.docks.layout.data;

/**
 * DTO representing a floating leaf with its bounds and restore hint.
 */
public record FloatingLeafData(
    LeafData leaf,
    BoundsData bounds,
    RestoreHintData restoreHint
) {
    /**
     * Creates FloatingLeafData with the given parameters.
     */
    public static FloatingLeafData of(LeafData leaf, BoundsData bounds, RestoreHintData restoreHint) {
        return new FloatingLeafData(leaf, bounds, restoreHint);
    }
}
