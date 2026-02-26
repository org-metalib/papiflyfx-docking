package org.metalib.papifly.fx.tree.api;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import org.metalib.papifly.fx.docking.api.DisposableContent;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.tree.controller.TreeDragDropController;
import org.metalib.papifly.fx.tree.controller.TreeEditController;
import org.metalib.papifly.fx.tree.controller.TreeInputController;
import org.metalib.papifly.fx.tree.controller.TreePointerController;
import org.metalib.papifly.fx.tree.model.FlattenedTree;
import org.metalib.papifly.fx.tree.model.TreeExpansionModel;
import org.metalib.papifly.fx.tree.model.TreeSelectionModel;
import org.metalib.papifly.fx.tree.render.TreeViewport;
import org.metalib.papifly.fx.tree.theme.TreeViewTheme;
import org.metalib.papifly.fx.tree.theme.TreeViewThemeMapper;
import org.metalib.papifly.fx.tree.util.TreeStateCodec;
import org.metalib.papifly.fx.tree.util.TreeViewStateData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class TreeView<T> extends StackPane implements DisposableContent {

    private final ObjectProperty<TreeItem<T>> root = new SimpleObjectProperty<>(this, "root");
    private final TreeSelectionModel<T> selectionModel = new TreeSelectionModel<>();
    private final TreeExpansionModel<T> expansionModel = new TreeExpansionModel<>();
    private final FlattenedTree<T> flattenedTree = new FlattenedTree<>(expansionModel);
    private final TreeViewport<T> viewport = new TreeViewport<>(flattenedTree, selectionModel);
    private final TreeEditController<T> editController = new TreeEditController<>(viewport, viewport::markDirty);

    private final TreeInputController<T> inputController = new TreeInputController<>(
        flattenedTree,
        selectionModel,
        expansionModel,
        viewport,
        editController
    );
    private final TreePointerController<T> pointerController = new TreePointerController<>(
        flattenedTree,
        selectionModel,
        expansionModel,
        viewport,
        editController
    );
    private final TreeDragDropController<T> dragDropController = new TreeDragDropController<>(
        this,
        viewport,
        flattenedTree,
        selectionModel,
        expansionModel
    );

    private ObjectProperty<Theme> boundThemeProperty;
    private ChangeListener<Theme> themeChangeListener;
    private BiConsumer<TreeItem<T>, String> editCommitHandler = (item, text) -> {};
    private Predicate<TreeItem<T>> navigationSelectablePredicate = item -> true;
    private TreeViewStateData pendingState;
    private boolean disposed;

    public TreeView() {
        setMinSize(0, 0);
        setPrefSize(480, 360);
        setFocusTraversable(true);

        editController.setCommitHandler(this::onEditCommit);
        getChildren().addAll(viewport, dragDropController.overlayCanvas(), editController.editorNode());

        installHandlers();
        inputController.setNavigationSelectablePredicate(navigationSelectablePredicate);
        root.addListener((obs, oldRoot, newRoot) -> onRootChanged(newRoot));
        selectionModel.addListener(model -> viewport.markDirty());
        expansionModel.addListener((item, expanded) -> viewport.markDirty());
        setTreeViewTheme(TreeViewTheme.dark());
    }

    public TreeItem<T> getRoot() {
        return root.get();
    }

    public void setRoot(TreeItem<T> root) {
        this.root.set(root);
    }

    public ObjectProperty<TreeItem<T>> rootProperty() {
        return root;
    }

    public TreeSelectionModel<T> getSelectionModel() {
        return selectionModel;
    }

    public TreeExpansionModel<T> getExpansionModel() {
        return expansionModel;
    }

    public FlattenedTree<T> getFlattenedTree() {
        return flattenedTree;
    }

    public boolean isShowRoot() {
        return flattenedTree.isShowRoot();
    }

    public void setShowRoot(boolean showRoot) {
        flattenedTree.setShowRoot(showRoot);
        viewport.markDirty();
    }

    public TreeViewport<T> getViewport() {
        return viewport;
    }

    public TreeEditController<T> getEditController() {
        return editController;
    }

    public TreeDragDropController<T> getDragDropController() {
        return dragDropController;
    }

    public void setCellRenderer(TreeCellRenderer<T> cellRenderer) {
        viewport.setCellRenderer(cellRenderer);
    }

    public void setIconResolver(Function<T, Image> iconResolver) {
        viewport.setIconResolver(iconResolver);
    }

    public void setEditCommitHandler(BiConsumer<TreeItem<T>, String> editCommitHandler) {
        this.editCommitHandler = editCommitHandler == null ? (item, text) -> {} : editCommitHandler;
    }

    public void setNavigationSelectablePredicate(Predicate<TreeItem<T>> navigationSelectablePredicate) {
        this.navigationSelectablePredicate = navigationSelectablePredicate == null ? item -> true : navigationSelectablePredicate;
        inputController.setNavigationSelectablePredicate(this.navigationSelectablePredicate);
    }

    public Predicate<TreeItem<T>> getNavigationSelectablePredicate() {
        return navigationSelectablePredicate;
    }

    public void bindThemeProperty(ObjectProperty<Theme> themeProperty) {
        unbindThemeProperty();
        if (themeProperty == null) {
            return;
        }
        this.boundThemeProperty = themeProperty;
        this.themeChangeListener = (obs, oldTheme, newTheme) -> applyDockingTheme(newTheme);
        themeProperty.addListener(themeChangeListener);
        applyDockingTheme(themeProperty.get());
    }

    public void unbindThemeProperty() {
        if (boundThemeProperty != null && themeChangeListener != null) {
            boundThemeProperty.removeListener(themeChangeListener);
        }
        boundThemeProperty = null;
        themeChangeListener = null;
    }

    public void setTreeViewTheme(TreeViewTheme theme) {
        viewport.setTheme(theme == null ? TreeViewTheme.dark() : theme);
    }

    public TreeViewTheme getTreeViewTheme() {
        return viewport.getTheme();
    }

    public Map<String, Object> captureState() {
        return TreeStateCodec.toMap(captureStateData());
    }

    public TreeViewStateData captureStateData() {
        List<List<Integer>> expandedPaths = expansionModel.getExpandedItems().stream()
            .map(this::pathOf)
            .filter(path -> !path.isEmpty() || getRoot() != null)
            .sorted(Comparator.comparingInt(List::size))
            .toList();
        List<List<Integer>> selectedPaths = selectionModel.getSelectedItems().stream()
            .map(this::pathOf)
            .toList();
        List<Integer> focusedPath = pathOf(selectionModel.getFocusedItem());
        return new TreeViewStateData(
            expandedPaths,
            selectedPaths,
            focusedPath,
            viewport.getScrollOffset(),
            viewport.getHorizontalScrollOffset()
        );
    }

    public void applyState(Map<String, Object> state) {
        applyState(TreeStateCodec.fromMap(state));
    }

    public void applyState(TreeViewStateData state) {
        TreeViewStateData safeState = state == null ? TreeViewStateData.empty() : state;
        if (getRoot() == null) {
            pendingState = safeState;
            return;
        }
        pendingState = null;
        expansionModel.clear();
        for (List<Integer> path : safeState.expandedPaths()) {
            TreeItem<T> item = resolvePath(path);
            if (item != null) {
                expansionModel.setExpanded(item, true);
            }
        }
        if (safeState.expandedPaths().isEmpty() && getRoot() != null) {
            expansionModel.setExpanded(getRoot(), true);
        }

        selectionModel.clearSelection();
        for (List<Integer> path : safeState.selectedPaths()) {
            TreeItem<T> item = resolvePath(path);
            if (item != null) {
                selectionModel.addSelection(item);
            }
        }

        TreeItem<T> focused = resolvePath(safeState.focusedPath());
        if (focused != null) {
            selectionModel.setFocusedItem(focused);
            if (!selectionModel.isSelected(focused)) {
                selectionModel.addSelection(focused);
            }
        }

        viewport.setScrollOffset(safeState.scrollOffset());
        viewport.setHorizontalScrollOffset(safeState.horizontalScrollOffset());
        viewport.markDirty();
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        editController.relayout();
    }

    @Override
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        unbindThemeProperty();
        editController.dispose();
        pointerController.dispose();
        dragDropController.dispose();
        setOnKeyPressed(null);
        viewport.setOnMousePressed(null);
        viewport.setOnMouseDragged(null);
        viewport.setOnMouseReleased(null);
        viewport.setOnMouseMoved(null);
        viewport.setOnMouseExited(null);
        viewport.setOnScroll(null);
        viewport.setOnDragDetected(null);
        setOnDragOver(null);
        setOnDragDropped(null);
        setOnDragExited(null);
        setOnDragDone(null);
    }

    public TreeItem<T> resolvePath(List<Integer> path) {
        TreeItem<T> current = getRoot();
        if (current == null) {
            return null;
        }
        if (path == null || path.isEmpty()) {
            return current;
        }
        for (Integer segment : path) {
            if (segment == null) {
                return null;
            }
            int index = segment;
            if (index < 0 || index >= current.getChildCount()) {
                return null;
            }
            current = current.getChildren().get(index);
        }
        return current;
    }

    public List<Integer> pathOf(TreeItem<T> item) {
        if (item == null || getRoot() == null) {
            return List.of();
        }
        List<Integer> path = new ArrayList<>();
        TreeItem<T> current = item;
        while (current != null && current.getParent() != null) {
            TreeItem<T> parent = current.getParent();
            int index = parent.indexOfChild(current);
            if (index < 0) {
                return List.of();
            }
            path.add(index);
            current = parent;
        }
        if (current != getRoot()) {
            return List.of();
        }
        Collections.reverse(path);
        return List.copyOf(path);
    }

    private void installHandlers() {
        setOnKeyPressed(event -> inputController.handleKeyPressed(event));
        viewport.setOnMousePressed(event -> {
            requestFocus();
            pointerController.handleMousePressed(event);
        });
        viewport.setOnMouseDragged(pointerController::handleMouseDragged);
        viewport.setOnMouseReleased(pointerController::handleMouseReleased);
        viewport.setOnMouseMoved(pointerController::handleMouseMoved);
        viewport.setOnMouseExited(pointerController::handleMouseExited);
        viewport.setOnScroll(pointerController::handleScroll);
    }

    private void onRootChanged(TreeItem<T> newRoot) {
        flattenedTree.setRoot(newRoot);
        if (newRoot != null && !expansionModel.isExpanded(newRoot)) {
            expansionModel.setExpanded(newRoot, true);
        }
        if (pendingState != null) {
            applyState(pendingState);
        } else {
            viewport.markDirty();
        }
    }

    private void onEditCommit(TreeItem<T> item, String text) {
        if (item == null) {
            return;
        }
        editCommitHandler.accept(item, text);
        viewport.markDirty();
    }

    private void applyDockingTheme(Theme theme) {
        setTreeViewTheme(TreeViewThemeMapper.map(theme));
    }
}
