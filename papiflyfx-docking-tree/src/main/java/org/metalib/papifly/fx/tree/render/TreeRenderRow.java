package org.metalib.papifly.fx.tree.render;

import org.metalib.papifly.fx.tree.api.TreeItem;

public record TreeRenderRow<T>(
    TreeItem<T> item,
    int rowIndex,
    int depth,
    double y,
    double height,
    boolean leaf,
    boolean expanded
) {
}
