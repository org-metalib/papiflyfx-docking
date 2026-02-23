package org.metalib.papifly.fx.samples.docks;

import javafx.beans.property.ObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.metalib.papifly.fx.docks.DockManager;
import org.metalib.papifly.fx.docks.core.DockLeaf;
import org.metalib.papifly.fx.docks.core.DockTabGroup;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.samples.SampleScene;

/**
 * Demonstrates programmatic minimize and restore via toolbar buttons.
 * The minimized bar appears at the bottom; clicking the bar chip restores the panel.
 */
public class MinimizeSample implements SampleScene {

    /**
     * Creates the minimize sample.
     */
    public MinimizeSample() {
        // Default constructor.
    }

    @Override
    public String category() {
        return "Docks";
    }

    @Override
    public String title() {
        return "Minimize to Bar";
    }

    @Override
    public Node build(Stage ownerStage, ObjectProperty<Theme> themeProperty) {
        DockManager dm = new DockManager();
        dm.themeProperty().bind(themeProperty);
        dm.setOwnerStage(ownerStage);

        DockTabGroup mainGroup = dm.createTabGroup();
        mainGroup.addLeaf(dm.createLeaf("Main", centeredLabel("Main Panel — always visible")));

        DockLeaf sideLeaf = dm.createLeaf("Side Panel", centeredLabel("Side Panel — minimize/restore with the buttons above"));
        DockTabGroup sideGroup = dm.createTabGroup();
        sideGroup.addLeaf(sideLeaf);

        dm.setRoot(dm.createHorizontalSplit(mainGroup, sideGroup, 0.6));

        Button minimizeBtn = new Button("Minimize Side Panel");
        minimizeBtn.setStyle("-fx-background-color: #0e639c; -fx-text-fill: white;");
        minimizeBtn.setOnAction(e -> dm.minimizeLeaf(sideLeaf));

        Button restoreBtn = new Button("Restore Side Panel");
        restoreBtn.setStyle("-fx-background-color: #0e639c; -fx-text-fill: white;");
        restoreBtn.setOnAction(e -> dm.restoreLeaf(sideLeaf));

        HBox toolbar = new HBox(8, minimizeBtn, restoreBtn);
        toolbar.setPadding(new Insets(4, 8, 4, 8));
        toolbar.setStyle("-fx-background-color: #3c3c3c;");

        BorderPane wrapper = new BorderPane();
        wrapper.setTop(toolbar);
        wrapper.setCenter(dm.getRootPane());
        return wrapper;
    }

    private static StackPane centeredLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 14px;");
        StackPane pane = new StackPane(label);
        pane.setMinSize(0, 0);
        return pane;
    }
}
