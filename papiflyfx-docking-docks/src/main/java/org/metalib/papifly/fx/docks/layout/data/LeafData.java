package org.metalib.papifly.fx.docks.layout.data;

import org.metalib.papifly.fx.docking.api.LeafContentData;

/**
 * DTO representing a leaf node (terminal content) in the layout.
 *
 * @param id leaf identifier
 * @param title title shown for the leaf
 * @param contentFactoryId content factory key used to recreate content
 * @param content persisted content identity and state
 */
public record LeafData(
    String id,
    String title,
    String contentFactoryId,
    LeafContentData content
) implements LayoutNode {

    /**
     * Creates a LeafData with the given parameters.
     *
     * @param id leaf identifier
     * @param title title shown for the leaf
     * @return leaf data without content factory or persisted content
     */
    public static LeafData of(String id, String title) {
        return new LeafData(id, title, null, null);
    }

    /**
     * Creates a LeafData with a content factory reference.
     *
     * @param id leaf identifier
     * @param title title shown for the leaf
     * @param contentFactoryId content factory key used to recreate content
     * @return leaf data with content factory id and no persisted state
     */
    public static LeafData of(String id, String title, String contentFactoryId) {
        return new LeafData(id, title, contentFactoryId, null);
    }

    /**
     * Creates a LeafData with content identity and state.
     *
     * @param id leaf identifier
     * @param title title shown for the leaf
     * @param contentFactoryId content factory key used to recreate content
     * @param content persisted content identity and state
     * @return leaf data with persisted content information
     */
    public static LeafData of(String id, String title, String contentFactoryId, LeafContentData content) {
        return new LeafData(id, title, contentFactoryId, content);
    }
}
