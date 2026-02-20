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
 * Demonstrates the simplest two-pane vertical split layout.
 */
public class BasicSplitSample implements SampleScene {

    @Override
    public String category() {
        return "Docks";
    }

    @Override
    public String title() {
        return "Basic Split";
    }

    @Override
    public Node build(Stage ownerStage, ObjectProperty<Theme> themeProperty) {
        DockManager dm = new DockManager();
        dm.themeProperty().bind(themeProperty);

        var leafA = dm.createLeaf("Panel A", centeredLabel("Panel A Content"));
        var leafB = dm.createLeaf("Panel B", centeredLabel("Panel B Content"));

        DockTabGroup groupA = dm.createTabGroup();
        groupA.addLeaf(leafA);

        DockTabGroup groupB = dm.createTabGroup();
        groupB.addLeaf(leafB);

        dm.setRoot(dm.createVerticalSplit(groupA, groupB, 0.7));

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
