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
     * Creates a tab group with the given leaves.
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
     */
    public static DockTabGroup empty(ObjectProperty<Theme> themeProperty) {
        return new DockTabGroup(themeProperty);
    }
}
