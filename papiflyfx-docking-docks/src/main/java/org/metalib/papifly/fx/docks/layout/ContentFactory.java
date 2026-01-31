package org.metalib.papifly.fx.docks.layout;

import javafx.scene.Node;

/**
 * Factory interface for creating content nodes by ID.
 * Used during layout restoration to recreate content.
 */
@FunctionalInterface
public interface ContentFactory {

    /**
     * Creates a content node for the given factory ID.
     *
     * @param factoryId the identifier for the content type
     * @return the created Node, or null if the ID is unknown
     */
    Node create(String factoryId);
}
