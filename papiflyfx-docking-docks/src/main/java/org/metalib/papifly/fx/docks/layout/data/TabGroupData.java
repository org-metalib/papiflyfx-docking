package org.metalib.papifly.fx.docks.layout.data;

import java.util.List;

/**
 * DTO representing a tab group container in the layout.
 *
 * @param id tab group identifier
 * @param tabs tabs contained by the group
 * @param activeTabIndex index of the active tab
 */
public record TabGroupData(
    String id,
    List<LeafData> tabs,
    int activeTabIndex
) implements LayoutNode {

    /**
     * Creates a TabGroupData with the given tabs, first tab active.
     *
     * @param id tab group identifier
     * @param tabs tabs contained by the group
     * @return tab group data with the first tab active
     */
    public static TabGroupData of(String id, List<LeafData> tabs) {
        return new TabGroupData(id, tabs, 0);
    }

    /**
     * Creates a TabGroupData with specified active tab.
     *
     * @param id tab group identifier
     * @param tabs tabs contained by the group
     * @param activeTabIndex index of the active tab
     * @return tab group data with explicit active tab index
     */
    public static TabGroupData of(String id, List<LeafData> tabs, int activeTabIndex) {
        return new TabGroupData(id, tabs, activeTabIndex);
    }

    /**
     * Creates a TabGroupData from varargs tabs.
     *
     * @param id tab group identifier
     * @param tabs tabs contained by the group
     * @return tab group data with the first tab active
     */
    public static TabGroupData of(String id, LeafData... tabs) {
        return new TabGroupData(id, List.of(tabs), 0);
    }
}
