package org.metalib.papifly.fx.ui;

import javafx.scene.Parent;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;

import java.net.URL;
import java.util.Locale;

/**
 * Shared stylesheet and token helpers for lightweight JavaFX UI components.
 */
public final class UiStyleSupport {

    public static final String COMMON_STYLESHEET = "/org/metalib/papifly/fx/ui/ui-common.css";

    private UiStyleSupport() {
    }

    public static void ensureCommonStylesheetLoaded(Parent parent) {
        ensureStylesheetLoaded(parent, COMMON_STYLESHEET);
    }

    public static void ensureStylesheetLoaded(Parent parent, String resourcePath) {
        URL stylesheetUrl = UiStyleSupport.class.getResource(resourcePath);
        if (stylesheetUrl == null) {
            return;
        }
        String stylesheet = stylesheetUrl.toExternalForm();
        if (!parent.getStylesheets().contains(stylesheet)) {
            parent.getStylesheets().add(stylesheet);
        }
    }

    public static String metricVariables() {
        return String.format(Locale.ROOT, """
            -pf-ui-font-family: "System";
            -pf-ui-font-size-xs: 10;
            -pf-ui-font-size-sm: 11;
            -pf-ui-font-size-md: 12;
            -pf-ui-font-weight-strong: bold;
            -pf-ui-space-1: %.1fpx;
            -pf-ui-space-2: %.1fpx;
            -pf-ui-space-3: %.1fpx;
            -pf-ui-space-4: %.1fpx;
            -pf-ui-space-5: %.1fpx;
            -pf-ui-space-6: %.1fpx;
            -pf-ui-radius-sm: %.1fpx;
            -pf-ui-radius-md: %.1fpx;
            -pf-ui-radius-lg: %.1fpx;
            -pf-ui-radius-pill: %.1fpx;
            -pf-ui-control-height-compact: %.1fpx;
            -pf-ui-control-height-regular: %.1fpx;
            -pf-ui-toolbar-height: %.1fpx;
            """,
            UiMetrics.SPACE_1,
            UiMetrics.SPACE_2,
            UiMetrics.SPACE_3,
            UiMetrics.SPACE_4,
            UiMetrics.SPACE_5,
            UiMetrics.SPACE_6,
            UiMetrics.RADIUS_SM,
            UiMetrics.RADIUS_MD,
            UiMetrics.RADIUS_LG,
            UiMetrics.RADIUS_PILL,
            UiMetrics.CONTROL_HEIGHT_COMPACT,
            UiMetrics.CONTROL_HEIGHT_REGULAR,
            UiMetrics.TOOLBAR_HEIGHT
        );
    }

    public static String fontVariables(Font font) {
        String family = font == null ? "System" : font.getFamily().replace("\"", "\\\"");
        double size = font == null ? 12.0 : font.getSize();
        return String.format(Locale.ROOT, """
            -pf-ui-font-family: "%s";
            -pf-ui-font-size-xs: %.1f;
            -pf-ui-font-size-sm: %.1f;
            -pf-ui-font-size-md: %.1f;
            -pf-ui-font-weight-strong: bold;
            """, family, Math.max(10.0, size - 2.0), Math.max(11.0, size - 1.0), size);
    }

    public static String paintToCss(Paint paint, String fallback) {
        if (paint instanceof Color color) {
            int red = (int) Math.round(color.getRed() * 255.0);
            int green = (int) Math.round(color.getGreen() * 255.0);
            int blue = (int) Math.round(color.getBlue() * 255.0);
            return String.format(Locale.ROOT, "rgba(%d, %d, %d, %.3f)", red, green, blue, color.getOpacity());
        }
        return fallback;
    }

    public static Color asColor(Paint paint, Color fallback) {
        if (paint instanceof Color color) {
            return color;
        }
        return fallback;
    }

    public static Color alpha(Paint paint, Color fallback, double opacity) {
        Color color = asColor(paint, fallback);
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), clamp(opacity));
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
