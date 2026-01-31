package org.metalib.papifly.fx.docks.layout.data;

import java.util.List;

/**
 * DTO representing a complete dock session including the layout tree,
 * floating windows, minimized leaves, and maximized state.
 */
public record DockSessionData(
    int version,
    LayoutNode layout,
    List<FloatingLeafData> floating,
    List<MinimizedLeafData> minimized,
    MaximizedLeafData maximized
) {
    public static final int CURRENT_VERSION = 1;

    /**
     * Creates a DockSessionData with current version.
     */
    public static DockSessionData of(
        LayoutNode layout,
        List<FloatingLeafData> floating,
        List<MinimizedLeafData> minimized,
        MaximizedLeafData maximized
    ) {
        return new DockSessionData(CURRENT_VERSION, layout, floating, minimized, maximized);
    }

    /**
     * Creates an empty session.
     */
    public static DockSessionData empty() {
        return new DockSessionData(CURRENT_VERSION, null, List.of(), List.of(), null);
    }
}
