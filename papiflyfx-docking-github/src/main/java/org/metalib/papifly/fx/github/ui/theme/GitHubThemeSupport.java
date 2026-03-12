package org.metalib.papifly.fx.github.ui.theme;

import javafx.scene.Parent;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

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
        return """
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
            paintToCss(theme.busyIndicator(), "#007acc")
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
