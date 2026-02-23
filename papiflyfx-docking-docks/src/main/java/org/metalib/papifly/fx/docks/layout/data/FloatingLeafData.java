package org.metalib.papifly.fx.docks.layout.data;

/**
 * DTO representing a floating leaf with its bounds and restore hint.
 *
 * @param leaf serialized leaf data for the floating item
 * @param bounds floating window bounds
 * @param restoreHint restore target used when docking back
 */
public record FloatingLeafData(
    LeafData leaf,
    BoundsData bounds,
    RestoreHintData restoreHint
) {
    /**
     * Creates FloatingLeafData with the given parameters.
     *
     * @param leaf serialized leaf data for the floating item
     * @param bounds floating window bounds
     * @param restoreHint restore target used when docking back
     * @return floating leaf data instance
     */
    public static FloatingLeafData of(LeafData leaf, BoundsData bounds, RestoreHintData restoreHint) {
        return new FloatingLeafData(leaf, bounds, restoreHint);
    }
}
