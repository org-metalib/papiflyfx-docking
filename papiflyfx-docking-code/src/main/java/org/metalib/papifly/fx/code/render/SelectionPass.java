package org.metalib.papifly.fx.code.render;

import javafx.scene.canvas.GraphicsContext;
import org.metalib.papifly.fx.code.command.CaretRange;

/**
 * Paints primary and multi-caret selection overlays.
 */
final class SelectionPass implements RenderPass {

    @Override
    public void renderFull(RenderContext context) {
        if (!hasAnySelection(context)) {
            return;
        }
        GraphicsContext gc = context.graphics();
        gc.setFill(context.theme().selectionColor());
        for (RenderLine renderLine : context.renderLines()) {
            renderLine(context, renderLine);
        }
    }

    @Override
    public void renderLine(RenderContext context, RenderLine renderLine) {
        if (!hasAnySelection(context)) {
            return;
        }
        GraphicsContext gc = context.graphics();
        gc.setFill(context.theme().selectionColor());
        if (context.hasMultiCarets()) {
            for (CaretRange caret : context.activeCarets()) {
                if (caret.hasSelection()) {
                    paintRangeForLine(context, renderLine,
                        caret.getStartLine(), caret.getStartColumn(), caret.getEndLine(), caret.getEndColumn());
                }
            }
            return;
        }
        if (!context.selectionModel().hasSelection()) {
            return;
        }
        paintRangeForLine(context, renderLine,
            context.selectionModel().getSelectionStartLine(),
            context.selectionModel().getSelectionStartColumn(),
            context.selectionModel().getSelectionEndLine(),
            context.selectionModel().getSelectionEndColumn());
    }

    private boolean hasAnySelection(RenderContext context) {
        if (!context.hasMultiCarets()) {
            return context.selectionModel().hasSelection();
        }
        for (CaretRange caret : context.activeCarets()) {
            if (caret.hasSelection()) {
                return true;
            }
        }
        return false;
    }

    private void paintRangeForLine(
        RenderContext context,
        RenderLine renderLine,
        int startLine,
        int startCol,
        int endLine,
        int endCol
    ) {
        SelectionGeometry.SelectionSpan span = SelectionGeometry.spanForLine(
            renderLine.lineIndex(),
            context.viewportWidth(),
            context.charWidth(),
            startLine,
            startCol,
            endLine,
            endCol
        );
        if (span == null) {
            return;
        }
        context.graphics().fillRect(span.x(), renderLine.y(), span.width(), context.lineHeight());
    }
}
