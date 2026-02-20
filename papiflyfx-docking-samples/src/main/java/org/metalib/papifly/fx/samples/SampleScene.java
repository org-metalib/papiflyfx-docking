package org.metalib.papifly.fx.samples;

import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;
import javafx.stage.Stage;
import org.metalib.papifly.fx.docking.api.Theme;

/**
 * Contract for all sample builders in the PapiflyFX samples catalog.
 */
public interface SampleScene {

    /** Category label used to group entries in the catalog (e.g. "Docks", "Code"). */
    String category();

    /** Display title shown in the sample list and window title bar. */
    String title();

    /**
     * Builds the sample content node.
     *
     * @param ownerStage    the primary stage, used as owner for floating windows
     * @param themeProperty shared theme property; bind DockManagers and editors to it
     * @return the root {@link Node} to display in the content area
     */
    Node build(Stage ownerStage, ObjectProperty<Theme> themeProperty);
}
