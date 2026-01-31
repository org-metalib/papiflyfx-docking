package org.metalib.papifly.fx.docks;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.metalib.papifly.fx.docks.core.DockLeaf;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Demo application showing session save/restore with floating windows.
 */
public class DemoAppSession extends Application {

    private DockManager dockManager;
    private Path sessionFile;

    @Override
    public void start(Stage primaryStage) {
        sessionFile = Paths.get(System.getProperty("user.home"), ".papiflyfx-session-demo.json");

        dockManager = new DockManager();
        dockManager.setOwnerStage(primaryStage);

        // Set content factory
        dockManager.setContentFactory(id -> {
            if (id != null && id.startsWith("editor:")) {
                String title = id.substring("editor:".length());
                return new StackPane(new Label("Editor: " + title));
            } else if ("files".equals(id)) {
                return new StackPane(new Label("Files Browser"));
            } else if ("properties".equals(id)) {
                return new StackPane(new Label("Properties Panel"));
            } else if ("console".equals(id)) {
                return new StackPane(new Label("Console Output"));
            }
            return new StackPane(new Label(id));
        });

        // Create initial layout
        createInitialLayout();

        // Create control buttons
        HBox controls = new HBox(10);

        Button saveBtn = new Button("Save Session");
        saveBtn.setOnAction(e -> {
            try {
                dockManager.saveSessionToFile(sessionFile);
                System.out.println("Session saved to: " + sessionFile);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        Button loadBtn = new Button("Load Session");
        loadBtn.setOnAction(e -> {
            try {
                if (sessionFile.toFile().exists()) {
                    dockManager.loadSessionFromFile(sessionFile);
                    System.out.println("Session loaded from: " + sessionFile);
                } else {
                    System.out.println("No session file found");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        Button floatEditor2Btn = new Button("Float Editor 2");
        floatEditor2Btn.setOnAction(e -> {
            // Find Editor 2 and float it
            findAndFloatLeaf("Editor 2");
        });

        Button resetBtn = new Button("Reset Layout");
        resetBtn.setOnAction(e -> createInitialLayout());

        controls.getChildren().addAll(saveBtn, loadBtn, floatEditor2Btn, resetBtn);

        // Create main layout
        VBox root = new VBox(5);
        root.getChildren().addAll(controls, dockManager.getRootPane());
        VBox.setVgrow(dockManager.getRootPane(), javafx.scene.layout.Priority.ALWAYS);

        Scene scene = new Scene(root, 1000, 700);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Session Save/Restore Demo");
        primaryStage.show();
    }

    private void createInitialLayout() {
        // Create leaves
        DockLeaf files = dockManager.createLeaf("Files", new StackPane(new Label("Files Browser")));
        files.setContentFactoryId("files");

        DockLeaf editor1 = dockManager.createLeaf("Editor 1", new StackPane(new Label("Editor: Main.java")));
        editor1.setContentFactoryId("editor:Main.java");

        DockLeaf editor2 = dockManager.createLeaf("Editor 2", new StackPane(new Label("Editor: Utils.java")));
        editor2.setContentFactoryId("editor:Utils.java");

        DockLeaf properties = dockManager.createLeaf("Properties", new StackPane(new Label("Properties Panel")));
        properties.setContentFactoryId("properties");

        DockLeaf console = dockManager.createLeaf("Console", new StackPane(new Label("Console Output")));
        console.setContentFactoryId("console");

        // Create tab groups
        var filesGroup = dockManager.createTabGroup();
        filesGroup.addLeaf(files);

        var editorsGroup = dockManager.createTabGroup();
        editorsGroup.addLeaf(editor1);
        editorsGroup.addLeaf(editor2);

        var propertiesGroup = dockManager.createTabGroup();
        propertiesGroup.addLeaf(properties);

        var consoleGroup = dockManager.createTabGroup();
        consoleGroup.addLeaf(console);

        // Create layout
        var topSplit = dockManager.createHorizontalSplit(filesGroup, editorsGroup, 0.2);
        var bottomSplit = dockManager.createHorizontalSplit(consoleGroup, propertiesGroup, 0.7);
        var mainSplit = dockManager.createVerticalSplit(topSplit, bottomSplit, 0.7);

        dockManager.setRoot(mainSplit);
    }

    private void findAndFloatLeaf(String title) {
        var root = dockManager.getRoot();
        if (root != null) {
            DockLeaf leaf = findLeafByTitle(root, title);
            if (leaf != null) {
                dockManager.floatLeaf(leaf);
            }
        }
    }

    private DockLeaf findLeafByTitle(org.metalib.papifly.fx.docks.core.DockElement element, String title) {
        if (element instanceof org.metalib.papifly.fx.docks.core.DockTabGroup tabGroup) {
            for (DockLeaf leaf : tabGroup.getTabs()) {
                if (title.equals(leaf.getMetadata().title())) {
                    return leaf;
                }
            }
        } else if (element instanceof org.metalib.papifly.fx.docks.core.DockSplitGroup split) {
            DockLeaf leaf = findLeafByTitle(split.getFirst(), title);
            if (leaf != null) return leaf;
            return findLeafByTitle(split.getSecond(), title);
        }
        return null;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
