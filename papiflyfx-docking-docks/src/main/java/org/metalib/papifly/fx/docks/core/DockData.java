package org.metalib.papifly.fx.docks.core;

import javafx.scene.Node;

/**
 * Metadata record for docking elements.
 * Contains identification and state information.
 *
 * @param id stable element identifier
 * @param title display title shown in the tab header
 * @param icon optional icon shown in the tab header
 * @param state current docking state for the element
 */
public record DockData(
    String id,
    String title,
    Node icon,
    DockState state
) {
    /**
     * Creates a DockData with just an ID and title.
     *
     * @param id stable element identifier
     * @param title display title shown in the tab header
     * @return dock metadata with default docked state and no icon
     */
    public static DockData of(String id, String title) {
        return new DockData(id, title, null, DockState.DOCKED);
    }

    /**
     * Creates a DockData with ID, title, and icon.
     *
     * @param id stable element identifier
     * @param title display title shown in the tab header
     * @param icon optional icon shown in the tab header
     * @return dock metadata with default docked state
     */
    public static DockData of(String id, String title, Node icon) {
        return new DockData(id, title, icon, DockState.DOCKED);
    }

    /**
     * Returns a copy with a new title.
     *
     * @param newTitle replacement title
     * @return a new metadata instance with updated title
     */
    public DockData withTitle(String newTitle) {
        return new DockData(id, newTitle, icon, state);
    }

    /**
     * Returns a copy with a new icon.
     *
     * @param newIcon replacement icon
     * @return a new metadata instance with updated icon
     */
    public DockData withIcon(Node newIcon) {
        return new DockData(id, title, newIcon, state);
    }

    /**
     * Returns a copy with a new state.
     *
     * @param newState replacement docking state
     * @return a new metadata instance with updated state
     */
    public DockData withState(DockState newState) {
        return new DockData(id, title, icon, newState);
    }
}
