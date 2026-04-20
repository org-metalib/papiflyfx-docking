package org.metalib.papifly.fx.docks.ribbon;

/**
 * Presentation modes used by adaptive ribbon groups.
 */
enum RibbonGroupSizeMode {
    LARGE,
    MEDIUM,
    SMALL,
    COLLAPSED;

    RibbonGroupSizeMode smaller() {
        return switch (this) {
            case LARGE -> MEDIUM;
            case MEDIUM -> SMALL;
            case SMALL -> COLLAPSED;
            case COLLAPSED -> COLLAPSED;
        };
    }
}
