package org.metalib.papifly.fx.docks.ribbon;

import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.ui.UiCommonPalette;
import org.metalib.papifly.fx.ui.UiCommonThemeSupport;

/**
 * Internal theme helper for the ribbon shell.
 */
final class RibbonThemeSupport {

    private RibbonThemeSupport() {
    }

    static String themeVariables(Theme theme) {
        Theme resolved = UiCommonThemeSupport.resolvedTheme(theme);
        UiCommonPalette palette = new UiCommonPalette(
            UiCommonThemeSupport.headerBackground(resolved),
            UiCommonThemeSupport.border(resolved),
            UiCommonThemeSupport.textPrimary(resolved),
            UiCommonThemeSupport.alpha(UiCommonThemeSupport.textPrimary(resolved), 0.66),
            UiCommonThemeSupport.alpha(UiCommonThemeSupport.textPrimary(resolved), 0.50),
            UiCommonThemeSupport.headerBackgroundActive(resolved),
            UiCommonThemeSupport.hover(resolved),
            UiCommonThemeSupport.pressed(resolved),
            UiCommonThemeSupport.accent(resolved),
            UiCommonThemeSupport.accent(resolved),
            UiCommonThemeSupport.success(resolved),
            UiCommonThemeSupport.warning(resolved),
            UiCommonThemeSupport.danger(resolved),
            UiCommonThemeSupport.dropHint(resolved),
            UiCommonThemeSupport.shadowColor(resolved, 0.30, 0.18)
        );
        return UiCommonThemeSupport.themeVariables(palette) + ribbonVariables(resolved);
    }

    private static String ribbonVariables(Theme theme) {
        return """
            -fx-ribbon-accent: %s;
            -fx-ribbon-background: %s;
            -fx-ribbon-background-subtle: %s;
            -fx-ribbon-tab-hover: %s;
            -fx-ribbon-tab-selected: %s;
            -fx-ribbon-contextual-background: %s;
            -fx-ribbon-group-label-color: %s;
            -fx-ribbon-shadow: %s;
            """.formatted(
            UiCommonThemeSupport.paintToCss(UiCommonThemeSupport.accent(theme), "transparent"),
            UiCommonThemeSupport.paintToCss(UiCommonThemeSupport.headerBackground(theme), "transparent"),
            UiCommonThemeSupport.paintToCss(UiCommonThemeSupport.headerBackgroundActive(theme), "transparent"),
            UiCommonThemeSupport.paintToCss(UiCommonThemeSupport.hover(theme), "transparent"),
            UiCommonThemeSupport.paintToCss(UiCommonThemeSupport.headerBackgroundActive(theme), "transparent"),
            UiCommonThemeSupport.paintToCss(UiCommonThemeSupport.alpha(UiCommonThemeSupport.accent(theme), 0.16), "transparent"),
            UiCommonThemeSupport.paintToCss(UiCommonThemeSupport.alpha(UiCommonThemeSupport.textPrimary(theme), 0.66), "transparent"),
            UiCommonThemeSupport.paintToCss(UiCommonThemeSupport.shadowColor(theme, 0.30, 0.18), "transparent")
        );
    }
}
