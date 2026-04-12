package org.metalib.papifly.fx.samples;

import javafx.application.Application;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import org.metalib.papifly.fx.docks.DockManager;
import org.metalib.papifly.fx.docking.api.DisposableContent;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.samples.catalog.SampleCatalog;
import org.metalib.papifly.fx.tree.api.CellState;
import org.metalib.papifly.fx.tree.api.TreeItem;
import org.metalib.papifly.fx.tree.api.TreeView;
import org.metalib.papifly.fx.tree.render.TreeRenderContext;
import org.metalib.papifly.fx.tree.theme.TreeViewTheme;
import org.metalib.papifly.fx.tree.theme.TreeViewThemeMapper;
import org.metalib.papifly.fx.ui.UiCommonThemeSupport;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Main application shell for the PapiflyFX Docking Samples.
 *
 * <p>Layout: top bar (title + theme toggle) | left navigation tree | center content area.</p>
 */
public class SamplesApp extends Application {

    private final ObjectProperty<Theme> themeProperty = new SimpleObjectProperty<>(Theme.dark());
    private Stage primaryStage;
    private HBox topBar;
    private final StackPane contentArea = new StackPane();
    private final Map<String, TreeItem<NavigationEntry>> sampleItemsByTitle = new LinkedHashMap<>();
    private TreeView<NavigationEntry> sampleTree;
    private TreeItem<NavigationEntry> selectedSampleItem;
    private boolean syncingNavigationSelection;
    private Label titleLabel;
    private Label authHint;
    private Button authSettingsButton;
    private Button loginDemoButton;
    private ToggleButton themeToggle;
    private Label placeholderLabel;

    /**
     * Creates the samples application.
     */
    public SamplesApp() {
        // Default constructor.
    }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        SamplesRuntimeSupport.initialize(themeProperty);

        sampleTree = buildSampleTree();
        topBar = buildTopBar();
        buildContentArea();

        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setLeft(sampleTree);
        root.setCenter(contentArea);

        themeProperty.addListener((obs, oldTheme, newTheme) -> applyTheme(newTheme));
        applyTheme(themeProperty.get());

        stage.setTitle("PapiflyFX Docking Samples");
        stage.setScene(new Scene(root, 1200, 800));
        stage.setOnCloseRequest(event -> disposeContentArea());
        stage.show();
    }

    private HBox buildTopBar() {
        titleLabel = new Label("PapiflyFX Docking Samples");
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        authHint = new Label("Configure auth providers in Auth Settings, then try a sign-in sample.");
        authHint.setStyle("-fx-font-size: 12px;");

        authSettingsButton = new Button("Auth Settings");
        authSettingsButton.setOnAction(event -> openSample("Settings Panel"));

        loginDemoButton = new Button("Login Demo");
        loginDemoButton.setOnAction(event -> openSample("Sign in with Google"));

        themeToggle = new ToggleButton("Light Mode");
        themeToggle.setSelected(false);
        themeToggle.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            themeProperty.set(isSelected ? Theme.light() : Theme.dark());
            themeToggle.setText(isSelected ? "Dark Mode" : "Light Mode");
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(8, titleLabel, authHint, spacer, authSettingsButton, loginDemoButton, themeToggle);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(8, 12, 8, 12));
        return bar;
    }

    private TreeView<NavigationEntry> buildSampleTree() {
        TreeView<NavigationEntry> treeView = new TreeView<>();
        treeView.setShowRoot(false);
        treeView.setRoot(buildNavigationRoot());
        treeView.setTreeViewTheme(TreeViewThemeMapper.map(themeProperty.get()));
        treeView.setCellRenderer(this::renderNavigationCell);
        treeView.setNavigationSelectablePredicate(item -> item != null && item.getValue() != null && !item.getValue().isCategory());
        treeView.setPrefWidth(190);
        treeView.setMinWidth(140);
        treeView.getSelectionModel().addListener(model -> onNavigationSelectionChanged(treeView));
        return treeView;
    }

    private TreeItem<NavigationEntry> buildNavigationRoot() {
        TreeItem<NavigationEntry> root = new TreeItem<>(new NavigationEntry("", null));
        sampleItemsByTitle.clear();
        String currentCategory = null;
        TreeItem<NavigationEntry> currentCategoryItem = null;
        for (SampleScene sample : SampleCatalog.all()) {
            if (!sample.category().equals(currentCategory)) {
                currentCategory = sample.category();
                currentCategoryItem = new TreeItem<>(new NavigationEntry(currentCategory, null));
                currentCategoryItem.setExpanded(true);
                root.addChild(currentCategoryItem);
            }
            TreeItem<NavigationEntry> sampleItem = new TreeItem<>(new NavigationEntry(sample.title(), sample));
            sampleItemsByTitle.put(sample.title(), sampleItem);
            if (currentCategoryItem != null) {
                currentCategoryItem.addChild(sampleItem);
            }
        }
        root.setExpanded(true);
        return root;
    }

    private void onNavigationSelectionChanged(TreeView<NavigationEntry> treeView) {
        if (syncingNavigationSelection) {
            return;
        }
        TreeItem<NavigationEntry> focusedItem = treeView.getSelectionModel().getFocusedItem();
        if (focusedItem == null || focusedItem.getValue() == null) {
            return;
        }
        NavigationEntry entry = focusedItem.getValue();
        if (entry.isCategory()) {
            syncingNavigationSelection = true;
            try {
                if (selectedSampleItem != null) {
                    treeView.getSelectionModel().selectOnly(selectedSampleItem);
                    treeView.getSelectionModel().setFocusedItem(selectedSampleItem);
                } else {
                    treeView.getSelectionModel().clearSelection();
                    treeView.getSelectionModel().setFocusedItem(null);
                }
            } finally {
                syncingNavigationSelection = false;
            }
            return;
        }
        showSample(focusedItem);
    }

    @Override
    public void stop() {
        disposeContentArea();
    }

    private void renderNavigationCell(
        GraphicsContext graphics,
        NavigationEntry entry,
        TreeRenderContext<NavigationEntry> context,
        CellState state
    ) {
        if (entry == null) {
            return;
        }
        double textY = state.y() + ((state.height() - context.glyphCache().getLineHeight()) * 0.5) + context.baseline();
        double baseX = Math.max(0.0, state.x() - context.iconSize() - context.indentWidth());
        if (entry.isCategory()) {
            graphics.setFill(context.theme().background());
            graphics.fillRect(0.0, state.y(), context.effectiveTextWidth(), state.height());
            graphics.setFont(Font.font(context.theme().font().getFamily(), FontWeight.BOLD, context.theme().font().getSize()));
            graphics.setFill(context.theme().connectingLineColor());
            graphics.fillText(entry.label(), baseX + 2.0, textY);
            graphics.setFont(context.theme().font());
            return;
        }
        graphics.setFont(context.theme().font());
        graphics.setFill(state.selected() ? context.theme().textColorSelected() : context.theme().textColor());
        graphics.fillText(entry.label(), baseX + 14.0, textY);
    }

    private void buildContentArea() {
        placeholderLabel = new Label("Select a sample from the navigation panel, or use Auth Settings and Login Demo to open a sign-in sample.");
        placeholderLabel.setStyle("-fx-font-size: 14px;");
        placeholderLabel.setWrapText(true);
        placeholderLabel.setMaxWidth(420);
        contentArea.getChildren().add(placeholderLabel);
    }

    private void openSample(String sampleTitle) {
        if (sampleTree == null) {
            return;
        }
        TreeItem<NavigationEntry> sampleItem = sampleItemsByTitle.get(sampleTitle);
        if (sampleItem == null) {
            return;
        }
        expandParents(sampleItem);
        syncingNavigationSelection = true;
        try {
            sampleTree.getSelectionModel().selectOnly(sampleItem);
            sampleTree.getSelectionModel().setFocusedItem(sampleItem);
        } finally {
            syncingNavigationSelection = false;
        }
        showSample(sampleItem);
    }

    private void expandParents(TreeItem<NavigationEntry> item) {
        TreeItem<NavigationEntry> current = item.getParent();
        while (current != null) {
            current.setExpanded(true);
            current = current.getParent();
        }
    }

    private void showSample(TreeItem<NavigationEntry> sampleItem) {
        if (sampleItem == null || sampleItem.getValue() == null || sampleItem.getValue().isCategory()) {
            return;
        }
        selectedSampleItem = sampleItem;
        disposeContentArea();
        Node content = sampleItem.getValue().sample().build(primaryStage, themeProperty);
        contentArea.getChildren().setAll(content);
    }

    private void applyTheme(Theme theme) {
        Theme resolved = UiCommonThemeSupport.resolvedTheme(theme);
        Color headerBg = UiCommonThemeSupport.headerBackground(resolved);
        Color canvasBg = UiCommonThemeSupport.background(resolved);
        Color textPrimary = UiCommonThemeSupport.textPrimary(resolved);
        Color textMuted = UiCommonThemeSupport.alpha(textPrimary, 0.66);
        Color accent = UiCommonThemeSupport.accent(resolved);
        Color controlBg = UiCommonThemeSupport.border(resolved);

        String headerBgCss = UiCommonThemeSupport.paintToCss(headerBg, "transparent");
        String canvasBgCss = UiCommonThemeSupport.paintToCss(canvasBg, "transparent");
        String textPrimaryCss = UiCommonThemeSupport.paintToCss(textPrimary, "transparent");
        String textMutedCss = UiCommonThemeSupport.paintToCss(textMuted, "transparent");
        String accentCss = UiCommonThemeSupport.paintToCss(accent, "transparent");
        String controlBgCss = UiCommonThemeSupport.paintToCss(controlBg, "transparent");

        topBar.setStyle("-fx-background-color: " + headerBgCss + ";");
        titleLabel.setStyle("-fx-text-fill: " + textPrimaryCss + "; -fx-font-size: 14px; -fx-font-weight: bold;");
        authHint.setStyle("-fx-text-fill: " + textMutedCss + "; -fx-font-size: 12px;");
        authSettingsButton.setStyle("-fx-background-color: " + controlBgCss + "; -fx-text-fill: " + textPrimaryCss + ";");
        loginDemoButton.setStyle("-fx-background-color: " + accentCss + "; -fx-text-fill: " + textPrimaryCss + ";");
        themeToggle.setStyle("-fx-background-color: " + accentCss + "; -fx-text-fill: " + textPrimaryCss + ";");
        contentArea.setStyle("-fx-background-color: " + canvasBgCss + ";");
        if (placeholderLabel != null) {
            placeholderLabel.setStyle("-fx-text-fill: " + textMutedCss + "; -fx-font-size: 14px;");
        }

        TreeViewTheme navTheme = TreeViewThemeMapper.map(resolved);
        sampleTree.setTreeViewTheme(navTheme);
    }

    private void disposeContentArea() {
        if (contentArea.getChildren().isEmpty()) {
            return;
        }
        Set<DockManager> disposedManagers = new HashSet<>();
        List<Node> children = new ArrayList<>(contentArea.getChildren());
        for (Node child : children) {
            disposeNodeTree(child, disposedManagers);
        }
        contentArea.getChildren().clear();
    }

    private void disposeNodeTree(Node node, Set<DockManager> disposedManagers) {
        if (node == null) {
            return;
        }
        Object managerValue = node.getProperties().get(DockManager.ROOT_PANE_MANAGER_PROPERTY);
        if (managerValue instanceof DockManager dockManager) {
            if (disposedManagers.add(dockManager)) {
                dockManager.dispose();
            }
            return;
        }
        if (node instanceof DisposableContent disposable) {
            disposable.dispose();
        }
        if (node instanceof Parent parent) {
            List<Node> children = new ArrayList<>(parent.getChildrenUnmodifiable());
            for (Node child : children) {
                disposeNodeTree(child, disposedManagers);
            }
        }
    }

    /**
     * Launches the samples application.
     *
     * @param args launcher arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

    private record NavigationEntry(String label, SampleScene sample) {
        private boolean isCategory() {
            return sample == null;
        }
    }
}
