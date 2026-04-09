package org.metalib.papifly.fx.tree.theme;

import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.ui.UiMetrics;

public final class TreeViewThemeMapper {

    private static final double DARK_THRESHOLD = 0.5;

    private TreeViewThemeMapper() {}

    public static TreeViewTheme map(Theme theme) {
        if (theme == null) {
            return TreeViewTheme.dark();
        }
        boolean dark = isDark(theme.background());
        Color background = asColor(theme.background(), dark ? Color.web("#1e1e1e") : Color.web("#ffffff"));
        Color headerBackground = asColor(theme.headerBackground(), blend(background, dark ? Color.WHITE : Color.BLACK, dark ? 0.08 : 0.05));
        Color accent = asColor(theme.accentColor(), Color.web("#007acc"));
        Color textPrimary = asColor(theme.textColor(), dark ? Color.web("#d4d4d4") : Color.web("#1e1e1e"));
        Color textActive = asColor(theme.textColorActive(), dark ? Color.WHITE : Color.BLACK);
        Color border = asColor(theme.borderColor(), dark ? Color.web("#3f3f46") : Color.web("#c8c8c8"));
        Color divider = asColor(theme.dividerColor(), blend(border, textPrimary, dark ? 0.18 : 0.10));
        Color hover = asColor(theme.buttonHoverBackground(), alpha(accent, dark ? 0.12 : 0.08));
        Color pressed = asColor(theme.buttonPressedBackground(), alpha(accent, dark ? 0.20 : 0.14));
        Color scrollbarTrack = alpha(border, dark ? 0.22 : 0.12);
        Color scrollbarThumb = alpha(textPrimary, dark ? 0.32 : 0.24);
        Color scrollbarThumbHover = alpha(textPrimary, dark ? 0.46 : 0.36);
        Color scrollbarThumbActive = alpha(textPrimary, dark ? 0.58 : 0.48);
        double rowHeight = Math.max(
            UiMetrics.SPACE_6,
            snapToGrid(Math.max(UiMetrics.CONTROL_HEIGHT_COMPACT, theme.tabHeight()))
        );
        double indentWidth = Math.max(UiMetrics.SPACE_4, snapToGrid(theme.buttonSpacing() + UiMetrics.SPACE_3));
        double iconSize = Math.max(UiMetrics.SPACE_3, Math.min(UiMetrics.SPACE_4, rowHeight - UiMetrics.SPACE_2));
        return new TreeViewTheme(
            background,
            background,
            blend(background, headerBackground, dark ? 0.32 : 0.24),
            pressed,
            alpha(accent, dark ? 0.12 : 0.08),
            accent,
            hover,
            textPrimary,
            textActive,
            blend(textPrimary, divider, dark ? 0.32 : 0.24),
            divider,
            scrollbarTrack,
            scrollbarThumb,
            scrollbarThumbHover,
            scrollbarThumbActive,
            theme.contentFont(),
            rowHeight,
            indentWidth,
            iconSize
        );
    }

    static boolean isDark(Paint paint) {
        if (paint instanceof Color color) {
            return color.getBrightness() < DARK_THRESHOLD;
        }
        return true;
    }

    private static Color asColor(Paint paint, Color fallback) {
        if (paint instanceof Color color) {
            return color;
        }
        return fallback;
    }

    private static Color blend(Color base, Color mix, double weight) {
        return base.interpolate(mix, clamp(weight));
    }

    private static Color alpha(Color color, double opacity) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), clamp(opacity));
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static double snapToGrid(double value) {
        return Math.max(UiMetrics.SPACE_1, Math.rint(value / UiMetrics.SPACE_1) * UiMetrics.SPACE_1);
    }
}
