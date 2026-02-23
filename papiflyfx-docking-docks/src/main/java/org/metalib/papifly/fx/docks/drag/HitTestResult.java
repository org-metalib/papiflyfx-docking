package org.metalib.papifly.fx.docks.drag;

import javafx.geometry.Bounds;
import org.metalib.papifly.fx.docks.core.DockElement;

/**
 * Result of a hit test operation.
 *
 * @param element      The target dock element under the mouse
 * @param zone         The drop zone within the target (CENTER, NORTH, SOUTH, EAST, WEST)
 * @param zoneBounds   The bounds of the drop zone area (what will be highlighted)
 * @param targetBounds The full bounds of the target element in scene coordinates
 * @param tabInsertIndex For TAB_BAR zone, the index where a tab should be inserted (-1 otherwise)
 * @param tabInsertX   For TAB_BAR zone, the X position where the insertion line should be drawn
 */
public record HitTestResult(
    DockElement element,
    DropZone zone,
    Bounds zoneBounds,
    Bounds targetBounds,
    int tabInsertIndex,
    double tabInsertX
) {
    /**
     * Constructor without insertion X (for non-tab-bar zones).
     *
     * @param element target dock element
     * @param zone drop zone within target
     * @param zoneBounds bounds of the highlighted zone
     * @param targetBounds bounds of the full target
     * @param tabInsertIndex tab insertion index for tab-bar drops
     */
    public HitTestResult(DockElement element, DropZone zone, Bounds zoneBounds, Bounds targetBounds, int tabInsertIndex) {
        this(element, zone, zoneBounds, targetBounds, tabInsertIndex, -1);
    }

    /**
     * Compact constructor for backwards compatibility.
     *
     * @param element target dock element
     * @param zone drop zone within target
     * @param zoneBounds bounds of the highlighted zone
     */
    public HitTestResult(DockElement element, DropZone zone, Bounds zoneBounds) {
        this(element, zone, zoneBounds, zoneBounds, -1, -1);
    }

    /**
     * Creates an empty (no hit) result.
     *
     * @return empty hit result
     */
    public static HitTestResult none() {
        return new HitTestResult(null, DropZone.NONE, null, null, -1, -1);
    }

    /**
     * Checks if this result represents a valid hit.
     *
     * @return {@code true} when this result represents a valid drop target
     */
    public boolean isHit() {
        return element != null && zone != DropZone.NONE;
    }

    /**
     * Checks if cursor is near an edge (directional drop zones).
     *
     * @return {@code true} when zone is one of the directional edge zones
     */
    public boolean isNearEdge() {
        return zone == DropZone.NORTH || zone == DropZone.SOUTH
            || zone == DropZone.EAST || zone == DropZone.WEST;
    }

    /**
     * Checks if this is a tab-add (center) drop.
     *
     * @return {@code true} when zone adds or inserts a tab
     */
    public boolean isTabDrop() {
        return zone == DropZone.CENTER || zone == DropZone.TAB_BAR;
    }
}
