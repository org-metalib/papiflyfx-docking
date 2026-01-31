package org.metalib.papifly.fx.docks.layout.data;

import java.util.List;

/**
 * DTO representing a tab group container in the layout.
 */
public record TabGroupData(
    String id,
    List<LeafData> tabs,
    int activeTabIndex
) implements LayoutNode {

    /**
     * Creates a TabGroupData with the given tabs, first tab active.
     */
    public static TabGroupData of(String id, List<LeafData> tabs) {
        return new TabGroupData(id, tabs, 0);
    }

    /**
     * Creates a TabGroupData with specified active tab.
     */
    public static TabGroupData of(String id, List<LeafData> tabs, int activeTabIndex) {
        return new TabGroupData(id, tabs, activeTabIndex);
    }

    /**
     * Creates a TabGroupData from varargs tabs.
     */
    public static TabGroupData of(String id, LeafData... tabs) {
        return new TabGroupData(id, List.of(tabs), 0);
    }
}
