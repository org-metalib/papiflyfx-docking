package org.metalib.papifly.fx.code.render;

import javafx.scene.canvas.GraphicsContext;
import org.metalib.papifly.fx.code.search.SearchMatch;

import java.util.List;

/**
 * Paints search match highlight overlays.
 */
final class SearchPass implements RenderPass {

    @Override
    public void renderFull(RenderContext context) {
        if (context.searchMatches().isEmpty()) {
            return;
        }
        for (RenderLine renderLine : context.renderLines()) {
            renderLine(context, renderLine);
        }
    }

    @Override
    public void renderLine(RenderContext context, RenderLine renderLine) {
        if (context.searchMatches().isEmpty()) {
            return;
        }
        List<Integer> lineIndexes = context.searchMatchIndexesByLine().get(renderLine.lineIndex());
        if (lineIndexes == null || lineIndexes.isEmpty()) {
            return;
        }
        GraphicsContext gc = context.graphics();
        for (int matchIndex : lineIndexes) {
            SearchMatch match = context.searchMatches().get(matchIndex);
            double x = match.startColumn() * context.charWidth();
            double width = (match.endColumn() - match.startColumn()) * context.charWidth();
            if (width <= 0) {
                continue;
            }
            gc.setFill(matchIndex == context.currentSearchMatchIndex()
                ? context.theme().searchCurrentColor()
                : context.theme().searchHighlightColor());
            gc.fillRect(x, renderLine.y(), width, context.lineHeight());
        }
    }
}
