package org.metalib.papifly.fx.hugo.theme;

import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import org.metalib.papifly.fx.docking.api.Theme;

public final class HugoThemeMapper {

    private HugoThemeMapper() {
    }

    public static Paint toolbarBackground(Theme theme) {
        return theme.headerBackground();
    }

    public static Paint statusBackground(Theme theme) {
        return theme.background();
    }

    public static Paint statusText(Theme theme) {
        return theme.textColor();
    }

    public static Paint linkColor(Theme theme) {
        return theme.accentColor();
    }

    public static Color toColor(Paint paint) {
        return paint instanceof Color color ? color : Color.GRAY;
    }

    public static String toHex(Paint paint) {
        Color color = toColor(paint);
        return String.format("#%02x%02x%02x",
            (int) Math.round(color.getRed() * 255.0),
            (int) Math.round(color.getGreen() * 255.0),
            (int) Math.round(color.getBlue() * 255.0));
    }

    public static boolean isDark(Theme theme) {
        Color bg = toColor(theme.background());
        double luminance = (0.299 * bg.getRed()) + (0.587 * bg.getGreen()) + (0.114 * bg.getBlue());
        return luminance < 0.5;
    }
}
