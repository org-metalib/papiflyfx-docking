package org.metalib.papifly.fx.github.ui.theme;

import javafx.geometry.Insets;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import org.metalib.papifly.fx.docking.api.Theme;

public final class GitHubToolbarThemeMapper {

    private static final double DARK_THRESHOLD = 0.5;

    private GitHubToolbarThemeMapper() {
    }

    public static GitHubToolbarTheme map(Theme theme) {
        Theme resolved = theme == null ? Theme.dark() : theme;
        boolean dark = isDark(resolved.background());

        Color background = asColor(resolved.background(), dark ? Color.web("#1e1e1e") : Color.web("#f0f0f0"));
        Color toolbarBackground = asColor(resolved.headerBackground(), dark ? Color.web("#2d2d2d") : Color.web("#dcdcdc"));
        Color activeBackground = asColor(resolved.headerBackgroundActive(),
            blend(toolbarBackground, asColor(resolved.accentColor(), Color.web("#007acc")), dark ? 0.08 : 0.04));
        Color accent = asColor(resolved.accentColor(), Color.web("#007acc"));
        Color textPrimary = asColor(resolved.textColor(), dark ? Color.web("#d4d4d4") : Color.web("#2d2d2d"));
        Color textActive = asColor(resolved.textColorActive(), dark ? Color.WHITE : Color.BLACK);
        Color border = asColor(resolved.borderColor(), dark ? Color.web("#3f3f46") : Color.web("#c4c4c4"));
        Color hover = asColor(resolved.buttonHoverBackground(), blend(activeBackground, accent, dark ? 0.10 : 0.06));
        Color pressed = asColor(resolved.buttonPressedBackground(), blend(activeBackground, accent, dark ? 0.18 : 0.12));
        Color groupBackground = blend(toolbarBackground, background, dark ? 0.28 : 0.18);
        Color groupBorder = blend(border, accent, dark ? 0.08 : 0.05);
        Color controlBackground = blend(activeBackground, background, dark ? 0.12 : 0.04);
        Color textMuted = blend(textPrimary, background, dark ? 0.36 : 0.48);
        Color textDisabled = blend(textPrimary, background, dark ? 0.52 : 0.62);
        Color linkText = blend(accent, textActive, dark ? 0.18 : 0.12);
        Color success = dark ? Color.web("#47a473") : Color.web("#1d7a46");
        Color warning = dark ? Color.web("#c69a31") : Color.web("#a66b00");
        Color danger = dark ? Color.web("#d16969") : Color.web("#b64141");
        Color badgeBackground = blend(groupBackground, background, dark ? 0.10 : 0.04);
        Color badgeBorder = blend(border, textPrimary, dark ? 0.14 : 0.08);
        Color statusBackground = blend(groupBackground, background, dark ? 0.36 : 0.24);
        Color errorBackground = alpha(danger, dark ? 0.18 : 0.12);
        Color busyTrack = alpha(border, dark ? 0.35 : 0.24);
        Color busyIndicator = accent;
        Color shadow = alpha(Color.BLACK, dark ? 0.28 : 0.16);

        Insets basePadding = resolved.contentPadding() == null ? Insets.EMPTY : resolved.contentPadding();
        Insets contentPadding = new Insets(
            Math.max(8.0, basePadding.getTop() + 4.0),
            Math.max(12.0, basePadding.getRight() + 8.0),
            Math.max(8.0, basePadding.getBottom() + 4.0),
            Math.max(12.0, basePadding.getLeft() + 8.0)
        );

        return new GitHubToolbarTheme(
            toolbarBackground,
            border,
            groupBackground,
            groupBorder,
            controlBackground,
            hover,
            pressed,
            border,
            accent,
            textPrimary,
            textMuted,
            textDisabled,
            linkText,
            accent,
            success,
            warning,
            danger,
            badgeBackground,
            badgeBorder,
            statusBackground,
            errorBackground,
            busyTrack,
            busyIndicator,
            shadow,
            Math.max(8.0, resolved.cornerRadius() + 4.0),
            Math.max(5.0, resolved.cornerRadius() + 1.0),
            Math.max(46.0, resolved.headerHeight() + 16.0),
            Math.max(28.0, resolved.tabHeight() + 4.0),
            contentPadding,
            Math.max(6.0, resolved.buttonSpacing() * 0.75)
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
}
