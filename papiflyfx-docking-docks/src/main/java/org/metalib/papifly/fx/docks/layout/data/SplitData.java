package org.metalib.papifly.fx.docks.layout.data;

import javafx.geometry.Orientation;

/**
 * DTO representing a split container in the layout.
 *
 * @param id split identifier
 * @param orientation split orientation
 * @param dividerPosition divider position ratio between first and second child
 * @param first first child layout node
 * @param second second child layout node
 */
public record SplitData(
    String id,
    Orientation orientation,
    double dividerPosition,
    LayoutNode first,
    LayoutNode second
) implements LayoutNode {

    /**
     * Creates a horizontal split with default divider position.
     *
     * @param id split identifier
     * @param first first child layout node
     * @param second second child layout node
     * @return horizontal split data with a 0.5 divider
     */
    public static SplitData horizontal(String id, LayoutNode first, LayoutNode second) {
        return new SplitData(id, Orientation.HORIZONTAL, 0.5, first, second);
    }

    /**
     * Creates a horizontal split with custom divider position.
     *
     * @param id split identifier
     * @param dividerPosition divider position ratio between first and second child
     * @param first first child layout node
     * @param second second child layout node
     * @return horizontal split data
     */
    public static SplitData horizontal(String id, double dividerPosition, LayoutNode first, LayoutNode second) {
        return new SplitData(id, Orientation.HORIZONTAL, dividerPosition, first, second);
    }

    /**
     * Creates a vertical split with default divider position.
     *
     * @param id split identifier
     * @param first first child layout node
     * @param second second child layout node
     * @return vertical split data with a 0.5 divider
     */
    public static SplitData vertical(String id, LayoutNode first, LayoutNode second) {
        return new SplitData(id, Orientation.VERTICAL, 0.5, first, second);
    }

    /**
     * Creates a vertical split with custom divider position.
     *
     * @param id split identifier
     * @param dividerPosition divider position ratio between first and second child
     * @param first first child layout node
     * @param second second child layout node
     * @return vertical split data
     */
    public static SplitData vertical(String id, double dividerPosition, LayoutNode first, LayoutNode second) {
        return new SplitData(id, Orientation.VERTICAL, dividerPosition, first, second);
    }
}
