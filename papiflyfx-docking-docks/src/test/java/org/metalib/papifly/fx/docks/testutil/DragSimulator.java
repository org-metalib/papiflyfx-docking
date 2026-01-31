package org.metalib.papifly.fx.docks.testutil;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import org.metalib.papifly.fx.docks.DockManager;
import org.metalib.papifly.fx.docks.core.DockElement;
import org.metalib.papifly.fx.docks.core.DockLeaf;
import org.metalib.papifly.fx.docks.core.DockSplitGroup;
import org.metalib.papifly.fx.docks.core.DockTabGroup;
import org.metalib.papifly.fx.docks.drag.DragManager;
import org.metalib.papifly.fx.docks.testutil.Merge;
import org.metalib.papifly.fx.docks.testutil.Split;
import org.testfx.util.WaitForAsyncUtils;

import java.lang.reflect.Field;

/**
 * A test utility to build and execute a drag-and-drop operation for testing.
 * This encapsulates the creation of synthetic mouse events and the interaction
 * with the DragManager.
 */
public class DragSimulator {
    private final DockManager dockManager;
    private final DragManager dragManager;

    private DockLeaf leafToDrag;
    private Node startNode;
    private Point2D startPoint;
    private Point2D endPoint;

    public DragSimulator(DockManager dockManager) {
        this.dockManager = dockManager;
        this.dragManager = getDragManager(dockManager);
    }

    public DragSimulator from(DockLeaf leaf, Node startNode) {
        this.leafToDrag = leaf;
        this.startNode = startNode;
        this.startPoint = FxTestUtil.callFx(() -> {
            Bounds bounds = startNode.localToScene(startNode.getBoundsInLocal());
            return new Point2D(bounds.getMinX() + bounds.getWidth() / 2, bounds.getMinY() + bounds.getHeight() / 2);
        });
        return this;
    }

    public DragSimulator from(String leafTitle) {
        DockElement root = dockManager.getRoot();
        this.leafToDrag = findLeafByTitle(root, leafTitle);
        if (leafToDrag == null) {
            throw new IllegalArgumentException("Could not find leaf with title: " + leafTitle);
        }

        DockTabGroup parent = leafToDrag.getParent();
        this.startNode = parent != null ? findTabForLeaf(parent, leafToDrag) : null;
        if (this.startNode == null) {
            throw new IllegalStateException("Could not find UI node to drag for leaf: " + leafTitle);
        }

        this.startPoint = FxTestUtil.callFx(() -> {
            Bounds bounds = startNode.localToScene(startNode.getBoundsInLocal());
            return new Point2D(bounds.getMinX() + bounds.getWidth() / 2, bounds.getMinY() + bounds.getHeight() / 2);
        });

        return this;
    }

    public DragSimulator to(Point2D endPoint) {
        this.endPoint = endPoint;
        return this;
    }

    public DragSimulator to(DockTabGroup tabGroup, Split location) {
        this.endPoint = FxTestUtil.callFx(() -> {
            Node tabGroupNode = tabGroup.getNode();
            Bounds bounds = tabGroupNode.localToScene(tabGroupNode.getBoundsInLocal());
            switch (location) {
                case NORTH:
                    return new Point2D(bounds.getMinX() + bounds.getWidth() / 2, bounds.getMinY() + 2);
                case EAST:
                    return new Point2D(bounds.getMaxX() - 2, bounds.getMinY() + bounds.getHeight() / 2);
                case SOUTH:
                    return new Point2D(bounds.getMinX() + bounds.getWidth() / 2, bounds.getMaxY() - 2);
                case WEST:
                    return new Point2D(bounds.getMinX() + 2, bounds.getMinY() + bounds.getHeight() / 2);
                default:
                    throw new IllegalArgumentException("Unsupported split location: " + location);
            }
        });
        return this;
    }

    public DragSimulator to(String tabTitle, Split location) {
        DockElement root = dockManager.getRoot();
        DockTabGroup tabGroup = findTabGroupContaining(root, tabTitle);
        if (tabGroup == null) {
            throw new IllegalArgumentException("Could not find tab group containing title: " + tabTitle);
        }
        return to(tabGroup, location);
    }

    public DragSimulator to(DockTabGroup tabGroup, Merge location) {
        this.endPoint = FxTestUtil.callFx(() -> {
            Node targetNode;
            if (location == Merge.TAB_BAR) {
                targetNode = tabGroup.getTabBar();
            } else {
                // Assuming Merge enum might have other values for merging into the content area.
                targetNode = tabGroup.getNode();
            }
            Bounds bounds = targetNode.localToScene(targetNode.getBoundsInLocal());
            return new Point2D(bounds.getMinX() + bounds.getWidth() / 2, bounds.getMinY() + bounds.getHeight() / 2);
        });
        return this;
    }

    public DragSimulator to(String tabTitle, Merge location) {
        DockElement root = dockManager.getRoot();
        DockTabGroup tabGroup = findTabGroupContaining(root, tabTitle);
        if (tabGroup != null) {
            return to(tabGroup, location);
        }

        DockLeaf leaf = findLeafByTitle(root, tabTitle);
        if (leaf == null) {
            throw new IllegalArgumentException("Could not find tab group or leaf containing title: " + tabTitle);
        }

        return to(leaf, location);
    }

    public DragSimulator to(DockLeaf leaf, Merge location) {
        this.endPoint = FxTestUtil.callFx(() -> {
            DockTabGroup tabGroup = leaf.getParent();
            if (tabGroup == null) {
                throw new IllegalStateException("Leaf has no tab group parent: " + leaf.getMetadata().title());
            }
            Node targetNode = (location == Merge.TAB_BAR) ? tabGroup.getTabBar() : tabGroup.getNode();
            Bounds bounds = targetNode.localToScene(targetNode.getBoundsInLocal());
            return new Point2D(bounds.getMinX() + bounds.getWidth() / 2, bounds.getMinY() + bounds.getHeight() / 2);
        });
        return this;
    }

    public void execute() {
        // 1. Start drag
        FxTestUtil.runFx(() -> {
            MouseEvent pressEvent = createMouseEvent(MouseEvent.MOUSE_PRESSED,
                startNode, startPoint.getX(), startPoint.getY(), true);
            dragManager.startDrag(leafToDrag, pressEvent);
        });
        WaitForAsyncUtils.waitForFxEvents();

        // 2. Drag to target
        FxTestUtil.runFx(() -> {
            MouseEvent dragEvent = createMouseEvent(MouseEvent.MOUSE_DRAGGED,
                dockManager.getRootPane(), endPoint.getX(), endPoint.getY(), true);
            dragManager.onDrag(dragEvent);
        });
        WaitForAsyncUtils.waitForFxEvents();

        // 3. End drag
        FxTestUtil.runFx(() -> {
            MouseEvent releaseEvent = createMouseEvent(MouseEvent.MOUSE_RELEASED,
                dockManager.getRootPane(), endPoint.getX(), endPoint.getY(), false);
            dragManager.endDrag(releaseEvent);
        });
        WaitForAsyncUtils.waitForFxEvents();
    }

    /**
     * Creates a synthetic MouseEvent with the given parameters.
     */
    private MouseEvent createMouseEvent(javafx.event.EventType<MouseEvent> eventType, Node target,
                                        double sceneX, double sceneY, boolean primaryButtonDown) {
        Point2D screenPos = target.localToScreen(target.sceneToLocal(sceneX, sceneY));
        double screenX = screenPos != null ? screenPos.getX() : sceneX;
        double screenY = screenPos != null ? screenPos.getY() : sceneY;

        PickResult pickResult = new PickResult(target, sceneX, sceneY);

        return new MouseEvent(
            null,                    // source
            target,                  // target
            eventType,
            sceneX, sceneY,          // x, y (scene coordinates for DragManager calculations)
            screenX, screenY,        // screenX, screenY
            MouseButton.PRIMARY,
            1,                       // clickCount
            false, false, false, false,  // shift, ctrl, alt, meta
            primaryButtonDown,       // primaryButtonDown
            false, false,            // middleButtonDown, secondaryButtonDown
            true,                    // synthesized
            false,                   // popupTrigger
            true,                    // stillSincePress
            pickResult               // pickResult
        );
    }

    /**
     * Gets the DragManager from a DockManager using reflection.
     */
    private static DragManager getDragManager(DockManager dockManager) {
        try {
            Field field = DockManager.class.getDeclaredField("dragManager");
            field.setAccessible(true);
            return (DragManager) field.get(dockManager);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static DockLeaf findLeafByTitle(DockElement root, String title) {
        if (root == null) return null;

        if (root instanceof DockTabGroup) {
            for (DockLeaf tab : ((DockTabGroup) root).getTabs()) {
                if (title.equals(tab.getMetadata().title())) {
                    return tab;
                }
            }
        }

        if (root instanceof DockSplitGroup) {
            DockLeaf found = findLeafByTitle(((DockSplitGroup) root).getFirst(), title);
            if (found != null) return found;
            return findLeafByTitle(((DockSplitGroup) root).getSecond(), title);
        }

        return null;
    }

    private static Node findTabForLeaf(DockTabGroup tabGroup, DockLeaf leaf) {
        for (Node child : tabGroup.getTabsContainer().getChildrenUnmodifiable()) {
            if (child.getUserData() == leaf) {
                return child;
            }
        }
        return null;
    }

    private static DockTabGroup findTabGroupContaining(DockElement root, String title) {
        if (root == null) return null;

        if (root instanceof DockTabGroup) {
            DockTabGroup tabGroup = (DockTabGroup) root;
            boolean hasTitle = tabGroup.getTabs().stream()
                .anyMatch(t -> title.equals(t.getMetadata().title()));
            if (hasTitle) return tabGroup;
        }

        if (root instanceof DockSplitGroup) {
            DockSplitGroup split = (DockSplitGroup) root;
            DockTabGroup found = findTabGroupContaining(split.getFirst(), title);
            if (found != null) return found;
            return findTabGroupContaining(split.getSecond(), title);
        }

        return null;
    }
}
