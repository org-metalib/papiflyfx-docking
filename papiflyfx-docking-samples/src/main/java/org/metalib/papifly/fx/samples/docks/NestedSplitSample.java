package org.metalib.papifly.fx.samples.docks;

import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.metalib.papifly.fx.docks.DockManager;
import org.metalib.papifly.fx.docks.core.DockTabGroup;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.samples.SampleScene;

/**
 * Demonstrates a three-pane IDE-style layout using nested splits:
 * sidebar (25%) | editor (top 70%) + console (bottom 30%).
 */
public class NestedSplitSample implements SampleScene {

    /**
     * Creates the nested-split sample.
     */
    public NestedSplitSample() {
        // Default constructor.
    }

    @Override
    public String category() {
        return "Docks";
    }

    @Override
    public String title() {
        return "Nested Splits";
    }

    @Override
    public Node build(Stage ownerStage, ObjectProperty<Theme> themeProperty) {
        DockManager dm = new DockManager();
        dm.themeProperty().bind(themeProperty);
        dm.setOwnerStage(ownerStage);

        DockTabGroup sidebarGroup = dm.createTabGroup();
        sidebarGroup.addLeaf(dm.createLeaf("Sidebar", centeredLabel("Sidebar")));

        DockTabGroup editorGroup = dm.createTabGroup();
        editorGroup.addLeaf(dm.createLeaf("Editor", centeredLabel("Editor")));

        DockTabGroup consoleGroup = dm.createTabGroup();
        consoleGroup.addLeaf(dm.createLeaf("Console", centeredLabel("Console Output")));

        // vertical: editor (70%) on top, console (30%) on bottom
        var rightArea = dm.createVerticalSplit(editorGroup, consoleGroup, 0.7);

        // horizontal: sidebar (25%) left, right area (75%) right
        dm.setRoot(dm.createHorizontalSplit(sidebarGroup, rightArea, 0.25));

        return dm.getRootPane();
    }

    private static StackPane centeredLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 16px;");
        StackPane pane = new StackPane(label);
        pane.setMinSize(0, 0);
        return pane;
    }
}
