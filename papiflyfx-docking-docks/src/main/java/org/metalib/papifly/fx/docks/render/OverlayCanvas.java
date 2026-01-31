package org.metalib.papifly.fx.docks.render;

import javafx.geometry.Bounds;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import org.metalib.papifly.fx.docks.drag.DropZone;
import org.metalib.papifly.fx.docks.drag.HitTestResult;

/**
 * Transparent canvas overlay for rendering drag-and-drop visual feedback.
 * Provides distinct visual hints for different drop zones.
 */
public class OverlayCanvas extends Canvas {

    // Split zone colors (blue)
    private Paint splitHintColor = Color.rgb(0, 122, 204, 0.3);
    private Paint splitHintBorderColor = Color.rgb(0, 122, 204, 0.8);

    // Tab zone colors (green) - distinct from splits
    private Paint tabHintColor = Color.rgb(76, 175, 80, 0.3);
    private Paint tabHintBorderColor = Color.rgb(76, 175, 80, 0.8);

    // Tab bar indicator color (highlight strip)
    private Paint tabBarIndicatorColor = Color.rgb(76, 175, 80, 0.9);

    private HitTestResult currentHitResult;

    public OverlayCanvas() {
        // Canvas doesn't participate in mouse events
        setMouseTransparent(true);
    }

    /**
     * Sets the split zone hint color.
     */
    public void setSplitHintColor(Paint color) {
        this.splitHintColor = color;
    }

    /**
     * Sets the split zone hint border color.
     */
    public void setSplitHintBorderColor(Paint color) {
        this.splitHintBorderColor = color;
    }

    /**
     * Sets the tab zone hint color.
     */
    public void setTabHintColor(Paint color) {
        this.tabHintColor = color;
    }

    /**
     * Sets the tab zone hint border color.
     */
    public void setTabHintBorderColor(Paint color) {
        this.tabHintBorderColor = color;
    }

    /**
     * Shows a drop hint for the given hit test result.
     * Only redraws if the result has actually changed.
     */
    public void showDropHint(HitTestResult result) {
        if (result == null || !result.isHit()) {
            clearDropHint();
            return;
        }

        // Skip redraw if nothing changed (include tabInsertIndex for tab bar reordering)
        if (currentHitResult != null
            && currentHitResult.element() == result.element()
            && currentHitResult.zone() == result.zone()
            && currentHitResult.tabInsertIndex() == result.tabInsertIndex()) {
            return;
        }

        currentHitResult = result;
        redraw();
    }

    /**
     * Shows a drop hint at the specified bounds.
     * @deprecated Use {@link #showDropHint(HitTestResult)} instead
     */
    @Deprecated
    public void showDropHint(Bounds bounds) {
        if (bounds == null) {
            clearDropHint();
            return;
        }
        // Create a minimal HitTestResult for backwards compatibility with a safe zone
        currentHitResult = new HitTestResult(null, DropZone.CENTER, bounds);
        redraw();
    }

    /**
     * Clears any visible drop hint.
     */
    public void clearDropHint() {
        if (currentHitResult == null) {
            return; // Already clear, skip redraw
        }
        currentHitResult = null;
        clear();
    }

    private void redraw() {
        clear();

        if (currentHitResult == null || currentHitResult.zoneBounds() == null || currentHitResult.zone() == null) return;

        DropZone zone = currentHitResult.zone();
        Bounds bounds = currentHitResult.zoneBounds();
        GraphicsContext gc = getGraphicsContext2D();

        if (zone == DropZone.TAB_BAR) {
            // Tab bar: draw insertion indicator line
            drawTabBarIndicator(gc, bounds, currentHitResult.tabInsertIndex());
        } else if (zone == DropZone.CENTER) {
            // Center (tab add): use green/tab colors, draw with inset effect
            drawTabZoneHint(gc, bounds);
        } else {
            // Edge zones (NORTH, SOUTH, EAST, WEST): use blue/split colors
            drawSplitZoneHint(gc, bounds);
        }
    }

    private void drawSplitZoneHint(GraphicsContext gc, Bounds bounds) {
        // Fill
        gc.setFill(splitHintColor);
        gc.fillRoundRect(
            bounds.getMinX(),
            bounds.getMinY(),
            bounds.getWidth(),
            bounds.getHeight(),
            8, 8
        );

        // Border
        gc.setStroke(splitHintBorderColor);
        gc.setLineWidth(2);
        gc.strokeRoundRect(
            bounds.getMinX(),
            bounds.getMinY(),
            bounds.getWidth(),
            bounds.getHeight(),
            8, 8
        );
    }

    private void drawTabZoneHint(GraphicsContext gc, Bounds bounds) {
        // Fill with green for tab-add
        gc.setFill(tabHintColor);
        gc.fillRoundRect(
            bounds.getMinX(),
            bounds.getMinY(),
            bounds.getWidth(),
            bounds.getHeight(),
            8, 8
        );

        // Border
        gc.setStroke(tabHintBorderColor);
        gc.setLineWidth(2);
        gc.strokeRoundRect(
            bounds.getMinX(),
            bounds.getMinY(),
            bounds.getWidth(),
            bounds.getHeight(),
            8, 8
        );

        // Draw a small tab icon in center to indicate this is a tab-add
        double iconSize = Math.min(24, Math.min(bounds.getWidth(), bounds.getHeight()) * 0.2);
        double iconX = bounds.getMinX() + (bounds.getWidth() - iconSize) / 2;
        double iconY = bounds.getMinY() + (bounds.getHeight() - iconSize) / 2;

        gc.setStroke(tabHintBorderColor);
        gc.setLineWidth(2);
        // Draw simple "+" icon
        gc.strokeLine(iconX + iconSize / 2, iconY, iconX + iconSize / 2, iconY + iconSize);
        gc.strokeLine(iconX, iconY + iconSize / 2, iconX + iconSize, iconY + iconSize / 2);
    }

    private void drawTabBarIndicator(GraphicsContext gc, Bounds tabBarBounds, int insertIndex) {
        // Draw a subtle highlight on the tab bar
        gc.setFill(Color.rgb(76, 175, 80, 0.15));
        gc.fillRoundRect(
            tabBarBounds.getMinX(),
            tabBarBounds.getMinY(),
            tabBarBounds.getWidth(),
            tabBarBounds.getHeight(),
            4, 4
        );

        // Draw a vertical insertion line at the insert position
        double insertX = currentHitResult.tabInsertX();
        if (insertX >= 0) {
            gc.setStroke(tabBarIndicatorColor);
            gc.setLineWidth(3);
            gc.strokeLine(
                insertX,
                tabBarBounds.getMinY() + 2,
                insertX,
                tabBarBounds.getMaxY() - 2
            );

            // Draw small triangles at top and bottom of the line
            double triangleSize = 5;
            gc.setFill(tabBarIndicatorColor);

            // Top triangle (pointing down)
            gc.fillPolygon(
                new double[]{insertX - triangleSize, insertX + triangleSize, insertX},
                new double[]{tabBarBounds.getMinY(), tabBarBounds.getMinY(), tabBarBounds.getMinY() + triangleSize},
                3
            );

            // Bottom triangle (pointing up)
            gc.fillPolygon(
                new double[]{insertX - triangleSize, insertX + triangleSize, insertX},
                new double[]{tabBarBounds.getMaxY(), tabBarBounds.getMaxY(), tabBarBounds.getMaxY() - triangleSize},
                3
            );
        }
    }

    private void clear() {
        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, getWidth(), getHeight());
    }

    /**
     * Resizes the canvas to match the given dimensions.
     */
    public void resize(double width, double height) {
        setWidth(width);
        setHeight(height);
        if (currentHitResult != null) {
            redraw();
        }
    }
}
