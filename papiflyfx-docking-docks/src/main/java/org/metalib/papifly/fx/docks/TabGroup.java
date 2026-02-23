package org.metalib.papifly.fx.docks;

import javafx.beans.property.ObjectProperty;
import org.metalib.papifly.fx.docks.core.DockLeaf;
import org.metalib.papifly.fx.docks.core.DockTabGroup;
import org.metalib.papifly.fx.docking.api.Theme;

/**
 * Fluent builder for creating DockTabGroup instances.
 */
public final class TabGroup {

    /**
     * Utility class.
     */
    private TabGroup() {
    }

    /**
     * Creates a tab group with the given leaves.
     *
     * @param themeProperty theme property
     * @param leaves leaves to add
     * @return created tab group
     */
    public static DockTabGroup of(ObjectProperty<Theme> themeProperty, DockLeaf... leaves) {
        DockTabGroup tabGroup = new DockTabGroup(themeProperty);
        for (DockLeaf leaf : leaves) {
            tabGroup.addLeaf(leaf);
        }
        return tabGroup;
    }

    /**
     * Creates an empty tab group.
     *
     * @param themeProperty theme property
     * @return created empty tab group
     */
    public static DockTabGroup empty(ObjectProperty<Theme> themeProperty) {
        return new DockTabGroup(themeProperty);
    }
}
