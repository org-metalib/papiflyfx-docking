package org.metalib.papifly.fx.docks.layout.data;

/**
 * Base sealed interface for layout DTOs.
 * Used for serialization and layout factory construction.
 */
public sealed interface LayoutNode permits LeafData, SplitData, TabGroupData {

    /**
     * Returns the unique identifier for this layout node.
     *
     * @return unique layout node identifier
     */
    String id();

    /**
     * Visits this node using a type-safe visitor instead of external type switches.
     *
     * @param visitor visitor to apply
     * @param <T> visitor result type
     * @return visitor result
     */
    <T> T accept(LayoutNodeVisitor<T> visitor);
}
