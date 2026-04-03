package org.metalib.papifly.fx.docking.api;

import javafx.geometry.Insets;

/**
 * Shared sizing, spacing, and padding values used by docking-aware UI components.
 */
public record ThemeDimensions(
    double cornerRadius,
    double borderWidth,
    double headerHeight,
    double tabHeight,
    Insets contentPadding,
    double buttonSpacing,
    double minimizedBarHeight
) {
}
