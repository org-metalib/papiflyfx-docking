package org.metalib.papifly.fx.samples.tree;

import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import org.metalib.papifly.fx.docks.DockManager;
import org.metalib.papifly.fx.docks.core.DockTabGroup;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.samples.SampleScene;
import org.metalib.papifly.fx.tree.api.TreeItem;
import org.metalib.papifly.fx.tree.api.TreeNodeInfoProvider;
import org.metalib.papifly.fx.tree.api.TreeView;
import org.metalib.papifly.fx.tree.model.TreeNodeInfoMode;

import java.util.function.Consumer;

public class TreeViewNodeInfoSample implements SampleScene {

    private static final String THEME_LISTENER_KEY = "nodeInfoThemeListener";

    @Override
    public String category() {
        return "Tree";
    }

    @Override
    public String title() {
        return "Tree View (Inline Node Info + Policies)";
    }

    @Override
    public Node build(Stage ownerStage, ObjectProperty<Theme> themeProperty) {
        DockManager dockManager = new DockManager();
        dockManager.themeProperty().bind(themeProperty);
        dockManager.setOwnerStage(ownerStage);

        TreeView<NodeInfoEntry> treeView = new TreeView<>();
        treeView.setRoot(createTree());
        treeView.setShowRoot(false);
        treeView.bindThemeProperty(themeProperty);
        treeView.setSearchTextExtractor(this::searchText);
        treeView.setNodeInfoProvider(new TreeNodeInfoProvider<>() {
            @Override
            public Node createContent(TreeItem<NodeInfoEntry> item) {
                return createInfoContent(item, themeProperty);
            }

            @Override
            public double preferredHeight(TreeItem<NodeInfoEntry> item, double availableWidth) {
                NodeInfoEntry entry = item == null ? null : item.getValue();
                if (entry == null) {
                    return 140.0;
                }
                return switch (entry.kind()) {
                    case "project" -> 170.0;
                    case "folder" -> 190.0;
                    default -> 155.0;
                };
            }
        });
        expandInitialInfo(treeView);
        Node content = withInfoModeBar(treeView);

        DockTabGroup tabGroup = dockManager.createTabGroup();
        tabGroup.addLeaf(dockManager.createLeaf("Tree Node Info", content));
        dockManager.setRoot(tabGroup);
        return dockManager.getRootPane();
    }

    private Node withInfoModeBar(TreeView<NodeInfoEntry> treeView) {
        ComboBox<TreeNodeInfoMode> modePicker = new ComboBox<>();
        modePicker.getItems().addAll(TreeNodeInfoMode.SINGLE, TreeNodeInfoMode.MULTIPLE);
        modePicker.setFocusTraversable(false);
        modePicker.setValue(treeView.getNodeInfoMode());
        modePicker.valueProperty().addListener((obs, oldMode, newMode) -> treeView.setNodeInfoMode(newMode));

        Label modeLabel = new Label("Info policy:");
        Label modeHint = new Label("SINGLE: one info open, MULTIPLE: keep others open.");
        HBox toolbar = new HBox(8.0, modeLabel, modePicker, modeHint);
        toolbar.setPadding(new Insets(6.0, 10.0, 6.0, 10.0));
        HBox.setHgrow(modeHint, Priority.ALWAYS);
        modeHint.setWrapText(true);

        BorderPane wrapper = new BorderPane(treeView);
        wrapper.setTop(toolbar);
        return wrapper;
    }

    private String searchText(NodeInfoEntry entry) {
        if (entry == null) {
            return "";
        }
        return entry.name() + " " + entry.kind() + " " + entry.owner() + " " + entry.status() + " " + entry.description();
    }

    private TreeItem<NodeInfoEntry> createTree() {
        TreeItem<NodeInfoEntry> root = new TreeItem<>(new NodeInfoEntry(
            "workspace",
            "project",
            "Primary workspace for docking modules and samples",
            "platform",
            "healthy"
        ));

        TreeItem<NodeInfoEntry> projectDocks = new TreeItem<>(new NodeInfoEntry(
            "papiflyfx-docking-docks",
            "project",
            "Core docking manager and split/tab containers",
            "ui-team",
            "stable"
        ));
        TreeItem<NodeInfoEntry> docksFolder = new TreeItem<>(new NodeInfoEntry(
            "src/main/java/org/metalib/papifly/fx/docks",
            "folder",
            "Docking runtime package with managers and layout serializers",
            "ui-team",
            "active"
        ));
        TreeItem<NodeInfoEntry> docksFile = new TreeItem<>(new NodeInfoEntry(
            "DockManager.java",
            "file",
            "Coordinates dock roots, floating windows, and persistence",
            "igor",
            "review"
        ));
        docksFolder.addChild(docksFile);
        projectDocks.addChild(docksFolder);

        TreeItem<NodeInfoEntry> projectTree = new TreeItem<>(new NodeInfoEntry(
            "papiflyfx-docking-tree",
            "project",
            "Canvas tree with inline node-info virtualization",
            "tree-team",
            "active"
        ));
        TreeItem<NodeInfoEntry> treeFolder = new TreeItem<>(new NodeInfoEntry(
            "src/main/java/org/metalib/papifly/fx/tree",
            "folder",
            "Tree API, render pipeline, controllers, and state codec",
            "tree-team",
            "active"
        ));
        TreeItem<NodeInfoEntry> treeFile = new TreeItem<>(new NodeInfoEntry(
            "TreeView.java",
            "file",
            "Assembles flattened model, viewport, inline host, and interactions",
            "igor",
            "review"
        ));
        treeFolder.addChild(treeFile);
        projectTree.addChild(treeFolder);

        root.addChild(projectDocks);
        root.addChild(projectTree);

        root.setExpanded(true);
        projectDocks.setExpanded(true);
        projectTree.setExpanded(true);
        docksFolder.setExpanded(true);
        treeFolder.setExpanded(true);
        return root;
    }

    private void expandInitialInfo(TreeView<NodeInfoEntry> treeView) {
        TreeItem<NodeInfoEntry> root = treeView.getRoot();
        if (root == null || root.getChildren().isEmpty()) {
            return;
        }
        TreeItem<NodeInfoEntry> firstProject = root.getChildren().getFirst();
        treeView.getNodeInfoModel().setExpanded(firstProject, true);
    }

    private Node createInfoContent(TreeItem<NodeInfoEntry> item, ObjectProperty<Theme> themeProperty) {
        NodeInfoEntry entry = item == null ? null : item.getValue();
        if (entry == null) {
            return null;
        }
        return switch (entry.kind()) {
            case "project" -> createRichTextInfo(entry, themeProperty);
            case "folder" -> createTableInfo(entry, themeProperty);
            default -> createCardForm(entry, themeProperty);
        };
    }

    private Node createRichTextInfo(NodeInfoEntry entry, ObjectProperty<Theme> themeProperty) {
        Text heading = new Text(entry.name() + "\n");
        heading.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
        Text body = new Text(entry.description() + "\n");
        Text emphasis = new Text("Owner: " + entry.owner() + " | Status: " + entry.status());
        emphasis.setStyle("-fx-font-style: italic;");
        TextFlow richText = new TextFlow(heading, body, emphasis);
        richText.setLineSpacing(2.0);

        Label hint = new Label("Rich text can be replaced with provider-supplied HTML/WebView in consumer apps.");
        hint.setWrapText(true);

        VBox card = cardContainer(themeProperty);
        card.getChildren().addAll(richText, hint);
        bindTheme(card, themeProperty, theme -> {
            InfoPalette palette = palette(theme);
            heading.setFill(palette.text());
            body.setFill(palette.text());
            emphasis.setFill(palette.mutedText());
            hint.setTextFill(palette.mutedText());
        });
        return card;
    }

    private Node createTableInfo(NodeInfoEntry entry, ObjectProperty<Theme> themeProperty) {
        GridPane table = new GridPane();
        table.setHgap(12.0);
        table.setVgap(6.0);
        addTableRow(table, 0, "Field", "Value", true, themeProperty);
        addTableRow(table, 1, "Name", entry.name(), false, themeProperty);
        addTableRow(table, 2, "Type", entry.kind(), false, themeProperty);
        addTableRow(table, 3, "Owner", entry.owner(), false, themeProperty);
        addTableRow(table, 4, "Status", entry.status(), false, themeProperty);
        addTableRow(table, 5, "Summary", entry.description(), false, themeProperty);
        table.setPrefHeight(150.0);
        VBox card = cardContainer(themeProperty);
        card.getChildren().add(table);
        return card;
    }

    private Node createCardForm(NodeInfoEntry entry, ObjectProperty<Theme> themeProperty) {
        GridPane form = new GridPane();
        form.setHgap(8.0);
        form.setVgap(8.0);

        Label nameKey = keyLabel("Name", false, themeProperty);
        Label ownerKey = keyLabel("Owner", false, themeProperty);
        Label statusKey = keyLabel("Status", false, themeProperty);
        Label nameValue = valueLabel(entry.name(), themeProperty);
        Label ownerValue = valueLabel(entry.owner(), themeProperty);
        Label statusValue = valueLabel(entry.status(), themeProperty);

        form.addRow(0, nameKey, nameValue);
        form.addRow(1, ownerKey, ownerValue);
        form.addRow(2, statusKey, statusValue);

        VBox card = cardContainer(themeProperty);
        card.getChildren().add(form);
        return card;
    }

    private void addTableRow(
        GridPane table,
        int row,
        String key,
        String value,
        boolean header,
        ObjectProperty<Theme> themeProperty
    ) {
        Label keyLabel = keyLabel(key, header, themeProperty);
        Label valueLabel = new Label(value);
        if (header) {
            valueLabel.setStyle("-fx-font-weight: bold;");
        }
        bindTheme(valueLabel, themeProperty, theme -> valueLabel.setTextFill(palette(theme).text()));
        valueLabel.setWrapText(true);
        GridPane.setHgrow(valueLabel, Priority.ALWAYS);
        table.addRow(row, keyLabel, valueLabel);
    }

    private Label valueLabel(String value, ObjectProperty<Theme> themeProperty) {
        Label label = new Label(value);
        label.setWrapText(true);
        label.setPadding(new Insets(4.0, 6.0, 4.0, 6.0));
        GridPane.setHgrow(label, Priority.ALWAYS);
        bindTheme(label, themeProperty, theme -> {
            InfoPalette palette = palette(theme);
            label.setTextFill(palette.text());
            label.setBackground(new Background(new BackgroundFill(palette.valueBackground(), new CornerRadii(4.0), Insets.EMPTY)));
            label.setBorder(new Border(new BorderStroke(
                palette.valueBorder(),
                BorderStrokeStyle.SOLID,
                new CornerRadii(4.0),
                BorderWidths.DEFAULT
            )));
        });
        return label;
    }

    private Label keyLabel(String value, boolean header, ObjectProperty<Theme> themeProperty) {
        Label label = new Label(value);
        if (header) {
            label.setStyle("-fx-font-weight: bold;");
        }
        bindTheme(label, themeProperty, theme -> {
            InfoPalette palette = palette(theme);
            label.setTextFill(header ? palette.text() : palette.mutedText());
        });
        return label;
    }

    private VBox cardContainer(ObjectProperty<Theme> themeProperty) {
        VBox card = new VBox(8.0);
        card.setMinHeight(0.0);
        card.setFillWidth(true);
        card.setPadding(new Insets(10.0));
        bindTheme(card, themeProperty, theme -> {
            InfoPalette palette = palette(theme);
            card.setBackground(new Background(new BackgroundFill(palette.cardBackground(), new CornerRadii(6.0), Insets.EMPTY)));
            card.setBorder(new Border(new BorderStroke(
                palette.cardBorder(),
                BorderStrokeStyle.SOLID,
                new CornerRadii(6.0),
                BorderWidths.DEFAULT
            )));
        });
        return card;
    }

    private void bindTheme(Node node, ObjectProperty<Theme> themeProperty, Consumer<Theme> applier) {
        ChangeListener<Theme> listener = (obs, oldTheme, newTheme) -> applier.accept(newTheme);
        themeProperty.addListener(new WeakChangeListener<>(listener));
        node.getProperties().put(THEME_LISTENER_KEY, listener);
        applier.accept(themeProperty.get());
    }

    private InfoPalette palette(Theme theme) {
        Theme safeTheme = theme == null ? Theme.dark() : theme;
        Color background = toColor(safeTheme.background(), Color.rgb(30, 30, 30));
        boolean dark = background.getBrightness() < 0.5;
        Color text = dark ? Color.rgb(235, 235, 235) : Color.rgb(28, 28, 28);
        Color mutedText = dark ? Color.rgb(190, 190, 190) : Color.rgb(96, 96, 96);
        Color cardBackground = dark ? Color.rgb(255, 255, 255, 0.08) : Color.rgb(0, 0, 0, 0.045);
        Color cardBorder = dark ? Color.rgb(255, 255, 255, 0.22) : Color.rgb(0, 0, 0, 0.16);
        Color valueBackground = dark ? Color.rgb(255, 255, 255, 0.13) : Color.rgb(0, 0, 0, 0.08);
        Color valueBorder = dark ? Color.rgb(255, 255, 255, 0.28) : Color.rgb(0, 0, 0, 0.18);
        return new InfoPalette(cardBackground, cardBorder, valueBackground, valueBorder, text, mutedText);
    }

    private Color toColor(Paint paint, Color fallback) {
        return paint instanceof Color color ? color : fallback;
    }

    private record NodeInfoEntry(
        String name,
        String kind,
        String description,
        String owner,
        String status
    ) {
        @Override
        public String toString() {
            return name;
        }
    }

    private record InfoPalette(
        Color cardBackground,
        Color cardBorder,
        Color valueBackground,
        Color valueBorder,
        Color text,
        Color mutedText
    ) {
    }
}
