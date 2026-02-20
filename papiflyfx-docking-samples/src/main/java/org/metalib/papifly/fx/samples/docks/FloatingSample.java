package org.metalib.papifly.fx.samples.docks;

import javafx.beans.property.ObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.metalib.papifly.fx.docks.DockManager;
import org.metalib.papifly.fx.docks.core.DockTabGroup;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.samples.SampleScene;

/**
 * Demonstrates floating window support.
 * Use the float button on the "Floatable" tab to detach it to a separate window,
 * and re-dock it by clicking float button again or closing the floating window.
 */
public class FloatingSample implements SampleScene {

    @Override
    public String category() {
        return "Docks";
    }

    @Override
    public String title() {
        return "Floating Window";
    }

    @Override
    public Node build(Stage ownerStage, ObjectProperty<Theme> themeProperty) {
        DockManager dm = new DockManager();
        dm.themeProperty().bind(themeProperty);
        dm.setOwnerStage(ownerStage);

        DockTabGroup mainGroup = dm.createTabGroup();
        mainGroup.addLeaf(dm.createLeaf("Main Panel", centeredLabel("Main Panel")));

        DockTabGroup floatGroup = dm.createTabGroup();
        floatGroup.addLeaf(dm.createLeaf("Floatable", centeredLabel("Click the float button above to detach")));

        dm.setRoot(dm.createHorizontalSplit(mainGroup, floatGroup, 0.5));

        Label info = new Label("Use the float ( \u29C1 ) button on the \"Floatable\" tab to detach it to a floating window.");
        info.setStyle("-fx-text-fill: #888888; -fx-font-size: 12px;");

        HBox toolbar = new HBox(info);
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
