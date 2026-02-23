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
     *
     * @return JavaFX node used to render this dock element
     */
    Region getNode();

    /**
     * Returns metadata about this dock element.
     *
     * @return element metadata
     */
    DockData getMetadata();

    /**
     * Serializes this element to a DTO for layout persistence.
     *
     * @return serialized layout node
     */
    LayoutNode serialize();

    /**
     * Disposes of this element, unbinding listeners and freeing resources.
     */
    void dispose();

    /**
     * Returns the parent element in the dock hierarchy, or null if this is the root.
     *
     * @return parent element, or {@code null} for the root
     */
    DockElement getParent();

    /**
     * Sets the parent element in the dock hierarchy.
     *
     * @param parent parent element, or {@code null} for root
     */
    void setParent(DockElement parent);
}
