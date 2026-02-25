package org.metalib.papifly.fx.tree.model;

import org.metalib.papifly.fx.tree.api.TreeItem;

public record FlattenedRow<T>(TreeItem<T> item, int depth) {
}
