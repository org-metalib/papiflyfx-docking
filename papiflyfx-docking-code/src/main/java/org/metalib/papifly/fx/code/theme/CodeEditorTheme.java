package org.metalib.papifly.fx.code.theme;

import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

/**
 * Immutable palette for the code editor.
 * <p>
 * This record is a separate composition from the docking {@code Theme} record.
 * Instances are created by {@link CodeEditorThemeMapper} which derives editor-
 * specific colors from the base docking theme.
 */
public record CodeEditorTheme(
    // Core editor colors (from spec)
    Paint editorBackground,
    Paint editorForeground,
    Paint keywordColor,
    Paint stringColor,
    Paint commentColor,
    Paint numberColor,
    Paint caretColor,
    Paint selectionColor,
    Paint lineNumberColor,
    Paint lineNumberActiveColor,

    // Extended syntax colors
    Paint booleanColor,
    Paint nullLiteralColor,
    Paint headlineColor,
    Paint listItemColor,
    Paint codeBlockColor,

    // Current-line highlight
    Paint currentLineColor,

    // Search highlights
    Paint searchHighlightColor,
    Paint searchCurrentColor,

    // Gutter
    Paint gutterBackground,
    Paint markerErrorColor,
    Paint markerWarningColor,
    Paint markerInfoColor,
    Paint markerBreakpointColor,
    Paint markerBookmarkColor,

    // Canvas scrollbars
    Paint scrollbarTrackColor,
    Paint scrollbarThumbColor,
    Paint scrollbarThumbHoverColor,
    Paint scrollbarThumbActiveColor,

    // Search overlay
    Paint searchOverlayBackground,
    Paint searchOverlayAccentBorder,
    Paint searchOverlayControlBackground,
    Paint searchOverlayControlBorder,
    Paint searchOverlayPrimaryText,
    Paint searchOverlaySecondaryText,
    Paint searchOverlayPanelBorder,
    Paint searchOverlayControlHoverBackground,
    Paint searchOverlayControlActiveBackground,
    Paint searchOverlayControlFocusedBorder,
    Paint searchOverlayControlDisabledText,
    Paint searchOverlayNoResultsBorder,
    Paint searchOverlayShadowColor,
    Paint searchOverlayIntegratedToggleActive,
    Paint searchOverlayErrorBackground
) {
    /**
     * Default dark palette matching the previously hardcoded values.
     */
    public static CodeEditorTheme dark() {
        return new CodeEditorTheme(
            Color.web("#1e1e1e"),   // editorBackground
            Color.web("#d4d4d4"),   // editorForeground
            Color.web("#569cd6"),   // keywordColor
            Color.web("#ce9178"),   // stringColor
            Color.web("#6a9955"),   // commentColor
            Color.web("#b5cea8"),   // numberColor
            Color.web("#aeafad"),   // caretColor
            Color.web("#264f78"),   // selectionColor
            Color.web("#858585"),   // lineNumberColor
            Color.web("#c6c6c6"),   // lineNumberActiveColor
            Color.web("#4ec9b0"),   // booleanColor
            Color.web("#4ec9b0"),   // nullLiteralColor
            Color.web("#569cd6"),   // headlineColor
            Color.web("#9cdcfe"),   // listItemColor
            Color.web("#d7ba7d"),   // codeBlockColor
            Color.web("#2a2d2e"),   // currentLineColor
            Color.web("#623315"),   // searchHighlightColor
            Color.web("#9e6a03"),   // searchCurrentColor
            Color.web("#1e1e1e"),   // gutterBackground
            Color.web("#f44747"),   // markerErrorColor
            Color.web("#cca700"),   // markerWarningColor
            Color.web("#75beff"),   // markerInfoColor
            Color.web("#e51400"),   // markerBreakpointColor
            Color.web("#569cd6"),   // markerBookmarkColor
            Color.rgb(255, 255, 255, 0.08), // scrollbarTrackColor
            Color.rgb(255, 255, 255, 0.32), // scrollbarThumbColor
            Color.rgb(255, 255, 255, 0.46), // scrollbarThumbHoverColor
            Color.rgb(255, 255, 255, 0.58), // scrollbarThumbActiveColor
            Color.web("#252526"),   // searchOverlayBackground
            Color.web("#007acc"),   // searchOverlayAccentBorder
            Color.web("#3c3c3c"),   // searchOverlayControlBackground
            Color.web("#555555"),   // searchOverlayControlBorder
            Color.web("#d4d4d4"),   // searchOverlayPrimaryText
            Color.web("#858585"),   // searchOverlaySecondaryText
            Color.web("#3f3f46"),   // searchOverlayPanelBorder
            Color.web("#4a4a4a"),   // searchOverlayControlHoverBackground
            Color.web("#164f7a"),   // searchOverlayControlActiveBackground
            Color.web("#007acc"),   // searchOverlayControlFocusedBorder
            Color.web("#7a7a7a"),   // searchOverlayControlDisabledText
            Color.web("#d16969"),   // searchOverlayNoResultsBorder
            Color.rgb(0, 0, 0, 0.25), // searchOverlayShadowColor
            Color.web("#007acc"),   // searchOverlayIntegratedToggleActive
            Color.rgb(209, 105, 105, 0.16) // searchOverlayErrorBackground
        );
    }

    /**
     * Default light palette.
     */
    public static CodeEditorTheme light() {
        return new CodeEditorTheme(
            Color.web("#ffffff"),   // editorBackground
            Color.web("#1e1e1e"),   // editorForeground
            Color.web("#0000ff"),   // keywordColor
            Color.web("#a31515"),   // stringColor
            Color.web("#008000"),   // commentColor
            Color.web("#098658"),   // numberColor
            Color.web("#000000"),   // caretColor
            Color.web("#add6ff"),   // selectionColor
            Color.web("#999999"),   // lineNumberColor
            Color.web("#333333"),   // lineNumberActiveColor
            Color.web("#267f99"),   // booleanColor
            Color.web("#267f99"),   // nullLiteralColor
            Color.web("#0000ff"),   // headlineColor
            Color.web("#001080"),   // listItemColor
            Color.web("#795e26"),   // codeBlockColor
            Color.web("#f0f0f0"),   // currentLineColor
            Color.web("#f5d9a8"),   // searchHighlightColor
            Color.web("#e8ab00"),   // searchCurrentColor
            Color.web("#f3f3f3"),   // gutterBackground
            Color.web("#e51400"),   // markerErrorColor
            Color.web("#bf8803"),   // markerWarningColor
            Color.web("#1a85ff"),   // markerInfoColor
            Color.web("#e51400"),   // markerBreakpointColor
            Color.web("#0000ff"),   // markerBookmarkColor
            Color.rgb(0, 0, 0, 0.08), // scrollbarTrackColor
            Color.rgb(0, 0, 0, 0.24), // scrollbarThumbColor
            Color.rgb(0, 0, 0, 0.38), // scrollbarThumbHoverColor
            Color.rgb(0, 0, 0, 0.5), // scrollbarThumbActiveColor
            Color.web("#f3f3f3"),   // searchOverlayBackground
            Color.web("#007acc"),   // searchOverlayAccentBorder
            Color.web("#ffffff"),   // searchOverlayControlBackground
            Color.web("#c8c8c8"),   // searchOverlayControlBorder
            Color.web("#1e1e1e"),   // searchOverlayPrimaryText
            Color.web("#999999"),   // searchOverlaySecondaryText
            Color.web("#d0d0d0"),   // searchOverlayPanelBorder
            Color.web("#eaeaea"),   // searchOverlayControlHoverBackground
            Color.web("#cde8ff"),   // searchOverlayControlActiveBackground
            Color.web("#007acc"),   // searchOverlayControlFocusedBorder
            Color.web("#b3b3b3"),   // searchOverlayControlDisabledText
            Color.web("#bf4f4f"),   // searchOverlayNoResultsBorder
            Color.rgb(0, 0, 0, 0.18), // searchOverlayShadowColor
            Color.web("#007acc"),   // searchOverlayIntegratedToggleActive
            Color.rgb(191, 79, 79, 0.12) // searchOverlayErrorBackground
        );
    }
}
