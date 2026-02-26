package org.metalib.papifly.fx.samples;

import javafx.application.Application;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.GraphicsContext;
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
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.samples.catalog.SampleCatalog;
import org.metalib.papifly.fx.tree.api.CellState;
import org.metalib.papifly.fx.tree.api.TreeItem;
import org.metalib.papifly.fx.tree.api.TreeView;
import org.metalib.papifly.fx.tree.render.TreeRenderContext;
import org.metalib.papifly.fx.tree.theme.TreeViewTheme;

/**
 * Main application shell for the PapiflyFX Docking Samples.
 *
 * <p>Layout: top bar (title + theme toggle) | left navigation tree | center content area.</p>
 */
public class SamplesApp extends Application {

    private final ObjectProperty<Theme> themeProperty = new SimpleObjectProperty<>(Theme.dark());
    private static final TreeViewTheme NAVIGATION_THEME = new TreeViewTheme(
        Color.web("#252526"),
        Color.web("#252526"),
        Color.web("#252526"),
        Color.web("#094771"),
        Color.web("#3a3d41"),
        Color.web("#007acc"),
        Color.web("#2a2d2e"),
        Color.web("#cccccc"),
        Color.web("#ffffff"),
        Color.web("#6f6f6f"),
        Color.web("#3f3f46"),
        Color.rgb(255, 255, 255, 0.08),
        Color.rgb(255, 255, 255, 0.32),
        Color.rgb(255, 255, 255, 0.46),
        Color.rgb(255, 255, 255, 0.58),
        Font.font("System", 13),
        24.0,
        16.0,
        0.0
    );
    private Stage primaryStage;
    private final StackPane contentArea = new StackPane();
    private TreeItem<NavigationEntry> selectedSampleItem;
    private boolean syncingNavigationSelection;

    /**
     * Creates the samples application.
     */
    public SamplesApp() {
        // Default constructor.
    }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;

        HBox topBar = buildTopBar();
        TreeView<NavigationEntry> sampleTree = buildSampleTree();
        buildContentArea();

        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setLeft(sampleTree);
        root.setCenter(contentArea);

        stage.setTitle("PapiflyFX Docking Samples");
        stage.setScene(new Scene(root, 1200, 800));
        stage.show();
    }

    private HBox buildTopBar() {
        Label titleLabel = new Label("PapiflyFX Docking Samples");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

        ToggleButton themeToggle = new ToggleButton("Light Mode");
        themeToggle.setStyle("-fx-background-color: #0e639c; -fx-text-fill: white;");
        themeToggle.setSelected(false);
        themeToggle.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            themeProperty.set(isSelected ? Theme.light() : Theme.dark());
            themeToggle.setText(isSelected ? "Dark Mode" : "Light Mode");
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topBar = new HBox(8, titleLabel, spacer, themeToggle);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(8, 12, 8, 12));
        topBar.setStyle("-fx-background-color: #3c3c3c;");
        return topBar;
    }

    private TreeView<NavigationEntry> buildSampleTree() {
        TreeView<NavigationEntry> treeView = new TreeView<>();
        treeView.setShowRoot(false);
        treeView.setRoot(buildNavigationRoot());
        treeView.setTreeViewTheme(NAVIGATION_THEME);
        treeView.setCellRenderer(this::renderNavigationCell);
        treeView.setPrefWidth(190);
        treeView.setMinWidth(140);
        treeView.getSelectionModel().addListener(model -> onNavigationSelectionChanged(treeView));
        return treeView;
    }

    private TreeItem<NavigationEntry> buildNavigationRoot() {
        TreeItem<NavigationEntry> root = new TreeItem<>(new NavigationEntry("", null));
        String currentCategory = null;
        for (SampleScene sample : SampleCatalog.all()) {
            if (!sample.category().equals(currentCategory)) {
                currentCategory = sample.category();
                root.addChild(new TreeItem<>(new NavigationEntry(currentCategory, null)));
            }
            root.addChild(new TreeItem<>(new NavigationEntry(sample.title(), sample)));
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
        selectedSampleItem = focusedItem;
        Node content = entry.sample().build(primaryStage, themeProperty);
        contentArea.getChildren().setAll(content);
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
            graphics.setFill(Color.web("#1e1e1e"));
            graphics.fillRect(0.0, state.y(), context.effectiveTextWidth(), state.height());
            graphics.setFont(Font.font(context.theme().font().getFamily(), FontWeight.BOLD, context.theme().font().getSize()));
            graphics.setFill(Color.web("#888888"));
            graphics.fillText(entry.label(), baseX + 2.0, textY);
            graphics.setFont(context.theme().font());
            return;
        }
        graphics.setFont(context.theme().font());
        graphics.setFill(state.selected() ? context.theme().textColorSelected() : context.theme().textColor());
        graphics.fillText(entry.label(), baseX + 14.0, textY);
    }

    private void buildContentArea() {
        Label placeholder = new Label("Select a sample from the navigation panel");
        placeholder.setStyle("-fx-text-fill: #888888; -fx-font-size: 14px;");
        contentArea.getChildren().add(placeholder);
        contentArea.setStyle("-fx-background-color: #1e1e1e;");
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
