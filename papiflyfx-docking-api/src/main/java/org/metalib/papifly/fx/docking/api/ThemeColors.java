package org.metalib.papifly.fx.docking.api;

import javafx.scene.paint.Paint;

/**
 * Color and paint values used by docking-aware UI components.
 */
public record ThemeColors(
    Paint background,
    Paint headerBackground,
    Paint headerBackgroundActive,
    Paint accentColor,
    Paint textColor,
    Paint textColorActive,
    Paint borderColor,
    Paint dividerColor,
    Paint dropHintColor,
    Paint buttonHoverBackground,
    Paint buttonPressedBackground,
    Paint minimizedBarBackground
) {
}
