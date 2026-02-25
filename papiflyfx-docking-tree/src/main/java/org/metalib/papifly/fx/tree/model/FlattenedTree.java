package org.metalib.papifly.fx.tree.model;

import org.metalib.papifly.fx.tree.api.TreeItem;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class FlattenedTree<T> {

    public interface FlattenedTreeListener {
        void onFlattenedTreeChanged();
    }

    private final List<FlattenedRow<T>> rows = new ArrayList<>();
    private final Map<TreeItem<T>, Integer> indexByItem = new IdentityHashMap<>();
    private final Map<TreeItem<T>, TreeItem.TreeItemListener<T>> treeListeners = new IdentityHashMap<>();
    private final List<FlattenedTreeListener> listeners = new CopyOnWriteArrayList<>();

    private TreeItem<T> root;
    private TreeExpansionModel<T> expansionModel;

    public FlattenedTree() {
        this(new TreeExpansionModel<>());
    }

    public FlattenedTree(TreeExpansionModel<T> expansionModel) {
        this.expansionModel = expansionModel == null ? new TreeExpansionModel<>() : expansionModel;
        this.expansionModel.addListener((item, expanded) -> rebuild());
    }

    public TreeItem<T> getRoot() {
        return root;
    }

    public void setRoot(TreeItem<T> root) {
        if (this.root == root) {
            return;
        }
        detachListeners();
        this.root = root;
        if (root != null) {
            attachListeners(root);
        }
        rebuild();
    }

    public TreeExpansionModel<T> getExpansionModel() {
        return expansionModel;
    }

    public void setExpansionModel(TreeExpansionModel<T> expansionModel) {
        this.expansionModel = expansionModel == null ? new TreeExpansionModel<>() : expansionModel;
        this.expansionModel.addListener((item, expanded) -> rebuild());
        rebuild();
    }

    public List<FlattenedRow<T>> rows() {
        return List.copyOf(rows);
    }

    public List<TreeItem<T>> visibleItems() {
        List<TreeItem<T>> items = new ArrayList<>(rows.size());
        for (FlattenedRow<T> row : rows) {
            items.add(row.item());
        }
        return items;
    }

    public int size() {
        return rows.size();
    }

    public FlattenedRow<T> getRow(int index) {
        return rows.get(index);
    }

    public TreeItem<T> getItem(int index) {
        return rows.get(index).item();
    }

    public int depthOf(TreeItem<T> item) {
        Integer index = indexByItem.get(item);
        if (index == null) {
            return -1;
        }
        return rows.get(index).depth();
    }

    public int indexOf(TreeItem<T> item) {
        Integer index = indexByItem.get(item);
        return index == null ? -1 : index;
    }

    public void addListener(FlattenedTreeListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(FlattenedTreeListener listener) {
        listeners.remove(listener);
    }

    public void rebuild() {
        rows.clear();
        indexByItem.clear();
        if (root != null) {
            flatten(root, 0);
        }
        notifyChanged();
    }

    private void flatten(TreeItem<T> item, int depth) {
        int index = rows.size();
        rows.add(new FlattenedRow<>(item, depth));
        indexByItem.put(item, index);
        if (item.isLeaf() || !expansionModel.isExpanded(item)) {
            return;
        }
        for (TreeItem<T> child : item.getChildren()) {
            flatten(child, depth + 1);
        }
    }

    private void attachListeners(TreeItem<T> node) {
        if (node == null || treeListeners.containsKey(node)) {
            return;
        }
        TreeItem.TreeItemListener<T> listener = new TreeItem.TreeItemListener<>() {
            @Override
            public void onChildAdded(TreeItem<T> parent, TreeItem<T> child, int index) {
                attachListeners(child);
                rebuild();
            }

            @Override
            public void onChildRemoved(TreeItem<T> parent, TreeItem<T> child, int index) {
                detachListener(child);
                rebuild();
            }

            @Override
            public void onExpandedChanged(TreeItem<T> item, boolean expanded) {
                expansionModel.setExpanded(item, expanded);
                rebuild();
            }
        };
        node.addListener(listener);
        treeListeners.put(node, listener);
        for (TreeItem<T> child : node.getChildren()) {
            attachListeners(child);
        }
    }

    private void detachListeners() {
        List<TreeItem<T>> nodes = new ArrayList<>(treeListeners.keySet());
        for (TreeItem<T> node : nodes) {
            detachListener(node);
        }
        treeListeners.clear();
    }

    private void detachListener(TreeItem<T> node) {
        TreeItem.TreeItemListener<T> listener = treeListeners.remove(node);
        if (listener != null) {
            node.removeListener(listener);
        }
        for (TreeItem<T> child : node.getChildren()) {
            detachListener(child);
        }
    }

    private void notifyChanged() {
        for (FlattenedTreeListener listener : listeners) {
            listener.onFlattenedTreeChanged();
        }
    }
}
