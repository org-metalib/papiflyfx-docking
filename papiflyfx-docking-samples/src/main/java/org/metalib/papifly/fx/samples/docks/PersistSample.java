package org.metalib.papifly.fx.samples.docks;

import javafx.beans.property.ObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.metalib.papifly.fx.docks.DockManager;
import org.metalib.papifly.fx.docks.core.DockTabGroup;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.samples.SampleScene;

/**
 * Demonstrates full round-trip session persistence.
 * "Save Session" serialises the current layout to JSON and displays it in a text overlay.
 * "Restore Session" reapplies the saved JSON, rebuilding the layout from scratch.
 */
public class PersistSample implements SampleScene {

    /** ID that the smoke test uses to find the JSON TextArea. */
    public static final String JSON_AREA_ID = "persist-sample-json-area";

    @Override
    public String category() {
        return "Docks";
    }

    @Override
    public String title() {
        return "Session Persist";
    }

    @Override
    public Node build(Stage ownerStage, ObjectProperty<Theme> themeProperty) {
        DockManager dm = new DockManager();
        dm.themeProperty().bind(themeProperty);
        dm.setOwnerStage(ownerStage);

        // IDE-style layout: sidebar | editor + console
        DockTabGroup sidebarGroup = dm.createTabGroup();
        sidebarGroup.addLeaf(dm.createLeaf("Sidebar", centeredLabel("Sidebar")));

        DockTabGroup editorGroup = dm.createTabGroup();
        editorGroup.addLeaf(dm.createLeaf("Editor", centeredLabel("Editor")));

        DockTabGroup consoleGroup = dm.createTabGroup();
        consoleGroup.addLeaf(dm.createLeaf("Console", centeredLabel("Console Output")));

        var rightArea = dm.createVerticalSplit(editorGroup, consoleGroup, 0.7);
        dm.setRoot(dm.createHorizontalSplit(sidebarGroup, rightArea, 0.25));

        // JSON display overlay (hidden by default)
        TextArea jsonArea = new TextArea();
        jsonArea.setId(JSON_AREA_ID);
        jsonArea.setEditable(false);
        jsonArea.setWrapText(false);
        jsonArea.setStyle(
            "-fx-control-inner-background: #1e1e1e; -fx-text-fill: #d4d4d4; -fx-font-family: monospace; -fx-font-size: 11px;");
        jsonArea.setVisible(false);
        jsonArea.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        // Mutable holder for the saved JSON string
        String[] savedJson = {""};

        Button hideJsonBtn = new Button("Hide JSON");
        hideJsonBtn.setStyle("-fx-background-color: #555555; -fx-text-fill: white;");
        hideJsonBtn.setVisible(false);
        hideJsonBtn.setOnAction(e -> {
            jsonArea.setVisible(false);
            hideJsonBtn.setVisible(false);
        });

        Button saveBtn = new Button("Save Session");
        saveBtn.setStyle("-fx-background-color: #0e639c; -fx-text-fill: white;");
        saveBtn.setOnAction(e -> {
            savedJson[0] = dm.saveSessionToString();
            jsonArea.setText(savedJson[0]);
            jsonArea.setVisible(true);
            hideJsonBtn.setVisible(true);
        });

        Button restoreBtn = new Button("Restore Session");
        restoreBtn.setStyle("-fx-background-color: #0e639c; -fx-text-fill: white;");
        restoreBtn.setOnAction(e -> {
            if (!savedJson[0].isEmpty()) {
                jsonArea.setVisible(false);
                hideJsonBtn.setVisible(false);
                dm.restoreSessionFromString(savedJson[0]);
            }
        });

        HBox toolbar = new HBox(8, saveBtn, restoreBtn, hideJsonBtn);
        toolbar.setPadding(new Insets(4, 8, 4, 8));
        toolbar.setStyle("-fx-background-color: #3c3c3c;");

        StackPane centerStack = new StackPane(dm.getRootPane(), jsonArea);

        BorderPane wrapper = new BorderPane();
        wrapper.setTop(toolbar);
        wrapper.setCenter(centerStack);
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
