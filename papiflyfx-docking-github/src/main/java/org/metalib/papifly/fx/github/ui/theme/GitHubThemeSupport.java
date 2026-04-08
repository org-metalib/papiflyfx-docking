package org.metalib.papifly.fx.github.ui.theme;

import javafx.scene.Parent;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import org.metalib.papifly.fx.ui.UiStyleSupport;

import java.net.URL;
import java.util.Locale;

public final class GitHubThemeSupport {

    public static final String TOOLBAR_STYLESHEET = "/org/metalib/papifly/fx/github/ui/github-toolbar.css";
    public static final String DIALOG_STYLESHEET = "/org/metalib/papifly/fx/github/ui/github-dialog.css";

    private GitHubThemeSupport() {
    }

    public static void ensureStylesheetLoaded(Parent parent, String resourcePath) {
        URL stylesheetUrl = GitHubThemeSupport.class.getResource(resourcePath);
        if (stylesheetUrl == null) {
            return;
        }
        String stylesheet = stylesheetUrl.toExternalForm();
        if (!parent.getStylesheets().contains(stylesheet)) {
            parent.getStylesheets().add(stylesheet);
        }
    }

    public static String themeVariables(GitHubToolbarTheme theme) {
        Color accent = UiStyleSupport.asColor(theme.accent(), Color.web("#007acc"));
        Color success = UiStyleSupport.asColor(theme.success(), Color.web("#47a473"));
        Color warning = UiStyleSupport.asColor(theme.warning(), Color.web("#c69a31"));
        Color danger = UiStyleSupport.asColor(theme.danger(), Color.web("#d16969"));
        return UiStyleSupport.metricVariables() + """
            -pf-github-toolbar-bg: %s;
            -pf-github-toolbar-border: %s;
            -pf-github-group-bg: %s;
            -pf-github-group-border: %s;
            -pf-github-control-bg: %s;
            -pf-github-control-hover-bg: %s;
            -pf-github-control-pressed-bg: %s;
            -pf-github-control-border: %s;
            -pf-github-focus-border: %s;
            -pf-github-text: %s;
            -pf-github-muted-text: %s;
            -pf-github-disabled-text: %s;
            -pf-github-link: %s;
            -pf-github-accent: %s;
            -pf-github-success: %s;
            -pf-github-warning: %s;
            -pf-github-danger: %s;
            -pf-github-badge-bg: %s;
            -pf-github-badge-border: %s;
            -pf-github-status-bg: %s;
            -pf-github-error-bg: %s;
            -pf-github-shadow: %s;
            -pf-github-busy-track: %s;
            -pf-github-busy-indicator: %s;
            -pf-ui-surface-panel: %s;
            -pf-ui-surface-panel-subtle: %s;
            -pf-ui-surface-overlay: %s;
            -pf-ui-surface-control: %s;
            -pf-ui-surface-control-hover: %s;
            -pf-ui-surface-control-pressed: %s;
            -pf-ui-surface-selected: %s;
            -pf-ui-text-primary: %s;
            -pf-ui-text-muted: %s;
            -pf-ui-text-disabled: %s;
            -pf-ui-border-default: %s;
            -pf-ui-border-subtle: %s;
            -pf-ui-border-focus: %s;
            -pf-ui-accent: %s;
            -pf-ui-accent-subtle: %s;
            -pf-ui-success: %s;
            -pf-ui-success-subtle: %s;
            -pf-ui-warning: %s;
            -pf-ui-warning-subtle: %s;
            -pf-ui-danger: %s;
            -pf-ui-danger-subtle: %s;
            -pf-ui-shadow-overlay: %s;
            """.formatted(
            paintToCss(theme.toolbarBackground(), "#2d2d2d"),
            paintToCss(theme.toolbarBorder(), "#3f3f46"),
            paintToCss(theme.groupBackground(), "#24262c"),
            paintToCss(theme.groupBorder(), "#3f3f46"),
            paintToCss(theme.controlBackground(), "#31343b"),
            paintToCss(theme.controlBackgroundHover(), "#3a3f49"),
            paintToCss(theme.controlBackgroundPressed(), "#454c58"),
            paintToCss(theme.controlBorder(), "#4c4f56"),
            paintToCss(theme.focusBorder(), "#007acc"),
            paintToCss(theme.textPrimary(), "#d4d4d4"),
            paintToCss(theme.textMuted(), "#9e9e9e"),
            paintToCss(theme.textDisabled(), "#7b7b7b"),
            paintToCss(theme.linkText(), "#5ab4ff"),
            paintToCss(theme.accent(), "#007acc"),
            paintToCss(theme.success(), "#47a473"),
            paintToCss(theme.warning(), "#c69a31"),
            paintToCss(theme.danger(), "#d16969"),
            paintToCss(theme.badgeBackground(), "#2b2f36"),
            paintToCss(theme.badgeBorder(), "#4c4f56"),
            paintToCss(theme.statusBackground(), "#20242c"),
            paintToCss(theme.errorBackground(), "rgba(209, 105, 105, 0.18)"),
            paintToCss(theme.shadow(), "rgba(0, 0, 0, 0.25)"),
            paintToCss(theme.busyTrack(), "rgba(255, 255, 255, 0.2)"),
            paintToCss(theme.busyIndicator(), "#007acc"),
            paintToCss(theme.groupBackground(), "#24262c"),
            paintToCss(theme.badgeBackground(), "#2b2f36"),
            paintToCss(theme.groupBackground(), "#24262c"),
            paintToCss(theme.controlBackground(), "#31343b"),
            paintToCss(theme.controlBackgroundHover(), "#3a3f49"),
            paintToCss(theme.controlBackgroundPressed(), "#454c58"),
            paintToCss(UiStyleSupport.alpha(accent, Color.web("#007acc"), 0.12), "rgba(0, 122, 204, 0.12)"),
            paintToCss(theme.textPrimary(), "#d4d4d4"),
            paintToCss(theme.textMuted(), "#9e9e9e"),
            paintToCss(theme.textDisabled(), "#7b7b7b"),
            paintToCss(theme.groupBorder(), "#3f3f46"),
            paintToCss(theme.controlBorder(), "#4c4f56"),
            paintToCss(theme.focusBorder(), "#007acc"),
            paintToCss(theme.accent(), "#007acc"),
            paintToCss(UiStyleSupport.alpha(accent, Color.web("#007acc"), 0.14), "rgba(0, 122, 204, 0.14)"),
            paintToCss(theme.success(), "#47a473"),
            paintToCss(UiStyleSupport.alpha(success, Color.web("#47a473"), 0.14), "rgba(71, 164, 115, 0.14)"),
            paintToCss(theme.warning(), "#c69a31"),
            paintToCss(UiStyleSupport.alpha(warning, Color.web("#c69a31"), 0.14), "rgba(198, 154, 49, 0.14)"),
            paintToCss(theme.danger(), "#d16969"),
            paintToCss(UiStyleSupport.alpha(danger, Color.web("#d16969"), 0.18), "rgba(209, 105, 105, 0.18)"),
            paintToCss(theme.shadow(), "rgba(0, 0, 0, 0.25)")
        );
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
}
