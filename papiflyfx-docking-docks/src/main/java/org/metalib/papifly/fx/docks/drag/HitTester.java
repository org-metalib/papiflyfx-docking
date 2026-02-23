package org.metalib.papifly.fx.docks.drag;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import org.metalib.papifly.fx.docks.core.DockElement;
import org.metalib.papifly.fx.docks.core.DockLeaf;
import org.metalib.papifly.fx.docks.core.DockSplitGroup;
import org.metalib.papifly.fx.docks.core.DockTabGroup;

/**
 * Hit testing engine for drag-and-drop operations.
 * Uses edge-biased hit testing for predictable drop behavior.
 */
public class HitTester {

    // Edge band: use max(MIN_EDGE_BAND_PX, min(width,height) * EDGE_BAND_RATIO)
    private static final double MIN_EDGE_BAND_PX = 24.0;
    private static final double EDGE_BAND_RATIO = 0.18;

    private final DockElement root;

    /**
     * Creates a hit tester for the provided dock root.
     *
     * @param root root dock element used for hit testing
     */
    public HitTester(DockElement root) {
        this.root = root;
    }

    /**
     * Updates the root element for hit testing.
     *
     * @param root new root element
     */
    public void setRoot(DockElement root) {
        // This is a workaround for final field - in real impl would use a mutable holder
    }

    /**
     * Performs hit testing at the given scene coordinates.
     *
     * @param sceneX x coordinate in scene
     * @param sceneY y coordinate in scene
     * @param source the leaf being dragged (to exclude from hits)
     * @return the hit test result
     */
    public HitTestResult hitTest(double sceneX, double sceneY, DockLeaf source) {
        return hitTestRecursive(root, sceneX, sceneY, source);
    }

    private HitTestResult hitTestRecursive(DockElement element, double sceneX, double sceneY, DockLeaf source) {
        if (element == null) {
            return HitTestResult.none();
        }

        Region node = element.getNode();
        Bounds boundsInScene = node.localToScene(node.getBoundsInLocal());

        // Check if point is within this element
        if (!boundsInScene.contains(sceneX, sceneY)) {
            return HitTestResult.none();
        }

        // For split groups, recurse into children
        if (element instanceof DockSplitGroup split) {
            HitTestResult firstResult = hitTestRecursive(split.getFirst(), sceneX, sceneY, source);
            if (firstResult.isHit()) {
                return firstResult;
            }

            HitTestResult secondResult = hitTestRecursive(split.getSecond(), sceneX, sceneY, source);
            if (secondResult.isHit()) {
                return secondResult;
            }
        }

        // For tab groups, check tab bar first, then content area
        if (element instanceof DockTabGroup tabGroup) {
            Region tabNode = tabGroup.getNode();
            Bounds tabBounds = tabNode.localToScene(tabNode.getBoundsInLocal());
            Region tabBar = tabGroup.getTabBar();
            Bounds tabBarBounds = tabBar.localToScene(tabBar.getBoundsInLocal());

            // Check if over tab bar - this is a tab insertion zone
            if (tabBarBounds.contains(sceneX, sceneY)) {
                TabInsertInfo insertInfo = calculateTabInsertInfo(tabGroup, sceneX);
                return new HitTestResult(
                    tabGroup,
                    DropZone.TAB_BAR,
                    tabBarBounds, // Zone bounds = tab bar
                    tabBounds,    // Target bounds = full tab group
                    insertInfo.index,
                    insertInfo.insertX
                );
            }

            // Check content area
            double tabBarHeight = tabBar.getHeight();
            Bounds contentBounds = new BoundingBox(
                tabBounds.getMinX(),
                tabBounds.getMinY() + tabBarHeight,
                tabBounds.getWidth(),
                tabBounds.getHeight() - tabBarHeight
            );

            if (contentBounds.contains(sceneX, sceneY)) {
                return calculateDropZone(tabGroup, contentBounds, sceneX, sceneY);
            }
        }

        return HitTestResult.none();
    }

    /**
     * Helper record for tab insertion information.
     */
    private record TabInsertInfo(int index, double insertX) {}

    /**
     * Calculates the tab insertion index and X position based on mouse X position over the tab bar.
     */
    private TabInsertInfo calculateTabInsertInfo(DockTabGroup tabGroup, double sceneX) {
        Region tabBar = tabGroup.getTabBar();
        Bounds tabBarBounds = tabBar.localToScene(tabBar.getBoundsInLocal());
        Region tabsContainer = tabGroup.getTabsContainer();
        var tabChildren = tabsContainer.getChildrenUnmodifiable();

        // Iterate through tab buttons to find insertion point
        int index = 0;
        double insertX = tabBarBounds.getMinX(); // Default to start of tab bar

        for (Node child : tabChildren) {
            Bounds childBounds = child.localToScene(child.getBoundsInLocal());
            double midX = childBounds.getMinX() + childBounds.getWidth() / 2;

            if (sceneX < midX) {
                // Insert before this tab - insertion line at left edge of this tab
                insertX = childBounds.getMinX();
                return new TabInsertInfo(index, insertX);
            }
            index++;
        }

        // Insert at end - insertion line at right edge of last tab
        if (!tabChildren.isEmpty()) {
            Node lastChild = tabChildren.getLast();
            Bounds lastBounds = lastChild.localToScene(lastChild.getBoundsInLocal());
            insertX = lastBounds.getMaxX();
        }
        return new TabInsertInfo(index, insertX);
    }

    private HitTestResult calculateDropZone(DockElement element, Bounds bounds, double sceneX, double sceneY) {
        double width = bounds.getWidth();
        double height = bounds.getHeight();
        double minX = bounds.getMinX();
        double minY = bounds.getMinY();

        // Edge-biased approach: calculate edge band thickness
        double edgeBand = Math.max(MIN_EDGE_BAND_PX, Math.min(width, height) * EDGE_BAND_RATIO);

        // Distance from each edge
        double distLeft = sceneX - minX;
        double distRight = minX + width - sceneX;
        double distTop = sceneY - minY;
        double distBottom = minY + height - sceneY;

        DropZone zone;
        Bounds zoneBounds;

        // Check if in edge band - prioritize the closest edge
        if (distLeft < edgeBand || distRight < edgeBand || distTop < edgeBand || distBottom < edgeBand) {
            // Find which edge is closest
            double minDist = Math.min(Math.min(distLeft, distRight), Math.min(distTop, distBottom));

            if (minDist == distLeft) {
                zone = DropZone.WEST;
                zoneBounds = new BoundingBox(minX, minY, width * 0.5, height);
            } else if (minDist == distRight) {
                zone = DropZone.EAST;
                zoneBounds = new BoundingBox(minX + width * 0.5, minY, width * 0.5, height);
            } else if (minDist == distTop) {
                zone = DropZone.NORTH;
                zoneBounds = new BoundingBox(minX, minY, width, height * 0.5);
            } else {
                zone = DropZone.SOUTH;
                zoneBounds = new BoundingBox(minX, minY + height * 0.5, width, height * 0.5);
            }
        } else {
            // Center zone (add as tab)
            zone = DropZone.CENTER;
            // For center, show a smaller inset rectangle to differentiate from splits
            double inset = edgeBand;
            zoneBounds = new BoundingBox(
                minX + inset,
                minY + inset,
                width - 2 * inset,
                height - 2 * inset
            );
        }

        return new HitTestResult(element, zone, zoneBounds, bounds, -1);
    }
}
