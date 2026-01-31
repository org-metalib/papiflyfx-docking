package org.metalib.papifly.fx.docks.layout.data;

/**
 * DTO representing a minimized leaf with its restore hint.
 */
public record MinimizedLeafData(
    LeafData leaf,
    RestoreHintData restoreHint
) {
    /**
     * Creates MinimizedLeafData with the given parameters.
     */
    public static MinimizedLeafData of(LeafData leaf, RestoreHintData restoreHint) {
        return new MinimizedLeafData(leaf, restoreHint);
    }
}
