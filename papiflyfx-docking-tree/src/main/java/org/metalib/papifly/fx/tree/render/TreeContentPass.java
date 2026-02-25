package org.metalib.papifly.fx.tree.render;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Paint;
import org.metalib.papifly.fx.tree.api.CellState;
import org.metalib.papifly.fx.tree.api.TreeItem;

final class TreeContentPass<T> implements TreeRenderPass<T> {

    @Override
    public void renderFull(TreeRenderContext<T> context) {
        GraphicsContext graphics = context.graphics();
        graphics.setFont(context.theme().font());
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
        drawDisclosure(context, row);

        double iconX = row.depth() * context.indentWidth() + context.indentWidth() + 2.0 - context.horizontalScrollOffset();
        double iconY = row.y() + ((row.height() - context.iconSize()) * 0.5);
        drawIcon(context, item, iconX, iconY);

        double textX = context.textOriginX(row);
        double textY = row.y() + ((row.height() - context.glyphCache().getLineHeight()) * 0.5) + context.baseline();
        double textWidth = Math.max(0.0, context.effectiveTextWidth() - textX);
        double textHeight = row.height();
        CellState state = new CellState(
            context.selectionModel().isSelected(item),
            context.selectionModel().getFocusedItem() == item,
            context.hoveredItem() == item,
            row.expanded(),
            row.leaf(),
            row.depth(),
            textX,
            row.y(),
            textWidth,
            textHeight
        );

        if (context.cellRenderer() != null) {
            context.cellRenderer().render(graphics, item.getValue(), context, state);
            return;
        }
        Paint textPaint = context.selectionModel().isSelected(item)
            ? context.theme().textColorSelected()
            : context.theme().textColor();
        graphics.setFill(textPaint);
        graphics.fillText(String.valueOf(item.getValue()), textX, textY);
    }

    private void drawIcon(TreeRenderContext<T> context, TreeItem<T> item, double x, double y) {
        if (context.iconResolver() == null) {
            return;
        }
        T value = item.getValue();
        Image image = context.iconResolver().apply(value);
        if (image == null) {
            return;
        }
        context.graphics().drawImage(image, x, y, context.iconSize(), context.iconSize());
    }

    private void drawDisclosure(TreeRenderContext<T> context, TreeRenderRow<T> row) {
        if (row.leaf()) {
            return;
        }
        GraphicsContext graphics = context.graphics();
        double centerX = context.disclosureOriginX(row);
        double centerY = row.y() + (row.height() * 0.5);
        double size = Math.min(8.0, row.height() * 0.4);
        graphics.setFill(context.theme().disclosureColor());
        if (row.expanded()) {
            graphics.fillPolygon(
                new double[] {centerX - size * 0.5, centerX + size * 0.5, centerX},
                new double[] {centerY - size * 0.25, centerY - size * 0.25, centerY + size * 0.5},
                3
            );
        } else {
            graphics.fillPolygon(
                new double[] {centerX - size * 0.25, centerX - size * 0.25, centerX + size * 0.5},
                new double[] {centerY - size * 0.5, centerY + size * 0.5, centerY},
                3
            );
        }
    }
}
