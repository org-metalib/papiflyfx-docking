package org.metalib.papifly.fx.ui;

import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import org.metalib.papifly.fx.docking.api.Theme;

import java.util.Locale;

/**
 * Emits the shared CSS token set defined by the UI standardization spec.
 */
public final class UiCommonThemeSupport {

    private static final double DARK_THRESHOLD = 0.5;

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

    public enum SemanticTone {
        SUCCESS,
        WARNING,
        DANGER
    }

    private UiCommonThemeSupport() {
    }

    public static String themeVariables(UiCommonPalette palette) {
        Theme darkFallback = Theme.dark();
        Color accent = asColor(palette.accent(), asColor(darkFallback.accentColor(), Color.TRANSPARENT));
        Color success = asColor(palette.success(), semanticColor(true, SemanticTone.SUCCESS));
        Color warning = asColor(palette.warning(), semanticColor(true, SemanticTone.WARNING));
        Color danger = asColor(palette.danger(), semanticColor(true, SemanticTone.DANGER));
        Color textPrimary = asColor(palette.textPrimary(), asColor(darkFallback.textColor(), Color.TRANSPARENT));
        Color borderDefault = asColor(palette.borderDefault(), asColor(darkFallback.borderColor(), Color.TRANSPARENT));
        Color surfaceOverlay = asColor(palette.surfaceOverlay(), asColor(darkFallback.headerBackground(), textPrimary));
        Color surfaceControl = asColor(palette.surfaceControl(), borderDefault);
        Color surfaceControlHover = asColor(palette.surfaceControlHover(), alpha(accent, 0.12));
        Color surfaceControlPressed = asColor(palette.surfaceControlPressed(), alpha(accent, 0.20));
        Color textMuted = asColor(palette.textMuted(), alpha(textPrimary, 0.66));
        Color textDisabled = asColor(palette.textDisabled(), alpha(textPrimary, 0.50));
        Color borderFocus = asColor(palette.borderFocus(), accent);
        Color dropHint = asColor(palette.dropHint(), accent);
        Color shadowOverlay = asColor(palette.shadowOverlay(), alpha(Color.BLACK, 0.25));

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
            paintToCss(surfaceOverlay, "transparent"),
            paintToCss(surfaceOverlay, "transparent"),
            paintToCss(surfaceControl, "transparent"),
            paintToCss(surfaceOverlay, "transparent"),
            paintToCss(surfaceControl, "transparent"),
            paintToCss(surfaceControlHover, "transparent"),
            paintToCss(surfaceControlPressed, "transparent"),
            paintToCss(alpha(accent, 0.16), "transparent"),
            paintToCss(alpha(accent, 0.10), "transparent"),
            paintToCss(textPrimary, "transparent"),
            paintToCss(textMuted, "transparent"),
            paintToCss(textDisabled, "transparent"),
            paintToCss(accent, "transparent"),
            paintToCss(contrastOn(accent), "transparent"),
            paintToCss(borderDefault, "transparent"),
            paintToCss(alpha(borderDefault, 0.72), "transparent"),
            paintToCss(borderFocus, "transparent"),
            paintToCss(borderDefault, "transparent"),
            paintToCss(accent, "transparent"),
            paintToCss(alpha(accent, 0.16), "transparent"),
            paintToCss(success, "transparent"),
            paintToCss(alpha(success, 0.14), "transparent"),
            paintToCss(warning, "transparent"),
            paintToCss(alpha(warning, 0.14), "transparent"),
            paintToCss(danger, "transparent"),
            paintToCss(alpha(danger, 0.16), "transparent"),
            paintToCss(dropHint, "transparent"),
            paintToCss(shadowOverlay, "transparent"),
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

    public static Theme resolvedTheme(Theme theme) {
        return theme == null ? Theme.dark() : theme;
    }

    public static Theme fallbackTheme(Theme theme) {
        Theme resolved = resolvedTheme(theme);
        return isDark(resolved.background()) ? Theme.dark() : Theme.light();
    }

    public static boolean isDark(Paint paint) {
        if (paint instanceof Color color) {
            return color.getBrightness() < DARK_THRESHOLD;
        }
        return true;
    }

    public static Color background(Theme theme) {
        Theme resolved = resolvedTheme(theme);
        Theme fallback = fallbackTheme(resolved);
        return asColor(resolved.background(), asColor(fallback.background(), Color.TRANSPARENT));
    }

    public static Color headerBackground(Theme theme) {
        Theme resolved = resolvedTheme(theme);
        Theme fallback = fallbackTheme(resolved);
        return asColor(resolved.headerBackground(), asColor(fallback.headerBackground(), background(resolved)));
    }

    public static Color headerBackgroundActive(Theme theme) {
        Theme resolved = resolvedTheme(theme);
        Theme fallback = fallbackTheme(resolved);
        return asColor(resolved.headerBackgroundActive(), asColor(fallback.headerBackgroundActive(), headerBackground(resolved)));
    }

    public static Color accent(Theme theme) {
        Theme resolved = resolvedTheme(theme);
        Theme fallback = fallbackTheme(resolved);
        return asColor(resolved.accentColor(), asColor(fallback.accentColor(), textPrimary(resolved)));
    }

    public static Color textPrimary(Theme theme) {
        Theme resolved = resolvedTheme(theme);
        Theme fallback = fallbackTheme(resolved);
        return asColor(resolved.textColor(), asColor(fallback.textColor(), Color.TRANSPARENT));
    }

    public static Color textActive(Theme theme) {
        Theme resolved = resolvedTheme(theme);
        Theme fallback = fallbackTheme(resolved);
        return asColor(resolved.textColorActive(), asColor(fallback.textColorActive(), textPrimary(resolved)));
    }

    public static Color border(Theme theme) {
        Theme resolved = resolvedTheme(theme);
        Theme fallback = fallbackTheme(resolved);
        return asColor(resolved.borderColor(), asColor(fallback.borderColor(), textPrimary(resolved)));
    }

    public static Color divider(Theme theme) {
        Theme resolved = resolvedTheme(theme);
        Theme fallback = fallbackTheme(resolved);
        return asColor(resolved.dividerColor(), asColor(fallback.dividerColor(), border(resolved)));
    }

    public static Color hover(Theme theme) {
        Theme resolved = resolvedTheme(theme);
        Theme fallback = fallbackTheme(resolved);
        return asColor(resolved.buttonHoverBackground(), asColor(fallback.buttonHoverBackground(), alpha(accent(resolved), 0.12)));
    }

    public static Color pressed(Theme theme) {
        Theme resolved = resolvedTheme(theme);
        Theme fallback = fallbackTheme(resolved);
        return asColor(resolved.buttonPressedBackground(), asColor(fallback.buttonPressedBackground(), alpha(accent(resolved), 0.20)));
    }

    public static Color dropHint(Theme theme) {
        Theme resolved = resolvedTheme(theme);
        Theme fallback = fallbackTheme(resolved);
        return asColor(resolved.dropHintColor(), asColor(fallback.dropHintColor(), accent(resolved)));
    }

    public static Color success(Theme theme) {
        return semanticColor(isDark(background(theme)), SemanticTone.SUCCESS);
    }

    public static Color warning(Theme theme) {
        return semanticColor(isDark(background(theme)), SemanticTone.WARNING);
    }

    public static Color danger(Theme theme) {
        return semanticColor(isDark(background(theme)), SemanticTone.DANGER);
    }

    public static Color semanticColor(boolean dark, SemanticTone tone) {
        return switch (tone) {
            case SUCCESS -> dark ? Color.rgb(71, 164, 115) : Color.rgb(29, 122, 70);
            case WARNING -> dark ? Color.rgb(198, 154, 49) : Color.rgb(166, 107, 0);
            case DANGER -> dark ? Color.rgb(209, 105, 105) : Color.rgb(182, 65, 65);
        };
    }

    public static Color shadowColor(Theme theme, double darkOpacity, double lightOpacity) {
        return alpha(Color.BLACK, isDark(background(theme)) ? darkOpacity : lightOpacity);
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
