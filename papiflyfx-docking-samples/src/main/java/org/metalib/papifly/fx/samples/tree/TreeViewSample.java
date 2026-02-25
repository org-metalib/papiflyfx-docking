package org.metalib.papifly.fx.samples.tree;

import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;
import org.metalib.papifly.fx.docks.DockManager;
import org.metalib.papifly.fx.docks.core.DockTabGroup;
import org.metalib.papifly.fx.docks.layout.ContentStateRegistry;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.samples.SampleScene;
import org.metalib.papifly.fx.tree.api.TreeItem;
import org.metalib.papifly.fx.tree.api.TreeView;
import org.metalib.papifly.fx.tree.api.TreeViewFactory;
import org.metalib.papifly.fx.tree.api.TreeViewStateAdapter;
import org.metalib.papifly.fx.tree.model.TreeSelectionModel;

public class TreeViewSample implements SampleScene {

    private static final Image FOLDER_ICON = createIcon(Color.web("#d7ba7d"));
    private static final Image FILE_ICON = createIcon(Color.web("#9cdcfe"));

    @Override
    public String category() {
        return "Tree";
    }

    @Override
    public String title() {
        return "Tree View (10k + DnD)";
    }

    @Override
    public Node build(Stage ownerStage, ObjectProperty<Theme> themeProperty) {
        DockManager dockManager = new DockManager();
        dockManager.themeProperty().bind(themeProperty);
        dockManager.setOwnerStage(ownerStage);

        ContentStateRegistry registry = new ContentStateRegistry();
        registry.register(new TreeViewStateAdapter());
        dockManager.setContentStateRegistry(registry);
        dockManager.setContentFactory(new TreeViewFactory());

        TreeView<String> treeView = new TreeView<>();
        treeView.setRoot(createLargeTree());
        treeView.setEditCommitHandler(TreeItem::setValue);
        treeView.bindThemeProperty(themeProperty);
        treeView.getSelectionModel().setSelectionMode(TreeSelectionModel.SelectionMode.MULTIPLE);
        treeView.setIconResolver(this::resolveIcon);
        treeView.setCellRenderer(this::renderCell);
        treeView.getDragDropController().setExternalDropHandler((hint, dragboard) -> {
            if (hint == null || hint.targetItem() == null) {
                return;
            }
            if (dragboard.hasString() && !dragboard.getString().isBlank()) {
                hint.targetItem().addChild(new TreeItem<>("external-" + dragboard.getString()));
                treeView.getExpansionModel().setExpanded(hint.targetItem(), true);
            }
        });

        var leaf = dockManager.createLeaf("Tree Explorer", treeView);
        leaf.setContentFactoryId(TreeViewFactory.FACTORY_ID);
        DockTabGroup tabGroup = dockManager.createTabGroup();
        tabGroup.addLeaf(leaf);
        dockManager.setRoot(tabGroup);
        return dockManager.getRootPane();
    }

    private TreeItem<String> createLargeTree() {
        TreeItem<String> root = new TreeItem<>("workspace");
        for (int group = 0; group < 100; group++) {
            TreeItem<String> groupItem = new TreeItem<>("group-" + group);
            root.addChild(groupItem);
            for (int item = 0; item < 100; item++) {
                groupItem.addChild(new TreeItem<>("item-" + group + "-" + item));
            }
            groupItem.setExpanded(group < 3);
        }
        root.setExpanded(true);
        return root;
    }

    private Image resolveIcon(String value) {
        if (value == null) {
            return FILE_ICON;
        }
        return value.startsWith("group-") || "workspace".equals(value) ? FOLDER_ICON : FILE_ICON;
    }

    private void renderCell(GraphicsContext gc, String item, org.metalib.papifly.fx.tree.render.TreeRenderContext<String> context,
                            org.metalib.papifly.fx.tree.api.CellState state) {
        Paint color = state.selected() ? context.theme().textColorSelected() : context.theme().textColor();
        gc.setFill(color);
        String label = item;
        if (item != null && item.startsWith("group-")) {
            label = item + " (100)";
        }
        double textY = state.y() + ((state.height() - context.glyphCache().getLineHeight()) * 0.5) + context.baseline();
        gc.fillText(label, state.x(), textY);
    }

    private static Image createIcon(Color color) {
        WritableImage image = new WritableImage(14, 14);
        PixelWriter pixelWriter = image.getPixelWriter();
        for (int y = 0; y < 14; y++) {
            for (int x = 0; x < 14; x++) {
                boolean inside = x >= 1 && x <= 12 && y >= 2 && y <= 11;
                pixelWriter.setColor(x, y, inside ? color : Color.TRANSPARENT);
            }
        }
        return image;
    }
}
