package org.metalib.papifly.fx.docks.core;

import javafx.scene.layout.Region;
import org.metalib.papifly.fx.docks.layout.data.LayoutNode;

/**
 * Base interface for all docking elements.
 * Every entity in the docking system must implement this contract.
 */
public interface DockElement {

    /**
     * Returns the actual JavaFX Region that represents this dock element.
     */
    Region getNode();

    /**
     * Returns metadata about this dock element.
     */
    DockData getMetadata();

    /**
     * Serializes this element to a DTO for layout persistence.
     */
    LayoutNode serialize();

    /**
     * Disposes of this element, unbinding listeners and freeing resources.
     */
    void dispose();

    /**
     * Returns the parent element in the dock hierarchy, or null if this is the root.
     */
    DockElement getParent();

    /**
     * Sets the parent element in the dock hierarchy.
     */
    void setParent(DockElement parent);
}
