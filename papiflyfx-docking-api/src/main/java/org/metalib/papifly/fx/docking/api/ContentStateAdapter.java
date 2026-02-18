package org.metalib.papifly.fx.docking.api;

import javafx.scene.Node;

import java.util.Map;

/**
 * Adapter for capturing and restoring content state for dock leaves.
 * Implementations are discovered via a registry or ServiceLoader.
 */
public interface ContentStateAdapter {

    /**
     * Returns the stable type key used for lookup.
     */
    String getTypeKey();

    /**
     * Returns the current schema version for the content state.
     */
    int getVersion();

    /**
     * Captures content state for the given content node.
     *
     * @param contentId stable content identifier
     * @param content   content node to capture
     * @return a state map that can be serialized, or null for no state
     */
    Map<String, Object> saveState(String contentId, Node content);

    /**
     * Restores content from persisted data.
     *
     * @param content persisted content data
     * @return restored content node, or null if it cannot be restored
     */
    Node restore(LeafContentData content);
}
