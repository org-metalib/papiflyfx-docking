package org.metalib.papifly.fx.code.theme;

import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import org.metalib.papifly.fx.docking.api.Theme;

/**
 * Maps a docking {@link Theme} to a {@link CodeEditorTheme} by composition.
 * <p>
 * Dark/light detection uses the brightness of the base theme's background
 * color, choosing the appropriate editor palette and blending accent colors
 * from the base theme.
 */
public final class CodeEditorThemeMapper {

    private static final double DARK_THRESHOLD = 0.5;

    /**
     * Utility class.
     */
    private CodeEditorThemeMapper() {}

    /**
     * Creates a {@link CodeEditorTheme} derived from the given docking theme.
     *
     * @param theme docking theme source, or {@code null} for default dark theme
     * @return mapped editor theme
     */
    public static CodeEditorTheme map(Theme theme) {
        if (theme == null) {
            return CodeEditorTheme.dark();
        }

        boolean dark = isDark(theme.background());
        CodeEditorTheme base = dark ? CodeEditorTheme.dark() : CodeEditorTheme.light();

        Paint accent = theme.accentColor();
        Paint bg = theme.background();

        return new CodeEditorTheme(
            bg,
            base.editorForeground(),
            base.keywordColor(),
            base.stringColor(),
            base.commentColor(),
            base.numberColor(),
            base.caretColor(),
            base.selectionColor(),
            base.lineNumberColor(),
            base.lineNumberActiveColor(),
            base.booleanColor(),
            base.nullLiteralColor(),
            base.headlineColor(),
            base.listItemColor(),
            base.codeBlockColor(),
            base.currentLineColor(),
            base.searchHighlightColor(),
            base.searchCurrentColor(),
            bg,                                 // gutterBackground follows editor bg
            base.markerErrorColor(),
            base.markerWarningColor(),
            base.markerInfoColor(),
            base.markerBreakpointColor(),
            accent,                             // markerBookmarkColor from accent
            base.scrollbarTrackColor(),
            base.scrollbarThumbColor(),
            base.scrollbarThumbHoverColor(),
            base.scrollbarThumbActiveColor(),
            base.searchOverlayBackground(),
            accent,                             // searchOverlayAccentBorder from accent
            base.searchOverlayControlBackground(),
            base.searchOverlayControlBorder(),
            base.searchOverlayPrimaryText(),
            base.searchOverlaySecondaryText(),
            base.searchOverlayPanelBorder(),
            base.searchOverlayControlHoverBackground(),
            base.searchOverlayControlActiveBackground(),
            accent,                             // searchOverlayControlFocusedBorder from accent
            base.searchOverlayControlDisabledText(),
            base.searchOverlayNoResultsBorder(),
            base.searchOverlayShadowColor(),
            accent,                             // searchOverlayIntegratedToggleActive from accent
            base.searchOverlayErrorBackground()
        );
    }

    /**
     * Returns {@code true} if the paint is considered dark.
     *
     * @param paint paint to evaluate
     * @return {@code true} when brightness is below the dark threshold
     */
    static boolean isDark(Paint paint) {
        if (paint instanceof Color c) {
            return c.getBrightness() < DARK_THRESHOLD;
        }
        return true;
    }
}
