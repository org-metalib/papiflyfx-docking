package org.metalib.papifly.fx.tree.theme;

import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;

public record TreeViewTheme(
    Paint background,
    Paint rowBackground,
    Paint rowBackgroundAlternate,
    Paint selectedBackground,
    Paint selectedBackgroundUnfocused,
    Paint focusedBorder,
    Paint hoverBackground,
    Paint textColor,
    Paint textColorSelected,
    Paint disclosureColor,
    Paint connectingLineColor,
    Paint scrollbarTrackColor,
    Paint scrollbarThumbColor,
    Paint scrollbarThumbHoverColor,
    Paint scrollbarThumbActiveColor,
    Font font,
    double rowHeight,
    double indentWidth,
    double iconSize
) {
    public static TreeViewTheme dark() {
        return new TreeViewTheme(
            Color.web("#1e1e1e"),
            Color.web("#1e1e1e"),
            Color.web("#252526"),
            Color.web("#094771"),
            Color.web("#3a3d41"),
            Color.web("#007acc"),
            Color.web("#2a2d2e"),
            Color.web("#d4d4d4"),
            Color.web("#ffffff"),
            Color.web("#c5c5c5"),
            Color.web("#3f3f46"),
            Color.rgb(255, 255, 255, 0.08),
            Color.rgb(255, 255, 255, 0.32),
            Color.rgb(255, 255, 255, 0.46),
            Color.rgb(255, 255, 255, 0.58),
            Font.font("System", 13),
            24.0,
            18.0,
            14.0
        );
    }

    public static TreeViewTheme light() {
        return new TreeViewTheme(
            Color.web("#ffffff"),
            Color.web("#ffffff"),
            Color.web("#f3f3f3"),
            Color.web("#cce8ff"),
            Color.web("#e1e1e1"),
            Color.web("#007acc"),
            Color.web("#e8f2ff"),
            Color.web("#1e1e1e"),
            Color.web("#1e1e1e"),
            Color.web("#444444"),
            Color.web("#c8c8c8"),
            Color.rgb(0, 0, 0, 0.08),
            Color.rgb(0, 0, 0, 0.24),
            Color.rgb(0, 0, 0, 0.38),
            Color.rgb(0, 0, 0, 0.5),
            Font.font("System", 13),
            24.0,
            18.0,
            14.0
        );
    }
}
