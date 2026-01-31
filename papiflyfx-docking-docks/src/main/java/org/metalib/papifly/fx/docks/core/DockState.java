package org.metalib.papifly.fx.docks.core;

/**
 * Represents the state of a dock element.
 */
public enum DockState {
    /** Element is docked within the main layout */
    DOCKED,
    /** Element is floating in its own window */
    FLOATING,
    /** Element is minimized/hidden */
    MINIMIZED,
    /** Element is maximized to fill the entire dock area */
    MAXIMIZED
}
