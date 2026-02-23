package org.metalib.papifly.fx.code.search;

import javafx.scene.shape.SVGPath;

/**
 * Centralized SVG icon definitions for the search overlay.
 */
public final class SearchIcons {

    /**
     * Magnifier icon path.
     */
    public static final String SEARCH = "M15.5 14h-.79l-.28-.27A6.5 6.5 0 1 0 14 15.5l.27.28v.79L20 21.49 21.49 20zM6.5 11a4.5 4.5 0 1 1 9 0 4.5 4.5 0 0 1-9 0z";
    /**
     * Chevron-right icon path.
     */
    public static final String CHEVRON_RIGHT = "M8.59 16.59 13.17 12 8.59 7.41 10 6l6 6-6 6z";
    /**
     * Chevron-down icon path.
     */
    public static final String CHEVRON_DOWN = "M7.41 8.59 12 13.17l4.59-4.58L18 10l-6 6-6-6z";
    /**
     * Up arrow icon path.
     */
    public static final String ARROW_UP = "M4 12l1.41 1.41L11 7.83V20h2V7.83l5.59 5.58L20 12 12 4z";
    /**
     * Down arrow icon path.
     */
    public static final String ARROW_DOWN = "M4 12l1.41-1.41L11 16.17V4h2v12.17l5.59-5.58L20 12l-8 8z";
    /**
     * Close icon path.
     */
    public static final String CLOSE = "M18.3 5.71 12 12l6.3 6.29-1.41 1.41L10.59 13.41 4.29 19.7 2.88 18.29 9.17 12 2.88 5.71 4.29 4.3l6.3 6.29 6.29-6.29z";
    /**
     * Filter icon path.
     */
    public static final String FILTER = "M3 5h18v2l-7 7v5l-4 2v-7L3 7z";

    private static final double BASE_SIZE = 24.0;

    private SearchIcons() {
    }

    /**
     * Creates a sized icon node from SVG path data.
     *
     * @param svgPath SVG path content
     * @param size target icon size in pixels
     * @return configured SVG path node
     */
    public static SVGPath createIcon(String svgPath, double size) {
        SVGPath icon = new SVGPath();
        icon.setContent(svgPath);
        double scale = size / BASE_SIZE;
        icon.setScaleX(scale);
        icon.setScaleY(scale);
        icon.getStyleClass().add("pf-search-icon");
        return icon;
    }
}
