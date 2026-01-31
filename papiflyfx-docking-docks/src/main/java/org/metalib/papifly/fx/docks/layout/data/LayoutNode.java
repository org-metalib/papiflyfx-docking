package org.metalib.papifly.fx.docks.layout.data;

/**
 * Base sealed interface for layout DTOs.
 * Used for serialization and layout factory construction.
 */
public sealed interface LayoutNode permits LeafData, SplitData, TabGroupData {

    /**
     * Returns the unique identifier for this layout node.
     */
    String id();
}
