package org.metalib.papifly.fx.tree.render;

import javafx.scene.canvas.GraphicsContext;

final class TreeConnectingLinesPass<T> implements TreeRenderPass<T> {

    @Override
    public void renderFull(TreeRenderContext<T> context) {
        GraphicsContext graphics = context.graphics();
        graphics.setStroke(context.theme().connectingLineColor());
        for (TreeRenderRow<T> row : context.rows()) {
            if (!row.isItemRow() || row.depth() <= 0) {
                continue;
            }
            double centerY = row.y() + (row.height() * 0.5);
            double indent = row.depth() * context.indentWidth() - context.horizontalScrollOffset();
            double parentIndent = indent - (context.indentWidth() * 0.5);
            graphics.strokeLine(parentIndent, centerY, indent, centerY);
            graphics.strokeLine(parentIndent, row.y(), parentIndent, row.y() + row.height());
        }
    }
}
