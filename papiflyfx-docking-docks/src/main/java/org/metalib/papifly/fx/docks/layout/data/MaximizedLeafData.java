package org.metalib.papifly.fx.docks.layout.data;

/**
 * DTO representing a maximized leaf with its restore hint.
 *
 * @param leaf serialized leaf data for the maximized item
 * @param restoreHint restore target used when un-maximizing
 */
public record MaximizedLeafData(
    LeafData leaf,
    RestoreHintData restoreHint
) {
    /**
     * Creates MaximizedLeafData with the given parameters.
     *
     * @param leaf serialized leaf data for the maximized item
     * @param restoreHint restore target used when un-maximizing
     * @return maximized leaf data instance
     */
    public static MaximizedLeafData of(LeafData leaf, RestoreHintData restoreHint) {
        return new MaximizedLeafData(leaf, restoreHint);
    }
}
