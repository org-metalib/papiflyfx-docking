package org.metalib.papifly.fx.tree.render;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Paint;
import org.metalib.papifly.fx.tree.api.TreeItem;

final class TreeBackgroundPass<T> implements TreeRenderPass<T> {

    @Override
    public void renderFull(TreeRenderContext<T> context) {
        GraphicsContext graphics = context.graphics();
        graphics.setFill(context.theme().background());
        graphics.fillRect(0, 0, context.viewportWidth(), context.viewportHeight());
        for (TreeRenderRow<T> row : context.rows()) {
            paintRow(context, row);
        }
    }

    @Override
    public void renderRow(TreeRenderContext<T> context, TreeRenderRow<T> row) {
        paintRow(context, row);
    }

    private void paintRow(TreeRenderContext<T> context, TreeRenderRow<T> row) {
        GraphicsContext graphics = context.graphics();
        TreeItem<T> item = row.item();
        Paint rowPaint = row.rowIndex() % 2 == 0
            ? context.theme().rowBackground()
            : context.theme().rowBackgroundAlternate();
        if (context.selectionModel().isSelected(item)) {
            if (context.selectionModel().getFocusedItem() == item) {
                rowPaint = context.theme().selectedBackground();
            } else {
                rowPaint = context.theme().selectedBackgroundUnfocused();
            }
        } else if (context.hoveredItem() == item) {
            rowPaint = context.theme().hoverBackground();
        }
        graphics.setFill(rowPaint);
        graphics.fillRect(0, row.y(), context.effectiveTextWidth(), row.height());

        if (context.selectionModel().getFocusedItem() == item) {
            graphics.setStroke(context.theme().focusedBorder());
            graphics.strokeRect(0.5, row.y() + 0.5, Math.max(0, context.effectiveTextWidth() - 1), Math.max(0, row.height() - 1));
        }
    }
}
