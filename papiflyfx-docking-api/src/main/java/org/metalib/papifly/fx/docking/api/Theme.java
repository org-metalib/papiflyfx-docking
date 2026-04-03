package org.metalib.papifly.fx.docking.api;

import javafx.geometry.Insets;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.Objects;

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
     * Creates a theme from grouped color, font, and dimension values.
     *
     * @param colors grouped paint values
     * @param fonts grouped font values
     * @param dimensions grouped sizing values
     * @return composed theme
     */
    public static Theme of(ThemeColors colors, ThemeFonts fonts, ThemeDimensions dimensions) {
        Objects.requireNonNull(colors, "colors");
        Objects.requireNonNull(fonts, "fonts");
        Objects.requireNonNull(dimensions, "dimensions");
        return new Theme(
            colors.background(),
            colors.headerBackground(),
            colors.headerBackgroundActive(),
            colors.accentColor(),
            colors.textColor(),
            colors.textColorActive(),
            colors.borderColor(),
            colors.dividerColor(),
            colors.dropHintColor(),
            fonts.headerFont(),
            fonts.contentFont(),
            dimensions.cornerRadius(),
            dimensions.borderWidth(),
            dimensions.headerHeight(),
            dimensions.tabHeight(),
            dimensions.contentPadding(),
            colors.buttonHoverBackground(),
            colors.buttonPressedBackground(),
            colors.minimizedBarBackground(),
            dimensions.buttonSpacing(),
            dimensions.minimizedBarHeight()
        );
    }

    /**
     * Returns the grouped theme colors.
     *
     * @return grouped theme paints
     */
    public ThemeColors colors() {
        return new ThemeColors(
            background,
            headerBackground,
            headerBackgroundActive,
            accentColor,
            textColor,
            textColorActive,
            borderColor,
            dividerColor,
            dropHintColor,
            buttonHoverBackground,
            buttonPressedBackground,
            minimizedBarBackground
        );
    }

    /**
     * Returns the grouped theme fonts.
     *
     * @return grouped theme fonts
     */
    public ThemeFonts fonts() {
        return new ThemeFonts(headerFont, contentFont);
    }

    /**
     * Returns the grouped theme dimensions.
     *
     * @return grouped theme metrics
     */
    public ThemeDimensions dimensions() {
        return new ThemeDimensions(
            cornerRadius,
            borderWidth,
            headerHeight,
            tabHeight,
            contentPadding,
            buttonSpacing,
            minimizedBarHeight
        );
    }

    /**
     * Default dark theme.
     *
     * @return the built-in dark theme preset
     */
    public static Theme dark() {
        return Theme.of(
            new ThemeColors(
                Color.rgb(30, 30, 30),
                Color.rgb(45, 45, 45),
                Color.rgb(60, 60, 60),
                Color.rgb(0, 122, 204),
                Color.rgb(200, 200, 200),
                Color.WHITE,
                Color.rgb(60, 60, 60),
                Color.rgb(80, 80, 80),
                Color.rgb(0, 122, 204, 0.3),
                Color.rgb(70, 70, 70),
                Color.rgb(90, 90, 90),
                Color.rgb(40, 40, 40)
            ),
            new ThemeFonts(
                Font.font("System", FontWeight.BOLD, 12),
                Font.font("System", FontWeight.NORMAL, 12)
            ),
            new ThemeDimensions(
                4.0,
                1.0,
                28.0,
                24.0,
                new Insets(4),
                8.0,
                24.0
            )
        );
    }

    /**
     * Default light theme.
     *
     * @return the built-in light theme preset
     */
    public static Theme light() {
        return Theme.of(
            new ThemeColors(
                Color.rgb(240, 240, 240),
                Color.rgb(220, 220, 220),
                Color.rgb(200, 200, 200),
                Color.rgb(0, 122, 204),
                Color.rgb(50, 50, 50),
                Color.BLACK,
                Color.rgb(180, 180, 180),
                Color.rgb(160, 160, 160),
                Color.rgb(0, 122, 204, 0.3),
                Color.rgb(200, 200, 200),
                Color.rgb(180, 180, 180),
                Color.rgb(210, 210, 210)
            ),
            new ThemeFonts(
                Font.font("System", FontWeight.BOLD, 12),
                Font.font("System", FontWeight.NORMAL, 12)
            ),
            new ThemeDimensions(
                4.0,
                1.0,
                28.0,
                24.0,
                new Insets(4),
                8.0,
                24.0
            )
        );
    }
}
