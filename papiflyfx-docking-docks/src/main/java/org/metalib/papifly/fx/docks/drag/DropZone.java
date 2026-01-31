package org.metalib.papifly.fx.docks.drag;

/**
 * Represents the drop zone within a target element.
 * Uses edge-biased hit testing for predictable drop behavior.
 */
public enum DropZone {
    /** Drop as tab (center of target) */
    CENTER,
    /** Split and dock to the left */
    WEST,
    /** Split and dock to the right */
    EAST,
    /** Split and dock above */
    NORTH,
    /** Split and dock below */
    SOUTH,
    /** Drop on tab bar (for tab reordering) */
    TAB_BAR,
    /** No valid drop zone */
    NONE
}
