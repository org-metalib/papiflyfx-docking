package org.metalib.papifly.fx.docks.layout.data;

import java.util.List;

/**
 * DTO representing a complete dock session including the layout tree,
 * floating windows, minimized leaves, and maximized state.
 *
 * @param version session schema version
 * @param layout serialized docked layout tree
 * @param floating serialized floating leaves
 * @param minimized serialized minimized leaves
 * @param maximized serialized maximized leaf state
 * @param ribbon optional ribbon state payload
 */
public record DockSessionData(
    int version,
    LayoutNode layout,
    List<FloatingLeafData> floating,
    List<MinimizedLeafData> minimized,
    MaximizedLeafData maximized,
    RibbonSessionData ribbon
) {
    public static final int CURRENT_VERSION = 2;

    /**
     * Creates a DockSessionData with current version.
     *
     * @param layout serialized docked layout tree
     * @param floating serialized floating leaves
     * @param minimized serialized minimized leaves
     * @param maximized serialized maximized leaf state
     * @return session data using {@link #CURRENT_VERSION}
     */
    public static DockSessionData of(
        LayoutNode layout,
        List<FloatingLeafData> floating,
        List<MinimizedLeafData> minimized,
        MaximizedLeafData maximized
    ) {
        return of(layout, floating, minimized, maximized, null);
    }

    /**
     * Creates a DockSessionData with current version and optional ribbon state.
     *
     * @param layout serialized docked layout tree
     * @param floating serialized floating leaves
     * @param minimized serialized minimized leaves
     * @param maximized serialized maximized leaf state
     * @param ribbon optional ribbon state payload
     * @return session data using {@link #CURRENT_VERSION}
     */
    public static DockSessionData of(
        LayoutNode layout,
        List<FloatingLeafData> floating,
        List<MinimizedLeafData> minimized,
        MaximizedLeafData maximized,
        RibbonSessionData ribbon
    ) {
        return new DockSessionData(CURRENT_VERSION, layout, floating, minimized, maximized, ribbon);
    }

    /**
     * Creates an empty session.
     *
     * @return empty session data using {@link #CURRENT_VERSION}
     */
    public static DockSessionData empty() {
        return new DockSessionData(CURRENT_VERSION, null, List.of(), List.of(), null, null);
    }

    /**
     * Returns a copy with the supplied ribbon session state.
     *
     * @param ribbon optional ribbon state payload
     * @return copied session state
     */
    public DockSessionData withRibbon(RibbonSessionData ribbon) {
        return new DockSessionData(version, layout, floating, minimized, maximized, ribbon);
    }
}
