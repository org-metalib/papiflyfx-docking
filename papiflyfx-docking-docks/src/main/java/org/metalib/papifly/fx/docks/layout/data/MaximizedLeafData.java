package org.metalib.papifly.fx.docks.layout.data;

/**
 * DTO representing a maximized leaf with its restore hint.
 */
public record MaximizedLeafData(
    LeafData leaf,
    RestoreHintData restoreHint
) {
    /**
     * Creates MaximizedLeafData with the given parameters.
     */
    public static MaximizedLeafData of(LeafData leaf, RestoreHintData restoreHint) {
        return new MaximizedLeafData(leaf, restoreHint);
    }
}
