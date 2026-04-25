package org.metalib.papifly.fx.docks.ribbon;

/**
 * Host side used for rendering a {@link Ribbon} around dock content.
 */
public enum RibbonPlacement {
    /**
     * Render the ribbon above dock content.
     */
    TOP,

    /**
     * Render the ribbon to the left of dock content.
     */
    LEFT,

    /**
     * Render the ribbon to the right of dock content.
     */
    RIGHT,

    /**
     * Render the ribbon below dock content.
     */
    BOTTOM;

    /**
     * Normalizes nullable placement input to the compatibility default.
     *
     * @param placement requested placement
     * @return requested placement, or {@link #TOP} when omitted
     */
    public static RibbonPlacement normalize(RibbonPlacement placement) {
        return placement == null ? TOP : placement;
    }
}
