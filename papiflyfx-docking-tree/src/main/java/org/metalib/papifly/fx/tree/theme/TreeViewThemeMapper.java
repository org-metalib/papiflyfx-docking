package org.metalib.papifly.fx.tree.theme;

import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import org.metalib.papifly.fx.docking.api.Theme;

public final class TreeViewThemeMapper {

    private static final double DARK_THRESHOLD = 0.5;

    private TreeViewThemeMapper() {}

    public static TreeViewTheme map(Theme theme) {
        if (theme == null) {
            return TreeViewTheme.dark();
        }
        TreeViewTheme base = isDark(theme.background()) ? TreeViewTheme.dark() : TreeViewTheme.light();
        return new TreeViewTheme(
            theme.background(),
            base.rowBackground(),
            base.rowBackgroundAlternate(),
            theme.accentColor(),
            base.selectedBackgroundUnfocused(),
            theme.accentColor(),
            base.hoverBackground(),
            theme.textColor(),
            theme.textColorActive(),
            theme.textColor(),
            theme.dividerColor(),
            base.scrollbarTrackColor(),
            base.scrollbarThumbColor(),
            base.scrollbarThumbHoverColor(),
            base.scrollbarThumbActiveColor(),
            theme.contentFont(),
            Math.max(20.0, theme.tabHeight()),
            base.indentWidth(),
            base.iconSize()
        );
    }

    static boolean isDark(Paint paint) {
        if (paint instanceof Color color) {
            return color.getBrightness() < DARK_THRESHOLD;
        }
        return true;
    }
}
