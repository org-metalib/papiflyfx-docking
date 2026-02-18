package org.metalib.papifly.fx.docks;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.metalib.papifly.fx.docks.core.DockElement;
import org.metalib.papifly.fx.docks.core.DockLeaf;
import org.metalib.papifly.fx.docks.core.DockSplitGroup;
import org.metalib.papifly.fx.docks.core.DockTabGroup;
import org.metalib.papifly.fx.docking.api.ContentStateAdapter;
import org.metalib.papifly.fx.docks.layout.ContentStateRegistry;
import org.metalib.papifly.fx.docking.api.ContentFactory;
import org.metalib.papifly.fx.docking.api.LeafContentData;
import org.metalib.papifly.fx.docks.layout.data.DockSessionData;
import org.metalib.papifly.fx.docking.api.Theme;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Demo application showcasing the papiflyfx-docks docking framework.
 */
public class DemoApp extends Application {

    private static final String CONTENT_FILES = "files";
    private static final String CONTENT_CONSOLE = "console";
    private static final String CONTENT_PROPERTIES = "properties";
    private static final String CONTENT_EDITOR_PREFIX = "editor:";
    private static final String CONTENT_PANEL_PREFIX = "panel:";
    private static final String CONTENT_TYPE_EDITOR = "editor";
    private static final String EDITOR_STATE_TEXT = "text";
    private static final String EDITOR_STATE_CARET = "caret";
    private static final int EDITOR_CONTENT_VERSION = 1;
    private static final String PANEL_CLOSE_BUTTON_KEY = "panelCloseButton";
    private static final Pattern PANEL_TITLE_PATTERN = Pattern.compile("^Panel\\s+(\\d+)$");
    private static final String SESSION_EXTENSION = ".json";
    private static final Pattern SESSION_NAME_PATTERN = Pattern.compile("[A-Za-z0-9 _.-]+");

    private DockManager dockManager;
    private final Path sessionsDir = Paths.get("target", "sessions");
    private MenuItem restoreSessionItem;

    @Override
    public void start(Stage primaryStage) {
        // Create dock manager with dark theme
        dockManager = new DockManager(Theme.dark());

        // Set owner stage for floating windows
        dockManager.setOwnerStage(primaryStage);

        // Configure content factory for session restoration
        dockManager.setContentFactory(createContentFactory());

        dockManager.setContentStateRegistry(createContentStateRegistry());

        // Build initial layout
        buildInitialLayout();

        // Create menu bar
        MenuBar menuBar = createMenuBar();

        // Create main layout
        VBox root = new VBox();
        root.setMinSize(0, 0); // Allow shrinking
        root.getChildren().addAll(menuBar, dockManager.getRootPane());
        VBox.setVgrow(dockManager.getRootPane(), Priority.ALWAYS);

        Scene scene = new Scene(root, 1200, 800);
        primaryStage.setTitle("PapiflyFX Docks Demo");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void buildInitialLayout() {
        dockManager.setRoot(createInitialLayout(dockManager));
        updatePanelCounterFromLayout();
    }

    /**
     * Builds the initial demo layout tree.
     *
     * <p>This is extracted so tests can validate the demo layout without duplicating the layout-building code.
     */
    public static DockElement createInitialLayout(DockManager dockManager) {
        // Create sample content panels
        DockLeaf fileExplorer = createContentLeaf(dockManager, "Files", CONTENT_FILES, createFileExplorerContent());
        DockLeaf editor1 = createEditorLeaf(
            dockManager,
            "Editor 1",
            "Editor 1 - main.java"
        );
        DockLeaf editor2 = createEditorLeaf(
            dockManager,
            "Editor 2",
            "Editor 2 - utils.java"
        );
        DockLeaf console = createContentLeaf(dockManager, "Console", CONTENT_CONSOLE, createConsoleContent());
        DockLeaf properties = createContentLeaf(dockManager, "Properties", CONTENT_PROPERTIES, createPropertiesContent());

        // Build layout:
        // ┌──────────────────────────────────────┐
        // │  Files  │    Editor Tabs    │ Props  │
        // │         │                   │        │
        // │  (20%)  │      (60%)        │ (20%)  │
        // │         ├───────────────────┤        │
        // │         │     Console       │        │
        // │         │      (30%)        │        │
        // └──────────────────────────────────────┘

        // Create tab group for editors
        DockTabGroup editorTabs = dockManager.createTabGroup();
        editorTabs.addLeaf(editor1);
        editorTabs.addLeaf(editor2);
        dockManager.setupTabGroupDragHandlers(editorTabs);

        DockTabGroup fileExplorerGroup = wrapLeaf(dockManager, fileExplorer);
        DockTabGroup consoleGroup = wrapLeaf(dockManager, console);
        DockTabGroup propertiesGroup = wrapLeaf(dockManager, properties);

        // Vertical split: editors on top, console on bottom
        DockSplitGroup centerArea = dockManager.createVerticalSplit(editorTabs, consoleGroup, 0.7);

        // Horizontal split: files | center | properties
        DockSplitGroup mainArea = dockManager.createHorizontalSplit(fileExplorerGroup, centerArea, 0.2);
        DockSplitGroup fullLayout = dockManager.createHorizontalSplit(mainArea, propertiesGroup, 0.8);

        return fullLayout;
    }

    private static DockLeaf createContentLeaf(DockManager dockManager, String title, String contentFactoryId, Region content) {
        DockLeaf leaf = dockManager.createLeaf(title, content);
        leaf.setContentFactoryId(contentFactoryId);
        return leaf;
    }

    private static DockLeaf createEditorLeaf(DockManager dockManager, String title, String documentName) {
        DockLeaf leaf = createContentLeaf(
            dockManager,
            title,
            CONTENT_EDITOR_PREFIX + documentName,
            createEditorContent(documentName)
        );
        leaf.setContentData(LeafContentData.of(CONTENT_TYPE_EDITOR, documentName, EDITOR_CONTENT_VERSION));
        return leaf;
    }

    private static DockTabGroup wrapLeaf(DockManager dockManager, DockLeaf leaf) {
        DockTabGroup group = dockManager.createTabGroup();
        group.addLeaf(leaf);
        return group;
    }

    private static Region createFileExplorerContent() {
        TreeItem<String> root = new TreeItem<>("Project");
        root.setExpanded(true);

        TreeItem<String> src = new TreeItem<>("src");
        src.setExpanded(true);
        src.getChildren().addAll(
            new TreeItem<>("main.java"),
            new TreeItem<>("utils.java"),
            new TreeItem<>("config.java")
        );

        TreeItem<String> resources = new TreeItem<>("resources");
        resources.getChildren().addAll(
            new TreeItem<>("styles.css"),
            new TreeItem<>("icon.png")
        );

        root.getChildren().addAll(src, resources);

        TreeView<String> treeView = new TreeView<>(root);
        treeView.setStyle("-fx-background-color: #252526; -fx-text-fill: #cccccc;");
        treeView.setMinSize(0, 0); // Allow shrinking

        return treeView;
    }

    private static TextArea createEditorContent(String title) {
        TextArea textArea = new TextArea();
        textArea.setText("// " + title + "\n\npublic class Main {\n    public static void main(String[] args) {\n        System.out.println(\"Hello, World!\");\n    }\n}");
        textArea.setStyle("-fx-control-inner-background: #1e1e1e; -fx-text-fill: #d4d4d4; -fx-font-family: monospace;");
        textArea.setWrapText(false);
        textArea.setMinSize(0, 0); // Allow shrinking

        return textArea;
    }

    private static Region createConsoleContent() {
        TextArea console = new TextArea();
        console.setEditable(false);
        console.setText("> Build started...\n> Compiling sources...\n> Build successful.\n> ");
        console.setStyle("-fx-control-inner-background: #1e1e1e; -fx-text-fill: #00ff00; -fx-font-family: monospace;");
        console.setMinSize(0, 0); // Allow shrinking

        return console;
    }

    private static Region createPropertiesContent() {
        VBox props = new VBox(8);
        props.setPadding(new Insets(8));
        props.setStyle("-fx-background-color: #252526;");
        props.setMinSize(0, 0); // Allow shrinking

        Label header = new Label("Properties");
        header.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(5);

        addPropertyRow(grid, 0, "Name:", "Main.java");
        addPropertyRow(grid, 1, "Size:", "1.2 KB");
        addPropertyRow(grid, 2, "Modified:", "2024-01-15");
        addPropertyRow(grid, 3, "Type:", "Java Source");

        props.getChildren().addAll(header, grid);

        return props;
    }

    private static void addPropertyRow(GridPane grid, int row, String label, String value) {
        Label labelNode = new Label(label);
        labelNode.setStyle("-fx-text-fill: #888888;");

        Label valueNode = new Label(value);
        valueNode.setStyle("-fx-text-fill: #cccccc;");

        grid.add(labelNode, 0, row);
        grid.add(valueNode, 1, row);
    }

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        menuBar.setStyle("-fx-background-color: #3c3c3c;");

        // File menu
        Menu fileMenu = new Menu("File");
        MenuItem newWindow = new MenuItem("New Window");
        MenuItem exit = new MenuItem("Exit");
        exit.setOnAction(e -> System.exit(0));
        fileMenu.getItems().addAll(newWindow, new SeparatorMenuItem(), exit);

        // View menu
        Menu viewMenu = new Menu("View");
        MenuItem addPanel = new MenuItem("Add Panel");
        addPanel.setOnAction(e -> addNewPanel());

        MenuItem toggleTheme = new MenuItem("Toggle Theme");
        toggleTheme.setOnAction(e -> toggleTheme());

        MenuItem restoreMaximized = new MenuItem("Restore Maximized");
        restoreMaximized.setOnAction(e -> {
            if (dockManager.isMaximized()) {
                dockManager.restoreMaximized();
            }
        });

        viewMenu.getItems().addAll(addPanel, toggleTheme, new SeparatorMenuItem(), restoreMaximized);

        // Window menu
        Menu windowMenu = new Menu("Window");
        MenuItem resetLayout = new MenuItem("Reset Layout");
        resetLayout.setOnAction(e -> {
            dockManager.dispose();
            buildInitialLayout();
        });
        windowMenu.getItems().add(resetLayout);

        // Sessions menu
        Menu sessionsMenu = new Menu("Sessions");
        MenuItem saveSession = new MenuItem("Save Session...");
        saveSession.setOnAction(e -> promptSaveSession());
        restoreSessionItem = new MenuItem("Restore Session...");
        restoreSessionItem.setOnAction(e -> promptRestoreSession());
        restoreSessionItem.setDisable(true);
        sessionsMenu.getItems().addAll(saveSession, restoreSessionItem);

        menuBar.getMenus().addAll(fileMenu, viewMenu, windowMenu, sessionsMenu);
        refreshSessionMenuState();

        return menuBar;
    }

    private int panelCounter = 1;

    private void addNewPanel() {
        String title = "Panel " + (++panelCounter);

        VBox content = createPanelContent(title);
        DockLeaf newLeaf = dockManager.createLeaf(title, content);
        newLeaf.setContentFactoryId(CONTENT_PANEL_PREFIX + title);
        DockTabGroup newGroup = wrapLeaf(dockManager, newLeaf);

        wirePanelCloseButton(newLeaf);

        // Add to root as a split
        DockElement currentRoot = dockManager.getRoot();
        if (currentRoot != null) {
            DockSplitGroup newSplit = dockManager.createHorizontalSplit(currentRoot, newGroup, 0.75);
            dockManager.setRoot(newSplit);
        } else {
            dockManager.setRoot(newGroup);
        }
    }

    private VBox createPanelContent(String title) {
        VBox content = new VBox(10);
        content.setAlignment(Pos.CENTER);
        content.setStyle("-fx-background-color: #252526;");
        content.setMinSize(0, 0); // Allow shrinking

        Label label = new Label(title);
        label.setStyle("-fx-text-fill: white; -fx-font-size: 16px;");

        Button closeBtn = new Button("Close This Panel");
        closeBtn.setStyle("-fx-background-color: #0e639c; -fx-text-fill: white;");

        content.getChildren().addAll(label, closeBtn);
        content.getProperties().put(PANEL_CLOSE_BUTTON_KEY, closeBtn);

        return content;
    }

    private ContentFactory createContentFactory() {
        return factoryId -> {
            if (factoryId == null || factoryId.isBlank()) {
                return null;
            }
            if (CONTENT_FILES.equals(factoryId)) {
                return createFileExplorerContent();
            }
            if (CONTENT_CONSOLE.equals(factoryId)) {
                return createConsoleContent();
            }
            if (CONTENT_PROPERTIES.equals(factoryId)) {
                return createPropertiesContent();
            }
            if (factoryId.startsWith(CONTENT_EDITOR_PREFIX)) {
                String title = factoryId.substring(CONTENT_EDITOR_PREFIX.length());
                return createEditorContent(title);
            }
            if (factoryId.startsWith(CONTENT_PANEL_PREFIX)) {
                String title = factoryId.substring(CONTENT_PANEL_PREFIX.length());
                return createPanelContent(title);
            }
            return null;
        };
    }

    private ContentStateRegistry createContentStateRegistry() {
        ContentStateRegistry registry = ContentStateRegistry.fromServiceLoader();
        registry.register(new EditorContentAdapter());
        return registry;
    }

    private void promptSaveSession() {
        if (!ensureSessionsDir()) {
            return;
        }

        TextInputDialog dialog = new TextInputDialog("Session " + (loadSessionNames().size() + 1));
        dialog.setTitle("Save Session");
        dialog.setHeaderText("Save current session");
        dialog.setContentText("Session name:");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return;
        }

        String name = result.get().trim();
        if (name.isEmpty()) {
            return;
        }

        if (!SESSION_NAME_PATTERN.matcher(name).matches()) {
            showErrorAlert("Invalid Name", "Session name contains unsupported characters.");
            return;
        }

        Path target = sessionsDir.resolve(name + SESSION_EXTENSION);
        if (Files.exists(target)) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Overwrite Session");
            confirm.setHeaderText("Session name already exists");
            confirm.setContentText("Overwrite \"" + name + "\"?");
            Optional<ButtonType> choice = confirm.showAndWait();
            if (choice.isEmpty() || choice.get() != ButtonType.OK) {
                return;
            }
        }

        if (isSessionEmpty(dockManager.captureSession())) {
            Alert warn = new Alert(Alert.AlertType.WARNING);
            warn.setTitle("No Session");
            warn.setHeaderText("Nothing to save");
            warn.setContentText("There is no session to save right now.");
            warn.showAndWait();
            return;
        }

        try {
            dockManager.saveSessionToFile(target);
        } catch (RuntimeException e) {
            showErrorAlert("Save Failed", "Could not save session \"" + name + "\".");
            return;
        }

        refreshSessionMenuState();
    }

    private void promptRestoreSession() {
        List<String> names = loadSessionNames();
        if (names.isEmpty()) {
            return;
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(names.get(0), names);
        dialog.setTitle("Restore Session");
        dialog.setHeaderText("Restore saved session");
        dialog.setContentText("Session name:");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return;
        }

        if (!ensureSessionsDir()) {
            return;
        }

        String name = result.get();
        Path target = sessionsDir.resolve(name + SESSION_EXTENSION);

        try {
            dockManager.loadSessionFromFile(target);
        } catch (RuntimeException e) {
            showErrorAlert("Restore Failed", "Could not restore session \"" + name + "\".");
            return;
        }

        wirePanelCloseButtons(dockManager.getRoot());
        updatePanelCounterFromLayout();
    }

    private void toggleTheme() {
        Theme current = dockManager.getTheme();
        if (current.background().equals(Theme.dark().background())) {
            dockManager.setTheme(Theme.light());
        } else {
            dockManager.setTheme(Theme.dark());
        }
    }

    private void wirePanelCloseButtons(DockElement element) {
        if (element == null) {
            return;
        }
        if (element instanceof DockTabGroup tabGroup) {
            for (DockLeaf leaf : tabGroup.getTabs()) {
                wirePanelCloseButton(leaf);
            }
        } else if (element instanceof DockSplitGroup splitGroup) {
            wirePanelCloseButtons(splitGroup.getFirst());
            wirePanelCloseButtons(splitGroup.getSecond());
        }
    }

    private void wirePanelCloseButton(DockLeaf leaf) {
        if (leaf == null) {
            return;
        }
        if (leaf.getContent() instanceof Region region) {
            Object stored = region.getProperties().get(PANEL_CLOSE_BUTTON_KEY);
            if (stored instanceof Button button) {
                button.setOnAction(e -> leaf.requestClose());
            }
        }
    }

    private void updatePanelCounterFromLayout() {
        DockElement root = dockManager.getRoot();
        int max = findMaxPanelNumber(root);
        if (max > panelCounter) {
            panelCounter = max;
        }
    }

    private boolean ensureSessionsDir() {
        try {
            Files.createDirectories(sessionsDir);
            return true;
        } catch (IOException e) {
            showErrorAlert("Storage Error", "Could not access " + sessionsDir + ".");
            return false;
        }
    }

    private List<String> loadSessionNames() {
        if (!Files.isDirectory(sessionsDir)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(sessionsDir)) {
            return stream
                .map(Path::getFileName)
                .map(Path::toString)
                .filter(name -> name.endsWith(SESSION_EXTENSION))
                .map(name -> name.substring(0, name.length() - SESSION_EXTENSION.length()))
                .sorted()
                .toList();
        } catch (IOException e) {
            showErrorAlert("Storage Error", "Could not read sessions from " + sessionsDir + ".");
            return List.of();
        }
    }

    private void refreshSessionMenuState() {
        restoreSessionItem.setDisable(loadSessionNames().isEmpty());
    }

    private void showErrorAlert(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Session Persistence");
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private boolean isSessionEmpty(DockSessionData session) {
        if (session == null) {
            return true;
        }
        boolean layoutEmpty = session.layout() == null;
        boolean floatingEmpty = session.floating() == null || session.floating().isEmpty();
        boolean minimizedEmpty = session.minimized() == null || session.minimized().isEmpty();
        boolean maximizedEmpty = session.maximized() == null;
        return layoutEmpty && floatingEmpty && minimizedEmpty && maximizedEmpty;
    }

    private int findMaxPanelNumber(DockElement element) {
        if (element == null) {
            return 0;
        }
        int max = 0;
        if (element instanceof DockTabGroup tabGroup) {
            for (DockLeaf leaf : tabGroup.getTabs()) {
                String title = leaf.getMetadata().title();
                Matcher matcher = PANEL_TITLE_PATTERN.matcher(title);
                if (matcher.matches()) {
                    int value = Integer.parseInt(matcher.group(1));
                    if (value > max) {
                        max = value;
                    }
                }
            }
        } else if (element instanceof DockSplitGroup splitGroup) {
            max = Math.max(findMaxPanelNumber(splitGroup.getFirst()), findMaxPanelNumber(splitGroup.getSecond()));
        }
        return max;
    }

    @Override
    public void stop() {
        if (dockManager != null) {
            dockManager.dispose();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    private static class EditorContentAdapter implements ContentStateAdapter {
        @Override
        public String getTypeKey() {
            return CONTENT_TYPE_EDITOR;
        }

        @Override
        public int getVersion() {
            return EDITOR_CONTENT_VERSION;
        }

        @Override
        public Map<String, Object> saveState(String contentId, javafx.scene.Node content) {
            if (!(content instanceof TextArea textArea)) {
                return null;
            }
            Map<String, Object> state = new LinkedHashMap<>();
            state.put(EDITOR_STATE_TEXT, textArea.getText());
            state.put(EDITOR_STATE_CARET, textArea.getCaretPosition());
            return state;
        }

        @Override
        public javafx.scene.Node restore(LeafContentData content) {
            String title = content.contentId() != null ? content.contentId() : "Editor";
            TextArea textArea = createEditorContent(title);
            if (content.state() == null) {
                return textArea;
            }
            Object text = content.state().get(EDITOR_STATE_TEXT);
            if (text instanceof String textValue) {
                textArea.setText(textValue);
            }
            Object caret = content.state().get(EDITOR_STATE_CARET);
            if (caret instanceof Number caretValue) {
                textArea.positionCaret(caretValue.intValue());
            }
            return textArea;
        }
    }
}
