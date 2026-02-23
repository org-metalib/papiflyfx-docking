package org.metalib.papifly.fx.docking.api;

import javafx.geometry.Insets;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Theme record for programmatic styling without CSS.
 * Contains all visual properties for the docking framework.
 *
 * @param background background paint for dock content areas
 * @param headerBackground background paint for inactive headers
 * @param headerBackgroundActive background paint for active headers
 * @param accentColor accent paint used for highlights and focus states
 * @param textColor text paint for inactive header labels
 * @param textColorActive text paint for active header labels
 * @param borderColor paint used for borders around dock elements
 * @param dividerColor paint used for split and separator dividers
 * @param dropHintColor paint used to render drag-and-drop hints
 * @param headerFont font used for dock header text
 * @param contentFont font used for dock content text
 * @param cornerRadius corner radius applied to rounded dock surfaces
 * @param borderWidth border stroke width for dock outlines
 * @param headerHeight preferred height for dock headers
 * @param tabHeight preferred height for dock tabs
 * @param contentPadding padding applied around dock content
 * @param buttonHoverBackground background paint for hovered title-bar buttons
 * @param buttonPressedBackground background paint for pressed title-bar buttons
 * @param minimizedBarBackground background paint for minimized bar containers
 * @param buttonSpacing spacing between title-bar buttons
 * @param minimizedBarHeight preferred height for minimized bar containers
 */
public record Theme(
    Paint background,
    Paint headerBackground,
    Paint headerBackgroundActive,
    Paint accentColor,
    Paint textColor,
    Paint textColorActive,
    Paint borderColor,
    Paint dividerColor,
    Paint dropHintColor,
    Font headerFont,
    Font contentFont,
    double cornerRadius,
    double borderWidth,
    double headerHeight,
    double tabHeight,
    Insets contentPadding,
    Paint buttonHoverBackground,
    Paint buttonPressedBackground,
    Paint minimizedBarBackground,
    double buttonSpacing,
    double minimizedBarHeight
) {
    /**
     * Default dark theme.
     *
     * @return the built-in dark theme preset
     */
    public static Theme dark() {
        return new Theme(
            Color.rgb(30, 30, 30),           // background
            Color.rgb(45, 45, 45),           // headerBackground
            Color.rgb(60, 60, 60),           // headerBackgroundActive
            Color.rgb(0, 122, 204),          // accentColor (blue)
            Color.rgb(200, 200, 200),        // textColor
            Color.WHITE,                      // textColorActive
            Color.rgb(60, 60, 60),           // borderColor
            Color.rgb(80, 80, 80),           // dividerColor
            Color.rgb(0, 122, 204, 0.3),     // dropHintColor
            Font.font("System", FontWeight.BOLD, 12),  // headerFont
            Font.font("System", FontWeight.NORMAL, 12), // contentFont
            4.0,                              // cornerRadius
            1.0,                              // borderWidth
            28.0,                             // headerHeight
            24.0,                             // tabHeight
            new Insets(4),                    // contentPadding
            Color.rgb(70, 70, 70),           // buttonHoverBackground
            Color.rgb(90, 90, 90),           // buttonPressedBackground
            Color.rgb(40, 40, 40),           // minimizedBarBackground
            8.0,                              // buttonSpacing
            24.0                              // minimizedBarHeight
        );
    }

    /**
     * Default light theme.
     *
     * @return the built-in light theme preset
     */
    public static Theme light() {
        return new Theme(
            Color.rgb(240, 240, 240),        // background
            Color.rgb(220, 220, 220),        // headerBackground
            Color.rgb(200, 200, 200),        // headerBackgroundActive
            Color.rgb(0, 122, 204),          // accentColor (blue)
            Color.rgb(50, 50, 50),           // textColor
            Color.BLACK,                      // textColorActive
            Color.rgb(180, 180, 180),        // borderColor
            Color.rgb(160, 160, 160),        // dividerColor
            Color.rgb(0, 122, 204, 0.3),     // dropHintColor
            Font.font("System", FontWeight.BOLD, 12),  // headerFont
            Font.font("System", FontWeight.NORMAL, 12), // contentFont
            4.0,                              // cornerRadius
            1.0,                              // borderWidth
            28.0,                             // headerHeight
            24.0,                             // tabHeight
            new Insets(4),                    // contentPadding
            Color.rgb(200, 200, 200),        // buttonHoverBackground
            Color.rgb(180, 180, 180),        // buttonPressedBackground
            Color.rgb(210, 210, 210),        // minimizedBarBackground
            8.0,                              // buttonSpacing
            24.0                              // minimizedBarHeight
        );
    }
}
