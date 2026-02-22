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
 * Demonstrates two tab groups side by side: left with 3 tabs, right with 2 tabs.
 * Shows tab close button, float control, and minimize control per tab.
 */
public class TabGroupSample implements SampleScene {

    @Override
    public String category() {
        return "Docks";
    }

    @Override
    public String title() {
        return "Tab Groups";
    }

    @Override
    public Node build(Stage ownerStage, ObjectProperty<Theme> themeProperty) {
        DockManager dm = new DockManager();
        dm.themeProperty().bind(themeProperty);
        dm.setOwnerStage(ownerStage);

        DockTabGroup leftGroup = dm.createTabGroup();
        leftGroup.addLeaf(dm.createLeaf("Tab 1", centeredLabel("Tab 1 Content")));
        leftGroup.addLeaf(dm.createLeaf("Tab 2", centeredLabel("Tab 2 Content")));
        leftGroup.addLeaf(dm.createLeaf("Tab 3", centeredLabel("Tab 3 Content")));

        DockTabGroup rightGroup = dm.createTabGroup();
        rightGroup.addLeaf(dm.createLeaf("Tab A", centeredLabel("Tab A Content")));
        rightGroup.addLeaf(dm.createLeaf("Tab B", centeredLabel("Tab B Content")));

        dm.setRoot(dm.createHorizontalSplit(leftGroup, rightGroup, 0.5));

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
