package org.metalib.papifly.fx.code.render;

import javafx.scene.canvas.GraphicsContext;
import org.metalib.papifly.fx.code.command.CaretRange;

/**
 * Paints primary and multi-caret insertion carets.
 */
final class CaretPass implements RenderPass {

    private static final double CARET_WIDTH = 2.0;

    @Override
    public void renderFull(RenderContext context) {
        if (!context.paintCaret()) {
            return;
        }
        if (context.hasMultiCarets()) {
            GraphicsContext gc = context.graphics();
            gc.setFill(context.theme().caretColor());
            for (CaretRange caret : context.activeCarets()) {
                if (!context.isLineVisible(caret.caretLine())) {
                    continue;
                }
                gc.fillRect(
                    caret.caretColumn() * context.charWidth(),
                    context.lineToY(caret.caretLine()),
                    CARET_WIDTH,
                    context.lineHeight()
                );
            }
            return;
        }
        int caretLine = context.selectionModel().getCaretLine();
        if (!context.isLineVisible(caretLine)) {
            return;
        }
        GraphicsContext gc = context.graphics();
        gc.setFill(context.theme().caretColor());
        gc.fillRect(
            context.selectionModel().getCaretColumn() * context.charWidth(),
            context.lineToY(caretLine),
            CARET_WIDTH,
            context.lineHeight()
        );
    }

    @Override
    public void renderLine(RenderContext context, RenderLine renderLine) {
        if (!context.paintCaret()) {
            return;
        }
        GraphicsContext gc = context.graphics();
        if (context.hasMultiCarets()) {
            gc.setFill(context.theme().caretColor());
            for (CaretRange caret : context.activeCarets()) {
                if (renderLine.lineIndex() == caret.caretLine()) {
                    gc.fillRect(
                        caret.caretColumn() * context.charWidth(),
                        renderLine.y(),
                        CARET_WIDTH,
                        context.lineHeight()
                    );
                }
            }
            return;
        }
        if (renderLine.lineIndex() != context.selectionModel().getCaretLine()) {
            return;
        }
        gc.setFill(context.theme().caretColor());
        gc.fillRect(
            context.selectionModel().getCaretColumn() * context.charWidth(),
            renderLine.y(),
            CARET_WIDTH,
            context.lineHeight()
        );
    }
}
