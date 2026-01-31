package org.metalib.papifly.fx.docks.layout.data;

/**
 * DTO representing a leaf node (terminal content) in the layout.
 */
public record LeafData(
    String id,
    String title,
    String contentFactoryId,
    LeafContentData content
) implements LayoutNode {

    /**
     * Creates a LeafData with the given parameters.
     */
    public static LeafData of(String id, String title) {
        return new LeafData(id, title, null, null);
    }

    /**
     * Creates a LeafData with a content factory reference.
     */
    public static LeafData of(String id, String title, String contentFactoryId) {
        return new LeafData(id, title, contentFactoryId, null);
    }

    /**
     * Creates a LeafData with content identity and state.
     */
    public static LeafData of(String id, String title, String contentFactoryId, LeafContentData content) {
        return new LeafData(id, title, contentFactoryId, content);
    }
}
