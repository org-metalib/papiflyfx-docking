package org.metalib.papifly.fx.docks;

import javafx.beans.property.ObjectProperty;
import javafx.geometry.Orientation;
import org.metalib.papifly.fx.docks.core.DockElement;
import org.metalib.papifly.fx.docks.core.DockSplitGroup;
import org.metalib.papifly.fx.docking.api.Theme;

/**
 * Fluent builder for creating DockSplitGroup instances.
 */
public final class Split {

    /**
     * Utility class.
     */
    private Split() {
    }

    /**
     * Creates a horizontal split (side-by-side).
     *
     * @param themeProperty theme property
     * @param first first child element
     * @param second second child element
     * @return horizontal split group
     */
    public static DockSplitGroup horizontal(ObjectProperty<Theme> themeProperty, DockElement first, DockElement second) {
        return horizontal(themeProperty, 0.5, first, second);
    }

    /**
     * Creates a horizontal split with custom divider position.
     *
     * @param themeProperty theme property
     * @param dividerPosition divider position ratio
     * @param first first child element
     * @param second second child element
     * @return horizontal split group
     */
    public static DockSplitGroup horizontal(ObjectProperty<Theme> themeProperty, double dividerPosition, DockElement first, DockElement second) {
        DockSplitGroup split = new DockSplitGroup(Orientation.HORIZONTAL, themeProperty);
        split.setDividerPosition(dividerPosition);
        split.setFirst(first);
        split.setSecond(second);
        return split;
    }

    /**
     * Creates a vertical split (top-bottom).
     *
     * @param themeProperty theme property
     * @param first first child element
     * @param second second child element
     * @return vertical split group
     */
    public static DockSplitGroup vertical(ObjectProperty<Theme> themeProperty, DockElement first, DockElement second) {
        return vertical(themeProperty, 0.5, first, second);
    }

    /**
     * Creates a vertical split with custom divider position.
     *
     * @param themeProperty theme property
     * @param dividerPosition divider position ratio
     * @param first first child element
     * @param second second child element
     * @return vertical split group
     */
    public static DockSplitGroup vertical(ObjectProperty<Theme> themeProperty, double dividerPosition, DockElement first, DockElement second) {
        DockSplitGroup split = new DockSplitGroup(Orientation.VERTICAL, themeProperty);
        split.setDividerPosition(dividerPosition);
        split.setFirst(first);
        split.setSecond(second);
        return split;
    }
}
