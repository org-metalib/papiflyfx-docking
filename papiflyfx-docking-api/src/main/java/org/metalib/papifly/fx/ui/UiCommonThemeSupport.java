package org.metalib.papifly.fx.ui;

import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

import java.util.Locale;

/**
 * Emits the shared CSS token set defined by the UI standardization spec.
 */
public final class UiCommonThemeSupport {

    public static final double SPACE_1 = 4.0;
    public static final double SPACE_2 = 8.0;
    public static final double SPACE_3 = 12.0;
    public static final double SPACE_4 = 16.0;
    public static final double SPACE_5 = 20.0;
    public static final double SPACE_6 = 24.0;

    public static final double RADIUS_SM = 4.0;
    public static final double RADIUS_MD = 8.0;
    public static final double RADIUS_LG = 12.0;
    public static final double RADIUS_PILL = 999.0;

    public static final double CONTROL_HEIGHT_COMPACT = 24.0;
    public static final double CONTROL_HEIGHT_REGULAR = 28.0;
    public static final double TOOLBAR_HEIGHT = 44.0;

    private UiCommonThemeSupport() {
    }

    public static String themeVariables(UiCommonPalette palette) {
        Color accent = asColor(palette.accent(), Color.web("#007acc"));
        Color success = asColor(palette.success(), Color.web("#47a473"));
        Color warning = asColor(palette.warning(), Color.web("#c69a31"));
        Color danger = asColor(palette.danger(), Color.web("#d16969"));
        Color textPrimary = asColor(palette.textPrimary(), Color.web("#d4d4d4"));
        Color borderDefault = asColor(palette.borderDefault(), Color.web("#3f3f46"));

        return """
            -pf-ui-surface-canvas: %s;
            -pf-ui-surface-panel: %s;
            -pf-ui-surface-panel-subtle: %s;
            -pf-ui-surface-overlay: %s;
            -pf-ui-surface-control: %s;
            -pf-ui-surface-control-hover: %s;
            -pf-ui-surface-control-pressed: %s;
            -pf-ui-surface-selected: %s;
            -pf-ui-surface-selected-inactive: %s;
            -pf-ui-text-primary: %s;
            -pf-ui-text-muted: %s;
            -pf-ui-text-disabled: %s;
            -pf-ui-text-link: %s;
            -pf-ui-text-on-accent: %s;
            -pf-ui-border-default: %s;
            -pf-ui-border-subtle: %s;
            -pf-ui-border-focus: %s;
            -pf-ui-divider: %s;
            -pf-ui-accent: %s;
            -pf-ui-accent-subtle: %s;
            -pf-ui-success: %s;
            -pf-ui-success-subtle: %s;
            -pf-ui-warning: %s;
            -pf-ui-warning-subtle: %s;
            -pf-ui-danger: %s;
            -pf-ui-danger-subtle: %s;
            -pf-ui-drop-hint: %s;
            -pf-ui-shadow-overlay: %s;
            -pf-ui-font-family: "System";
            -pf-ui-font-size-xs: 10px;
            -pf-ui-font-size-sm: 11px;
            -pf-ui-font-size-md: 12px;
            -pf-ui-font-weight-strong: bold;
            -pf-ui-space-1: %s;
            -pf-ui-space-2: %s;
            -pf-ui-space-3: %s;
            -pf-ui-space-4: %s;
            -pf-ui-space-5: %s;
            -pf-ui-space-6: %s;
            -pf-ui-radius-sm: %s;
            -pf-ui-radius-md: %s;
            -pf-ui-radius-lg: %s;
            -pf-ui-radius-pill: %s;
            -pf-ui-control-height-compact: %s;
            -pf-ui-control-height-regular: %s;
            -pf-ui-toolbar-height: %s;
            """.formatted(
            paintToCss(palette.surfaceOverlay(), "#252526"),
            paintToCss(palette.surfaceOverlay(), "#252526"),
            paintToCss(palette.surfaceControl(), "#3c3c3c"),
            paintToCss(palette.surfaceOverlay(), "#252526"),
            paintToCss(palette.surfaceControl(), "#3c3c3c"),
            paintToCss(palette.surfaceControlHover(), "#4a4a4a"),
            paintToCss(palette.surfaceControlPressed(), "#164f7a"),
            paintToCss(alpha(accent, 0.16), "rgba(0, 122, 204, 0.16)"),
            paintToCss(alpha(accent, 0.10), "rgba(0, 122, 204, 0.10)"),
            paintToCss(textPrimary, "#d4d4d4"),
            paintToCss(palette.textMuted(), "#858585"),
            paintToCss(palette.textDisabled(), "#7a7a7a"),
            paintToCss(accent, "#007acc"),
            paintToCss(contrastOn(accent), "#ffffff"),
            paintToCss(borderDefault, "#3f3f46"),
            paintToCss(alpha(borderDefault, 0.72), "rgba(63, 63, 70, 0.72)"),
            paintToCss(palette.borderFocus(), "#007acc"),
            paintToCss(borderDefault, "#3f3f46"),
            paintToCss(accent, "#007acc"),
            paintToCss(alpha(accent, 0.16), "rgba(0, 122, 204, 0.16)"),
            paintToCss(success, "#47a473"),
            paintToCss(alpha(success, 0.14), "rgba(71, 164, 115, 0.14)"),
            paintToCss(warning, "#c69a31"),
            paintToCss(alpha(warning, 0.14), "rgba(198, 154, 49, 0.14)"),
            paintToCss(danger, "#d16969"),
            paintToCss(alpha(danger, 0.16), "rgba(209, 105, 105, 0.16)"),
            paintToCss(palette.dropHint(), "#007acc"),
            paintToCss(palette.shadowOverlay(), "rgba(0, 0, 0, 0.25)"),
            cssSize(SPACE_1),
            cssSize(SPACE_2),
            cssSize(SPACE_3),
            cssSize(SPACE_4),
            cssSize(SPACE_5),
            cssSize(SPACE_6),
            cssSize(RADIUS_SM),
            cssSize(RADIUS_MD),
            cssSize(RADIUS_LG),
            cssSize(RADIUS_PILL),
            cssSize(CONTROL_HEIGHT_COMPACT),
            cssSize(CONTROL_HEIGHT_REGULAR),
            cssSize(TOOLBAR_HEIGHT)
        );
    }

    public static Color asColor(Paint paint, Color fallback) {
        if (paint instanceof Color color) {
            return color;
        }
        return fallback;
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

    public static Color alpha(Paint paint, double opacity) {
        Color color = asColor(paint, Color.BLACK);
        double clamped = Math.max(0.0, Math.min(1.0, opacity));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), clamped);
    }

    public static String cssSize(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0001) {
            return String.format(Locale.ROOT, "%.0fpx", value);
        }
        return String.format(Locale.ROOT, "%.2fpx", value);
    }

    private static Color contrastOn(Color color) {
        return color.getBrightness() < 0.55 ? Color.WHITE : Color.BLACK;
    }
}
