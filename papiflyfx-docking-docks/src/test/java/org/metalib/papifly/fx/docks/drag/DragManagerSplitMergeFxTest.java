package org.metalib.papifly.fx.docks.drag;

import javafx.geometry.BoundingBox;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.metalib.papifly.fx.docks.DemoApp;
import org.metalib.papifly.fx.docks.DockManager;
import org.metalib.papifly.fx.docks.core.DockElement;
import org.metalib.papifly.fx.docks.core.DockLeaf;
import org.metalib.papifly.fx.docks.core.DockSplitGroup;
import org.metalib.papifly.fx.docks.core.DockTabGroup;
import org.metalib.papifly.fx.docks.testutil.DragSimulator;
import org.metalib.papifly.fx.docks.testutil.FxTestUtil;
import org.metalib.papifly.fx.docks.testutil.Merge;
import org.metalib.papifly.fx.docks.testutil.Split;
import org.metalib.papifly.fx.docks.render.OverlayCanvas;
import org.metalib.papifly.fx.docking.api.Theme;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests drag-and-drop split and merge operations using synthetic mouse events.
 */
@ExtendWith(ApplicationExtension.class)
class DragManagerSplitMergeFxTest {

    private static final double COORD_TOLERANCE = 1.0;

    private DockManager dockManager;
    private Stage stage;

    @Start
    private void start(Stage stage) {
        this.stage = stage;
        dockManager = new DockManager(Theme.dark());
        stage.setScene(new Scene(dockManager.getRootPane(), 1200, 800));
        stage.show();
    }

    /**
     * <pre>
     * Scenario: Splitting and merging "Editor 2" back into the same tab group
     *    Given "Editor 1" and "Editor 2" are open in the same tab group
     *     When I split "Editor 2" to a new tab group
     *      And I merge "Editor 2" back to the original tab group
     *     Then "Editor 1" and "Editor 2" should be in the same tab group
     * </pre>
     *
     * This test is equivalent to:
     * <pre>
     * new DragSimulator(dockManager)
     *     .from("Editor 2")
     *     .to("Editor 1", Split.EAST)
     *     .execute();
     *
     * new DragSimulator(dockManager)
     *     .from("Editor 2")
     *     .to("Editor 1", Merge.TAB_BAR)
     *     .execute();
     * </pre>
     * @param fxRobot
     */
    @Test
    void editor1AndEditor2_canSplitAndMergeBackIntoSameTabGroup(FxRobot fxRobot) {
        // Setup initial layout with Editor tabs
        fxRobot.interact(() -> dockManager.setRoot(DemoApp.createInitialLayout(dockManager)));
        WaitForAsyncUtils.waitForFxEvents();

        // --- SPLIT: Drag "Editor 2" to the right side of the tab group ---
        new DragSimulator(dockManager)
            .from("Editor 2")
            .to("Editor 1", Split.EAST)
            .execute();
        WaitForAsyncUtils.waitForFxEvents();

        // Verify split
        DockTabGroup tabGroupAfterSplit = FxTestUtil.callFx(() -> findTabGroupContaining(dockManager.getRoot(), "Editor 1"));
        List<String> tabTitlesAfterSplit = FxTestUtil.callFx(() ->
            tabGroupAfterSplit.getTabs().stream().map(t -> t.getMetadata().title()).toList());
        assertEquals(List.of("Editor 1"), tabTitlesAfterSplit, "Tab group should only have Editor 1 after split");

        // --- MERGE: Drag "Editor 2" back into the original tab group's tab bar ---
        new DragSimulator(dockManager)
            .from("Editor 2")
            .to("Editor 1", Merge.TAB_BAR)
            .execute();
        WaitForAsyncUtils.waitForFxEvents();

        // Verify merge
        DockTabGroup finalTabGroup = FxTestUtil.callFx(() -> findTabGroupContaining(dockManager.getRoot(), "Editor 1"));
        assertNotNull(finalTabGroup, "Tab group containing Editor 1 should exist");
        List<String> finalTitles = FxTestUtil.callFx(() ->
            finalTabGroup.getTabs().stream().map(t -> t.getMetadata().title()).toList());
        assertTrue(finalTitles.containsAll(List.of("Editor 1", "Editor 2")), "Final tab group should contain both editors");
        assertEquals(2, finalTitles.size(), "Final tab group should have exactly 2 tabs");
    }

    @Test
    void splitDragHintUsesOverlayLocalCoordinatesInOffsetContainer(FxRobot fxRobot) {
        fxRobot.interact(() -> {
            dockManager.setRoot(DemoApp.createInitialLayout(dockManager));
            installOffsetContainer(56, 130);
        });
        WaitForAsyncUtils.waitForFxEvents();

        DragManager dragManager = FxTestUtil.callFx(() -> getDragManager(dockManager));
        OverlayCanvas overlay = FxTestUtil.callFx(() -> getOverlayLayer(dockManager));
        DockLeaf sourceLeaf = FxTestUtil.callFx(() -> findLeafByTitle(dockManager.getRoot(), "Editor 2"));
        DockTabGroup targetGroup = FxTestUtil.callFx(() -> findTabGroupContaining(dockManager.getRoot(), "Editor 1"));
        assertNotNull(sourceLeaf, "Source leaf should exist");
        assertNotNull(targetGroup, "Target tab group should exist");

        Node sourceTab = FxTestUtil.callFx(() -> findTabForLeaf(sourceLeaf.getParent(), sourceLeaf));
        assertNotNull(sourceTab, "Source tab node should exist");

        Point2D startPoint = FxTestUtil.callFx(() -> centerOfInScene(sourceTab));
        Point2D endPoint = FxTestUtil.callFx(() -> eastEdgePointInScene(targetGroup));

        FxTestUtil.runFx(() -> dragManager.startDrag(sourceLeaf,
            createMouseEvent(MouseEvent.MOUSE_PRESSED, sourceTab, startPoint.getX(), startPoint.getY(), true)));
        WaitForAsyncUtils.waitForFxEvents();

        FxTestUtil.runFx(() -> dragManager.onDrag(
            createMouseEvent(MouseEvent.MOUSE_DRAGGED, dockManager.getRootPane(), endPoint.getX(), endPoint.getY(), true)));
        WaitForAsyncUtils.waitForFxEvents();

        HitTestResult overlayHint = FxTestUtil.callFx(() -> getCurrentOverlayHitResult(overlay));
        assertNotNull(overlayHint, "Overlay should hold the current drag hint");
        assertEquals(DropZone.EAST, overlayHint.zone(), "Drag should resolve to EAST split zone");

        Bounds expectedSceneBounds = FxTestUtil.callFx(() -> calculateEastZoneBoundsInScene(targetGroup));
        Bounds expectedLocalBounds = FxTestUtil.callFx(() -> sceneBoundsToOverlayLocal(overlay, expectedSceneBounds));
        assertBoundsClose(expectedLocalBounds, overlayHint.zoneBounds(), COORD_TOLERANCE);

        FxTestUtil.runFx(dragManager::cancelDrag);
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    void tabBarDragHintUsesOverlayLocalCoordinatesInOffsetContainer(FxRobot fxRobot) {
        fxRobot.interact(() -> {
            dockManager.setRoot(DemoApp.createInitialLayout(dockManager));
            installOffsetContainer(60, 125);
        });
        WaitForAsyncUtils.waitForFxEvents();

        DragManager dragManager = FxTestUtil.callFx(() -> getDragManager(dockManager));
        OverlayCanvas overlay = FxTestUtil.callFx(() -> getOverlayLayer(dockManager));
        DockLeaf sourceLeaf = FxTestUtil.callFx(() -> findLeafByTitle(dockManager.getRoot(), "Editor 2"));
        DockTabGroup targetGroup = FxTestUtil.callFx(() -> findTabGroupContaining(dockManager.getRoot(), "Editor 1"));
        assertNotNull(sourceLeaf, "Source leaf should exist");
        assertNotNull(targetGroup, "Target tab group should exist");

        Node sourceTab = FxTestUtil.callFx(() -> findTabForLeaf(sourceLeaf.getParent(), sourceLeaf));
        assertNotNull(sourceTab, "Source tab node should exist");

        Point2D startPoint = FxTestUtil.callFx(() -> centerOfInScene(sourceTab));
        Point2D endPoint = FxTestUtil.callFx(() -> tabBarPointInScene(targetGroup));

        FxTestUtil.runFx(() -> dragManager.startDrag(sourceLeaf,
            createMouseEvent(MouseEvent.MOUSE_PRESSED, sourceTab, startPoint.getX(), startPoint.getY(), true)));
        WaitForAsyncUtils.waitForFxEvents();

        FxTestUtil.runFx(() -> dragManager.onDrag(
            createMouseEvent(MouseEvent.MOUSE_DRAGGED, dockManager.getRootPane(), endPoint.getX(), endPoint.getY(), true)));
        WaitForAsyncUtils.waitForFxEvents();

        HitTestResult overlayHint = FxTestUtil.callFx(() -> getCurrentOverlayHitResult(overlay));
        assertNotNull(overlayHint, "Overlay should hold the current drag hint");
        assertEquals(DropZone.TAB_BAR, overlayHint.zone(), "Drag should resolve to TAB_BAR zone");

        Bounds tabBarSceneBounds = FxTestUtil.callFx(() -> targetGroup.getTabBar()
            .localToScene(targetGroup.getTabBar().getBoundsInLocal()));
        Bounds tabBarLocalBounds = FxTestUtil.callFx(() -> sceneBoundsToOverlayLocal(overlay, tabBarSceneBounds));
        assertBoundsClose(tabBarLocalBounds, overlayHint.zoneBounds(), COORD_TOLERANCE);
        assertTrue(
            overlayHint.tabInsertX() >= tabBarLocalBounds.getMinX() - COORD_TOLERANCE
                && overlayHint.tabInsertX() <= tabBarLocalBounds.getMaxX() + COORD_TOLERANCE,
            "Tab insertion X should be in overlay-local tab bar bounds"
        );

        FxTestUtil.runFx(dragManager::cancelDrag);
        WaitForAsyncUtils.waitForFxEvents();
    }

    private static DockTabGroup findTabGroupContaining(DockElement root, String title) {
        if (root == null) return null;

        if (root instanceof DockTabGroup tabGroup) {
            boolean hasTitle = tabGroup.getTabs().stream()
                .anyMatch(t -> title.equals(t.getMetadata().title()));
            if (hasTitle) return tabGroup;
        }

        if (root instanceof DockSplitGroup split) {
            DockTabGroup found = findTabGroupContaining(split.getFirst(), title);
            if (found != null) return found;
            return findTabGroupContaining(split.getSecond(), title);
        }

        return null;
    }

    /**
     * <pre>
     * Scenario: Splitting, merging  "Editor 2" back into the same tab group
     *    Given "Editor 1" and "Editor 2" are open in the same tab group
     *      and "Editor 2" is active tab
     *     When I split "Editor 1" with "Editor 2" dragging "Editor 1" to the east of "Editor 2"
     *      And I merge "Editor 2" with "Editor 1" dragging "Editor 2" to the center of "Editor 1"
     *      And I split "Editor 1" with "Editor 2" dragging "Editor 1" to the east of "Editor 2"
     *     Then "Editor 1" and "Editor 2" should be in the same tab group
     * </pre>
     *
     * @param fxRobot
     */
    @Test
    void editor1AndEditor2_canSplitAndMergeAndSplit(FxRobot fxRobot) {
        // Setup initial layout with Editor tabs
        fxRobot.interact(() -> dockManager.setRoot(DemoApp.createInitialLayout(dockManager)));
        WaitForAsyncUtils.waitForFxEvents();

        // --- SPLIT 1: Drag "Editor 1" to the east of "Editor 2" ---
        new DragSimulator(dockManager)
            .from("Editor 1")
            .to("Editor 2", Split.EAST)
            .execute();
        WaitForAsyncUtils.waitForFxEvents();

        // Verify first split
        DockTabGroup tabGroupAfterFirstSplit = FxTestUtil.callFx(() -> findTabGroupContaining(dockManager.getRoot(), "Editor 2"));
        List<String> tabTitlesAfterFirstSplit = FxTestUtil.callFx(() ->
            tabGroupAfterFirstSplit.getTabs().stream().map(t -> t.getMetadata().title()).toList());
        assertEquals(List.of("Editor 2"), tabTitlesAfterFirstSplit, "Tab group should only have Editor 2 after first split");

        // --- MERGE: Drag "Editor 2" to the center of "Editor 1" ---
        new DragSimulator(dockManager)
            .from("Editor 2")
            .to("Editor 1", Merge.TAB_BAR)
            .execute();
        WaitForAsyncUtils.waitForFxEvents();

        // Verify merge
        DockTabGroup tabGroupAfterMerge = FxTestUtil.callFx(() -> findTabGroupContaining(dockManager.getRoot(), "Editor 1"));
        assertNotNull(tabGroupAfterMerge, "Tab group containing Editor 1 should exist after merge");
        List<String> tabTitlesAfterMerge = FxTestUtil.callFx(() ->
            tabGroupAfterMerge.getTabs().stream().map(t -> t.getMetadata().title()).toList());
        assertTrue(tabTitlesAfterMerge.containsAll(List.of("Editor 1", "Editor 2")), "Tab group should contain both editors after merge");
        assertEquals(2, tabTitlesAfterMerge.size(), "Tab group should have exactly 2 tabs after merge");

        // --- SPLIT 2: Drag "Editor 1" to the east of "Editor 2" ---
        new DragSimulator(dockManager)
            .from("Editor 1")
            .to("Editor 2", Split.EAST)
            .execute();
        WaitForAsyncUtils.waitForFxEvents();

        // Verify second split

        DockTabGroup tabGroupEditor2 = FxTestUtil.callFx(() -> findTabGroupContaining(dockManager.getRoot(), "Editor 2"));
        DockTabGroup tabGroupEditor1 = FxTestUtil.callFx(() -> findTabGroupContaining(dockManager.getRoot(), "Editor 1"));
        DockLeaf editor1Leaf = FxTestUtil.callFx(() -> findLeafByTitle(dockManager.getRoot(), "Editor 1"));

        assertNotNull(tabGroupEditor2, "Tab group containing Editor 2 should exist after second split");
        assertNotNull(tabGroupEditor1, "Tab group containing Editor 1 should exist after second split");
        assertNotNull(editor1Leaf, "Editor 1 leaf should exist after second split");
        assertSame(tabGroupEditor1, editor1Leaf.getParent(), "Editor 1 leaf should be in its own tab group");

        int editor1Leaves = FxTestUtil.callFx(() -> countLeavesWithTitle(dockManager.getRoot(), "Editor 1"));
        int editor2Leaves = FxTestUtil.callFx(() -> countLeavesWithTitle(dockManager.getRoot(), "Editor 2"));
        assertEquals(1, editor1Leaves, "Final layout should have exactly one Editor 1 leaf");
        assertEquals(1, editor2Leaves, "Final layout should have exactly one Editor 2 leaf");
    }

    /**
     * <pre>
     * Scenario: Maximize and restore "Editor 2" back into the same tab group
     *    Given "Editor 1" and "Editor 2" are open in the same tab group
     *      and "Editor 2" is active tab
     *     When I maximize "Editor 2" by clicking the maximize button
     *      and I restore "Editor 2" by clicking the restore button
     *     Then "Editor 1" and "Editor 2" should be in the same tab group
     * </pre>
     *
     * @param fxRobot
     */
    @Test
    void editor1AndEditor2_maximize_minimize(FxRobot fxRobot) {
        // Setup initial layout with Editor tabs
        fxRobot.interact(() -> dockManager.setRoot(DemoApp.createInitialLayout(dockManager)));
        WaitForAsyncUtils.waitForFxEvents();

        // Verify initial state - both editors in same tab group
        DockTabGroup initialTabGroup = FxTestUtil.callFx(() -> findTabGroupContaining(dockManager.getRoot(), "Editor 2"));
        assertNotNull(initialTabGroup, "Initial tab group containing Editor 2 should exist");
        List<String> initialTitles = FxTestUtil.callFx(() ->
            initialTabGroup.getTabs().stream().map(t -> t.getMetadata().title()).toList());
        assertTrue(initialTitles.containsAll(List.of("Editor 1", "Editor 2")), "Initial tab group should contain both editors");

        // Find the maximize button in the tab group's tab bar
        // Tab bar structure: HBox(tabsContainer, spacer, buttonContainer)
        // buttonContainer: HBox(floatButton, minimizeButton, maximizeButton)
        Node maximizeButton = FxTestUtil.callFx(() -> {
            HBox tabBar = initialTabGroup.getTabBar();
            // buttonContainer is the last child of tabBar
            HBox buttonContainer = (HBox) tabBar.getChildren().getLast();
            // maximizeButton is the last child of buttonContainer
            return buttonContainer.getChildren().getLast();
        });
        assertNotNull(maximizeButton, "Maximize button should exist");

        // --- MAXIMIZE: Click the maximize button using synthetic event ---
        FxTestUtil.runFx(() -> fireMouseClick(maximizeButton));
        WaitForAsyncUtils.waitForFxEvents();

        // Verify maximized state
        boolean isMaximized = FxTestUtil.callFx(() -> dockManager.isMaximized());
        assertTrue(isMaximized, "Should be in maximized state after clicking maximize button");

        DockLeaf maximizedLeaf = FxTestUtil.callFx(() -> dockManager.getMaximizedLeaf());
        final var maximizedLeafContent = FxTestUtil.callFx(() -> maximizedLeaf.getContent());
        assertNotNull(maximizedLeaf, "Maximized leaf should exist");
        assertEquals("Editor 2", FxTestUtil.callFx(() -> maximizedLeaf.getMetadata().title()),
            "Editor 2 should be the maximized leaf (it was the active tab)");

        // --- RESTORE: Click the maximize/restore button again ---
        // When maximized, the active tab group is rendered full-screen with its own buttons
        Node restoreButton = FxTestUtil.callFx(() -> {
            DockLeaf leaf = dockManager.getMaximizedLeaf();
            DockTabGroup group = leaf.getParent();
            HBox tabBar = group.getTabBar();
            // buttonContainer is the last child of the tab bar
            HBox buttonContainer = (HBox) tabBar.getChildren().getLast();
            // maximizeButton is the last child of buttonContainer
            return buttonContainer.getChildren().getLast();
        });
        assertNotNull(restoreButton, "Restore button should exist on maximized leaf");

        FxTestUtil.runFx(() -> fireMouseClick(restoreButton));
        WaitForAsyncUtils.waitForFxEvents();

        // Verify restored state
        boolean isMaximizedAfterRestore = FxTestUtil.callFx(() -> dockManager.isMaximized());
        assertFalse(isMaximizedAfterRestore, "Should not be in maximized state after clicking restore button");

        // Verify both editors are back in the same tab group
        DockTabGroup finalTabGroup = FxTestUtil.callFx(() -> findTabGroupContaining(dockManager.getRoot(), "Editor 1"));
        assertNotNull(finalTabGroup, "Tab group containing Editor 1 should exist after restore");
        List<String> finalTitles = FxTestUtil.callFx(() ->
            finalTabGroup.getTabs().stream().map(t -> t.getMetadata().title()).toList());
        assertTrue(finalTitles.containsAll(List.of("Editor 1", "Editor 2")),
            "Final tab group should contain both editors after restore");
        assertEquals(2, finalTitles.size(), "Final tab group should have exactly 2 tabs");
    }

    @Test
    void singleTab_dragSplitOnSelf_isNoOp(FxRobot fxRobot) {
        FxTestUtil.runFx(() -> {
            DockLeaf solo = dockManager.createLeaf("Solo", new HBox());
            DockTabGroup tabGroup = dockManager.createTabGroup();
            tabGroup.addLeaf(solo);
            dockManager.setRoot(tabGroup);
        });
        WaitForAsyncUtils.waitForFxEvents();

        new DragSimulator(dockManager)
            .from("Solo")
            .to("Solo", Split.EAST)
            .execute();
        WaitForAsyncUtils.waitForFxEvents();

        DockElement root = FxTestUtil.callFx(dockManager::getRoot);
        assertTrue(root instanceof DockTabGroup, "Root should remain a tab group after no-op split");
        DockTabGroup tabGroup = (DockTabGroup) root;
        assertEquals(1, tabGroup.getTabs().size(), "Tab group should still have one tab");
        assertEquals("Solo", tabGroup.getTabs().getFirst().getMetadata().title());
    }

    /**
     * Fires a synthetic mouse click event on the given node.
     */
    private static void fireMouseClick(Node node) {
        javafx.geometry.Bounds bounds = node.localToScene(node.getBoundsInLocal());
        double sceneX = bounds.getMinX() + bounds.getWidth() / 2;
        double sceneY = bounds.getMinY() + bounds.getHeight() / 2;

        PickResult pickResult = new PickResult(node, sceneX, sceneY);

        MouseEvent clickEvent = new MouseEvent(
            null,                       // source
            node,                       // target
            MouseEvent.MOUSE_CLICKED,
            sceneX, sceneY,             // x, y (scene coordinates)
            sceneX, sceneY,             // screenX, screenY
            MouseButton.PRIMARY,
            1,                          // clickCount
            false, false, false, false, // shift, ctrl, alt, meta
            true,                       // primaryButtonDown
            false, false,               // middleButtonDown, secondaryButtonDown
            true,                       // synthesized
            false,                      // popupTrigger
            true,                       // stillSincePress
            pickResult
        );
        node.fireEvent(clickEvent);
    }

    private static MouseEvent createMouseEvent(
        javafx.event.EventType<MouseEvent> eventType,
        Node target,
        double sceneX,
        double sceneY,
        boolean primaryButtonDown
    ) {
        Point2D screenPos = target.localToScreen(target.sceneToLocal(sceneX, sceneY));
        double screenX = screenPos != null ? screenPos.getX() : sceneX;
        double screenY = screenPos != null ? screenPos.getY() : sceneY;
        PickResult pickResult = new PickResult(target, sceneX, sceneY);

        return new MouseEvent(
            null,
            target,
            eventType,
            sceneX,
            sceneY,
            screenX,
            screenY,
            MouseButton.PRIMARY,
            1,
            false,
            false,
            false,
            false,
            primaryButtonDown,
            false,
            false,
            true,
            false,
            true,
            pickResult
        );
    }

    private static DockLeaf findLeafByTitle(DockElement root, String title) {
        if (root == null) return null;
        if (root instanceof DockTabGroup tabGroup) {
            return tabGroup.getTabs().stream()
                .filter(t -> title.equals(t.getMetadata().title()))
                .findFirst()
                .orElse(null);
        }
        if (root instanceof DockSplitGroup split) {
            DockLeaf found = findLeafByTitle(split.getFirst(), title);
            if (found != null) return found;
            return findLeafByTitle(split.getSecond(), title);
        }
        return null;
    }

    private static int countLeavesWithTitle(DockElement root, String title) {
        if (root == null) return 0;
        if (root instanceof DockTabGroup tabGroup) {
            return (int) tabGroup.getTabs().stream()
                .filter(t -> title.equals(t.getMetadata().title()))
                .count();
        }
        if (root instanceof DockSplitGroup split) {
            return countLeavesWithTitle(split.getFirst(), title) + countLeavesWithTitle(split.getSecond(), title);
        }
        return 0;
    }

    private void installOffsetContainer(double topHeight, double leftWidth) {
        BorderPane container = new BorderPane();
        Region top = new Region();
        top.setPrefHeight(topHeight);
        Region left = new Region();
        left.setPrefWidth(leftWidth);
        container.setTop(top);
        container.setLeft(left);
        container.setCenter(dockManager.getRootPane());
        stage.getScene().setRoot(container);
        container.applyCss();
        container.layout();
    }

    private static Point2D centerOfInScene(Node node) {
        Bounds bounds = node.localToScene(node.getBoundsInLocal());
        return new Point2D(
            bounds.getMinX() + bounds.getWidth() * 0.5,
            bounds.getMinY() + bounds.getHeight() * 0.5
        );
    }

    private static Point2D eastEdgePointInScene(DockTabGroup tabGroup) {
        Bounds bounds = tabGroup.getNode().localToScene(tabGroup.getNode().getBoundsInLocal());
        return new Point2D(bounds.getMaxX() - 2.0, bounds.getMinY() + bounds.getHeight() * 0.5);
    }

    private static Point2D tabBarPointInScene(DockTabGroup tabGroup) {
        Bounds tabBarBounds = tabGroup.getTabBar().localToScene(tabGroup.getTabBar().getBoundsInLocal());
        return new Point2D(
            tabBarBounds.getMinX() + tabBarBounds.getWidth() * 0.25,
            tabBarBounds.getMinY() + tabBarBounds.getHeight() * 0.5
        );
    }

    private static Bounds calculateEastZoneBoundsInScene(DockTabGroup tabGroup) {
        Bounds tabBounds = tabGroup.getNode().localToScene(tabGroup.getNode().getBoundsInLocal());
        double tabBarHeight = tabGroup.getTabBar().getHeight();
        Bounds contentBounds = new BoundingBox(
            tabBounds.getMinX(),
            tabBounds.getMinY() + tabBarHeight,
            tabBounds.getWidth(),
            tabBounds.getHeight() - tabBarHeight
        );
        return new BoundingBox(
            contentBounds.getMinX() + contentBounds.getWidth() * 0.5,
            contentBounds.getMinY(),
            contentBounds.getWidth() * 0.5,
            contentBounds.getHeight()
        );
    }

    private static Bounds sceneBoundsToOverlayLocal(OverlayCanvas overlay, Bounds sceneBounds) {
        Point2D localMin = overlay.sceneToLocal(sceneBounds.getMinX(), sceneBounds.getMinY());
        Point2D localMax = overlay.sceneToLocal(sceneBounds.getMaxX(), sceneBounds.getMaxY());
        return new BoundingBox(
            Math.min(localMin.getX(), localMax.getX()),
            Math.min(localMin.getY(), localMax.getY()),
            Math.abs(localMax.getX() - localMin.getX()),
            Math.abs(localMax.getY() - localMin.getY())
        );
    }

    private static void assertBoundsClose(Bounds expected, Bounds actual, double tolerance) {
        assertEquals(expected.getMinX(), actual.getMinX(), tolerance, "minX should match");
        assertEquals(expected.getMinY(), actual.getMinY(), tolerance, "minY should match");
        assertEquals(expected.getWidth(), actual.getWidth(), tolerance, "width should match");
        assertEquals(expected.getHeight(), actual.getHeight(), tolerance, "height should match");
    }

    private static Node findTabForLeaf(DockTabGroup tabGroup, DockLeaf leaf) {
        for (Node child : tabGroup.getTabsContainer().getChildrenUnmodifiable()) {
            if (child.getUserData() == leaf) {
                return child;
            }
        }
        return null;
    }

    private static DragManager getDragManager(DockManager manager) {
        return readField(DockManager.class, manager, "dragManager", DragManager.class);
    }

    private static OverlayCanvas getOverlayLayer(DockManager manager) {
        return readField(DockManager.class, manager, "overlayLayer", OverlayCanvas.class);
    }

    private static HitTestResult getCurrentOverlayHitResult(OverlayCanvas overlay) {
        return readField(OverlayCanvas.class, overlay, "currentHitResult", HitTestResult.class);
    }

    private static <T> T readField(Class<?> type, Object instance, String fieldName, Class<T> valueType) {
        try {
            Field field = type.getDeclaredField(fieldName);
            field.setAccessible(true);
            return valueType.cast(field.get(instance));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

}
