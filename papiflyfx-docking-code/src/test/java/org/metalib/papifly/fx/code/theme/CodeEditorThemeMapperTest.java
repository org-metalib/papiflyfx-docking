package org.metalib.papifly.fx.code.theme;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.docking.api.Theme;

import static org.junit.jupiter.api.Assertions.*;

class CodeEditorThemeMapperTest {

    @Test
    void mapNullReturnsDefaultDark() {
        CodeEditorTheme result = CodeEditorThemeMapper.map(null);
        assertNotNull(result);
        assertEquals(CodeEditorTheme.dark(), result);
    }

    @Test
    void mapDarkThemeProducesDarkPalette() {
        CodeEditorTheme result = CodeEditorThemeMapper.map(Theme.dark());
        assertNotNull(result);
        // Background comes from the base theme
        assertEquals(Theme.dark().background(), result.editorBackground());
        // Gutter matches editor
        assertEquals(result.editorBackground(), result.gutterBackground());
        // Accent flows to bookmark and search accent border
        assertEquals(Theme.dark().accentColor(), result.markerBookmarkColor());
        assertEquals(Theme.dark().accentColor(), result.searchOverlayAccentBorder());
        assertEquals(Theme.dark().accentColor(), result.searchOverlayControlFocusedBorder());
        assertEquals(Theme.dark().accentColor(), result.searchOverlayIntegratedToggleActive());
    }

    @Test
    void mapLightThemeProducesLightPalette() {
        CodeEditorTheme result = CodeEditorThemeMapper.map(Theme.light());
        assertNotNull(result);
        assertEquals(Theme.light().background(), result.editorBackground());
        // Light palette foreground should be dark
        Color fg = (Color) result.editorForeground();
        assertTrue(fg.getBrightness() < 0.3, "Light theme foreground should be dark");
    }

    @Test
    void isDarkDetectsDarkColors() {
        assertTrue(CodeEditorThemeMapper.isDark(Color.BLACK));
        assertTrue(CodeEditorThemeMapper.isDark(Color.web("#1e1e1e")));
        assertFalse(CodeEditorThemeMapper.isDark(Color.WHITE));
        assertFalse(CodeEditorThemeMapper.isDark(Color.web("#f0f0f0")));
    }

    @Test
    void isDarkHandlesNonColorPaint() {
        // Non-Color paint defaults to dark
        assertTrue(CodeEditorThemeMapper.isDark(javafx.scene.paint.LinearGradient.valueOf(
            "from 0% 0% to 100% 100%, white 0%, black 100%")));
    }

    @Test
    void darkAndLightThemesHaveDifferentForeground() {
        CodeEditorTheme dark = CodeEditorThemeMapper.map(Theme.dark());
        CodeEditorTheme light = CodeEditorThemeMapper.map(Theme.light());
        assertNotEquals(dark.editorForeground(), light.editorForeground());
    }

    @Test
    void darkPaletteFactoryRoundTrips() {
        CodeEditorTheme dark = CodeEditorTheme.dark();
        assertNotNull(dark.editorBackground());
        assertNotNull(dark.keywordColor());
        assertNotNull(dark.stringColor());
        assertNotNull(dark.commentColor());
        assertNotNull(dark.numberColor());
        assertNotNull(dark.caretColor());
        assertNotNull(dark.selectionColor());
        assertNotNull(dark.lineNumberColor());
        assertNotNull(dark.lineNumberActiveColor());
        assertNotNull(dark.scrollbarTrackColor());
        assertNotNull(dark.scrollbarThumbColor());
        assertNotNull(dark.scrollbarThumbHoverColor());
        assertNotNull(dark.scrollbarThumbActiveColor());
    }

    @Test
    void lightPaletteFactoryRoundTrips() {
        CodeEditorTheme light = CodeEditorTheme.light();
        assertNotNull(light.editorBackground());
        assertNotNull(light.keywordColor());
        assertNotNull(light.stringColor());
        assertNotNull(light.commentColor());
        assertNotNull(light.numberColor());
        assertNotNull(light.caretColor());
        assertNotNull(light.selectionColor());
        assertNotNull(light.lineNumberColor());
        assertNotNull(light.lineNumberActiveColor());
        assertNotNull(light.scrollbarTrackColor());
        assertNotNull(light.scrollbarThumbColor());
        assertNotNull(light.scrollbarThumbHoverColor());
        assertNotNull(light.scrollbarThumbActiveColor());
    }

    @Test
    void customAccentColorPropagates() {
        Theme custom = new Theme(
            Color.rgb(30, 30, 30),    // dark background
            Color.rgb(45, 45, 45),
            Color.rgb(60, 60, 60),
            Color.RED,                // custom accent
            Color.rgb(200, 200, 200),
            Color.WHITE,
            Color.rgb(60, 60, 60),
            Color.rgb(80, 80, 80),
            Color.rgb(0, 122, 204, 0.3),
            javafx.scene.text.Font.font(12),
            javafx.scene.text.Font.font(12),
            4.0, 1.0, 28.0, 24.0,
            javafx.geometry.Insets.EMPTY,
            Color.rgb(70, 70, 70),
            Color.rgb(90, 90, 90),
            Color.rgb(40, 40, 40),
            8.0, 24.0
        );
        CodeEditorTheme result = CodeEditorThemeMapper.map(custom);
        assertEquals(Color.RED, result.markerBookmarkColor());
        assertEquals(Color.RED, result.searchOverlayAccentBorder());
        assertEquals(Color.RED, result.searchOverlayControlFocusedBorder());
        assertEquals(Color.RED, result.searchOverlayIntegratedToggleActive());
    }
}
