package org.metalib.papifly.fx.docks;

import javafx.beans.property.ObjectProperty;
import javafx.geometry.Orientation;
import org.metalib.papifly.fx.docks.core.DockElement;
import org.metalib.papifly.fx.docks.core.DockSplitGroup;
import org.metalib.papifly.fx.docks.theme.Theme;

/**
 * Fluent builder for creating DockSplitGroup instances.
 */
public final class Split {

    /**
     * Creates a horizontal split (side-by-side).
     */
    public static DockSplitGroup horizontal(ObjectProperty<Theme> themeProperty, DockElement first, DockElement second) {
        return horizontal(themeProperty, 0.5, first, second);
    }

    /**
     * Creates a horizontal split with custom divider position.
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
     */
    public static DockSplitGroup vertical(ObjectProperty<Theme> themeProperty, DockElement first, DockElement second) {
        return vertical(themeProperty, 0.5, first, second);
    }

    /**
     * Creates a vertical split with custom divider position.
     */
    public static DockSplitGroup vertical(ObjectProperty<Theme> themeProperty, double dividerPosition, DockElement first, DockElement second) {
        DockSplitGroup split = new DockSplitGroup(Orientation.VERTICAL, themeProperty);
        split.setDividerPosition(dividerPosition);
        split.setFirst(first);
        split.setSecond(second);
        return split;
    }
}
