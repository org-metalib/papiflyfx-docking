package org.metalib.papifly.fx.docks.core;

import javafx.scene.Node;

/**
 * Metadata record for docking elements.
 * Contains identification and state information.
 */
public record DockData(
    String id,
    String title,
    Node icon,
    DockState state
) {
    /**
     * Creates a DockData with just an ID and title.
     */
    public static DockData of(String id, String title) {
        return new DockData(id, title, null, DockState.DOCKED);
    }

    /**
     * Creates a DockData with ID, title, and icon.
     */
    public static DockData of(String id, String title, Node icon) {
        return new DockData(id, title, icon, DockState.DOCKED);
    }

    /**
     * Returns a copy with a new title.
     */
    public DockData withTitle(String newTitle) {
        return new DockData(id, newTitle, icon, state);
    }

    /**
     * Returns a copy with a new icon.
     */
    public DockData withIcon(Node newIcon) {
        return new DockData(id, title, newIcon, state);
    }

    /**
     * Returns a copy with a new state.
     */
    public DockData withState(DockState newState) {
        return new DockData(id, title, icon, newState);
    }
}
