package org.metalib.papifly.fx.docks.layout.data;

/**
 * DTO representing a minimized leaf with its restore hint.
 *
 * @param leaf serialized leaf data for the minimized item
 * @param restoreHint restore target used when un-minimizing
 */
public record MinimizedLeafData(
    LeafData leaf,
    RestoreHintData restoreHint
) {
    /**
     * Creates MinimizedLeafData with the given parameters.
     *
     * @param leaf serialized leaf data for the minimized item
     * @param restoreHint restore target used when un-minimizing
     * @return minimized leaf data instance
     */
    public static MinimizedLeafData of(LeafData leaf, RestoreHintData restoreHint) {
        return new MinimizedLeafData(leaf, restoreHint);
    }
}
