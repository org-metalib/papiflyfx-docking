package org.metalib.papifly.fx.code.render;

import javafx.scene.canvas.GraphicsContext;

/**
 * Paints editor background and current-line highlight.
 */
final class BackgroundPass implements RenderPass {

    @Override
    public void renderFull(RenderContext context) {
        GraphicsContext gc = context.graphics();
        gc.setFill(context.theme().editorBackground());
        gc.fillRect(0, 0, context.viewportWidth(), context.viewportHeight());
        paintCurrentLineHighlight(context, context.selectionModel().getCaretLine(), context.lineToY(context.selectionModel().getCaretLine()));
    }

    @Override
    public void renderLine(RenderContext context, RenderLine renderLine) {
        GraphicsContext gc = context.graphics();
        gc.setFill(context.theme().editorBackground());
        gc.fillRect(0, renderLine.y(), context.viewportWidth(), context.lineHeight());
        paintCurrentLineHighlight(context, renderLine.lineIndex(), renderLine.y());
    }

    private void paintCurrentLineHighlight(RenderContext context, int lineIndex, double y) {
        if (lineIndex != context.selectionModel().getCaretLine() || context.selectionModel().hasSelection()) {
            return;
        }
        if (!context.isLineVisible(lineIndex)) {
            return;
        }
        GraphicsContext gc = context.graphics();
        gc.setFill(context.theme().currentLineColor());
        gc.fillRect(0, y, context.viewportWidth(), context.lineHeight());
    }
}
