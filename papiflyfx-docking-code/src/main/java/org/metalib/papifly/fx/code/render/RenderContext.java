package org.metalib.papifly.fx.code.render;

import javafx.scene.canvas.GraphicsContext;
import org.metalib.papifly.fx.code.command.CaretRange;
import org.metalib.papifly.fx.code.search.SearchMatch;
import org.metalib.papifly.fx.code.theme.CodeEditorTheme;

import java.util.List;
import java.util.Map;

/**
 * Immutable render frame context shared by viewport render passes.
 */
record RenderContext(
    GraphicsContext graphics,
    CodeEditorTheme theme,
    GlyphCache glyphCache,
    SelectionModel selectionModel,
    List<RenderLine> renderLines,
    List<CaretRange> activeCarets,
    boolean hasMultiCarets,
    boolean paintCaret,
    List<SearchMatch> searchMatches,
    Map<Integer, List<Integer>> searchMatchIndexesByLine,
    int currentSearchMatchIndex,
    int firstVisibleLine,
    int visibleLineCount,
    double viewportWidth,
    double viewportHeight,
    double lineHeight,
    double charWidth,
    double baseline,
    double scrollOffset
) {

    boolean isLineVisible(int line) {
        return line >= firstVisibleLine && line < firstVisibleLine + visibleLineCount;
    }

    double lineToY(int line) {
        return line * lineHeight - scrollOffset;
    }
}
