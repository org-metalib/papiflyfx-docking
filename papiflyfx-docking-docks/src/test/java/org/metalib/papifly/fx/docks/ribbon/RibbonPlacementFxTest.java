package org.metalib.papifly.fx.docks.ribbon;

import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.metalib.papifly.fx.api.ribbon.RibbonButtonSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonCommand;
import org.metalib.papifly.fx.api.ribbon.RibbonContext;
import org.metalib.papifly.fx.api.ribbon.RibbonGroupSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonProvider;
import org.metalib.papifly.fx.api.ribbon.RibbonTabSpec;
import org.metalib.papifly.fx.docks.DockManager;
import org.metalib.papifly.fx.docks.core.DockTabGroup;
import org.metalib.papifly.fx.docks.testutil.FxTestUtil;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApplicationExtension.class)
class RibbonPlacementFxTest {

    private DockManager dockManager;
    private RibbonDockHost host;
    private Ribbon ribbon;

    @Start
    void start(Stage stage) {
        dockManager = new DockManager();
        DockTabGroup group = dockManager.createTabGroup();
        group.addLeaf(dockManager.createLeaf("Editor", new StackPane(new Label("Editor"))));
        dockManager.setRoot(group);

        RibbonManager ribbonManager = new RibbonManager(List.of(new PlacementProvider()));
        ribbonManager.addQuickAccessCommand(RibbonCommand.of("placement.save-all", "Save All", () -> {
        }));
        ribbon = new Ribbon(ribbonManager);
        host = new RibbonDockHost(dockManager, ribbon.getManager(), ribbon);
        stage.setScene(new Scene(host, 900, 520));
        stage.show();
        settle();
    }

    @Test
    void defaultPlacementIsTopAndUsesTopBorderRegion() {
        assertEquals(RibbonPlacement.TOP, FxTestUtil.callFx(host::getPlacement));
        assertEquals(RibbonPlacement.TOP, FxTestUtil.callFx(ribbon::getPlacement));
        assertSame(ribbon, FxTestUtil.callFx(host::getTop));
        assertSame(dockManager.getRootPane(), FxTestUtil.callFx(host::getCenter));
        assertTrue(FxTestUtil.callFx(() -> ribbon.getStyleClass().contains("pf-ribbon-placement-top")));
        assertTrue(FxTestUtil.callFx(() -> ribbon.getStyleClass().contains("pf-ribbon-orientation-horizontal")));
    }

    @Test
    void explicitPlacementsMoveRibbonWithoutMovingDockContent() {
        assertPlacement(RibbonPlacement.BOTTOM, BorderPane::getBottom);
        assertPlacement(RibbonPlacement.LEFT, BorderPane::getLeft);
        assertPlacement(RibbonPlacement.RIGHT, BorderPane::getRight);
        assertPlacement(RibbonPlacement.TOP, BorderPane::getTop);
    }

    @Test
    void sidePlacementUsesCompactRailAndFallbackGlyphs() {
        FxTestUtil.runFx(() -> host.setPlacement(RibbonPlacement.LEFT));
        settle();

        assertTrue(FxTestUtil.callFx(() -> ribbon.getStyleClass().contains("pf-ribbon-placement-left")));
        assertTrue(FxTestUtil.callFx(() -> ribbon.getStyleClass().contains("pf-ribbon-orientation-vertical")));
        assertTrue(FxTestUtil.callFx(() -> ribbon.lookup(".pf-ribbon-side-content-pane") == null));
        assertTrue(FxTestUtil.callFx(() -> ribbon.lookup(".pf-ribbon-collapse-button") == null));

        Button saveAll = FxTestUtil.callFx(() -> findDescendant(
            ribbon,
            Button.class,
            button -> "Save All".equals(button.getAccessibleText())
                && button.getStyleClass().contains("pf-ribbon-side-toolbar-command")
        ));
        assertEquals("", saveAll.getText());
        assertEquals("Save All", saveAll.getAccessibleText());
        assertTrue(FxTestUtil.callFx(() -> saveAll.getGraphic() != null));
        assertTrue(FxTestUtil.callFx(() -> saveAll.getTooltip() != null && "Save All".equals(saveAll.getTooltip().getText())));

        Button home = FxTestUtil.callFx(() -> findDescendant(
            ribbon,
            Button.class,
            button -> "Home".equals(button.getAccessibleText())
                && button.getStyleClass().contains("pf-ribbon-side-toolbar-tab")
        ));
        assertEquals("", home.getText());
        assertEquals("Home", home.getAccessibleText());
        assertTrue(FxTestUtil.callFx(() -> home.getGraphic() != null));
        assertTrue(FxTestUtil.callFx(() -> home.getTooltip() != null && "Home".equals(home.getTooltip().getText())));
    }

    @Test
    void sideActivationShowsTransientCommandPopover(FxRobot robot) {
        FxTestUtil.runFx(() -> host.setPlacement(RibbonPlacement.LEFT));
        settle();

        Button tools = robot.lookup(node ->
            node instanceof Button button
                && "Tools".equals(button.getAccessibleText())
                && button.getStyleClass().contains("pf-ribbon-side-toolbar-tab")
                && button.isVisible()
        ).queryAs(Button.class);
        robot.clickOn(tools);
        settle();

        assertFalse(FxTestUtil.callFx(ribbon::isMinimized));
        assertEquals("tools", FxTestUtil.callFx(ribbon::getSelectedTabId));
        Button command = robot.lookup(node ->
            node instanceof Button button
                && "Tools Action".equals(button.getText())
                && button.isVisible()
        ).queryAs(Button.class);
        assertEquals("Tools Action", FxTestUtil.callFx(command::getText));
    }

    @Test
    void clickingAnotherVerticalRibbonTabSwitchesTransientCommandPopover(FxRobot robot) {
        FxTestUtil.runFx(() -> host.setPlacement(RibbonPlacement.LEFT));
        settle();

        Button home = robot.lookup(node ->
            node instanceof Button button
                && "Home".equals(button.getAccessibleText())
                && button.getStyleClass().contains("pf-ribbon-side-toolbar-tab")
                && button.isVisible()
        ).queryAs(Button.class);
        Button tools = robot.lookup(node ->
            node instanceof Button button
                && "Tools".equals(button.getAccessibleText())
                && button.getStyleClass().contains("pf-ribbon-side-toolbar-tab")
                && button.isVisible()
        ).queryAs(Button.class);
        robot.clickOn(tools);
        settle();

        assertFalse(robot.lookup(node ->
            node instanceof Button button
                && "Tools Action".equals(button.getText())
                && button.isVisible()
        ).queryAll().isEmpty());

        robot.clickOn(home);
        settle();

        assertEquals("home", FxTestUtil.callFx(ribbon::getSelectedTabId));
        assertFalse(robot.lookup(node ->
            node instanceof Button button
                && "Home Action".equals(button.getText())
                && button.isVisible()
        ).queryAll().isEmpty());
        assertTrue(robot.lookup(node ->
            node instanceof Button button
                && "Tools Action".equals(button.getText())
                && button.isVisible()
        ).queryAll().isEmpty());
    }

    @Test
    void clickingLowerVerticalRibbonAreaHidesTransientCommandPopover(FxRobot robot) {
        FxTestUtil.runFx(() -> host.setPlacement(RibbonPlacement.LEFT));
        settle();

        Button tools = robot.lookup(node ->
            node instanceof Button button
                && "Tools".equals(button.getAccessibleText())
                && button.getStyleClass().contains("pf-ribbon-side-toolbar-tab")
                && button.isVisible()
        ).queryAs(Button.class);
        robot.clickOn(tools);
        settle();

        assertFalse(robot.lookup(node ->
            node instanceof Button button
                && "Tools Action".equals(button.getText())
                && button.isVisible()
        ).queryAll().isEmpty());

        Bounds ribbonBounds = FxTestUtil.callFx(() -> ribbon.localToScreen(ribbon.getBoundsInLocal()));
        robot.clickOn(ribbonBounds.getMinX() + ribbonBounds.getWidth() / 2.0, ribbonBounds.getMaxY() - 12.0);
        settle();

        assertTrue(robot.lookup(node ->
            node instanceof Button button
                && "Tools Action".equals(button.getText())
                && button.isVisible()
        ).queryAll().isEmpty());
    }

    @Test
    void minimizedSideActivationKeepsFlagAndSuppressesPopover(FxRobot robot) {
        FxTestUtil.runFx(() -> {
            host.setPlacement(RibbonPlacement.LEFT);
            ribbon.setMinimized(true);
        });
        settle();

        ScrollPane scroller = FxTestUtil.callFx(() -> findDescendant(ribbon, ScrollPane.class, pane -> true));
        assertTrue(scroller == null || !FxTestUtil.callFx(scroller::isVisible));

        Button tools = robot.lookup(node ->
            node instanceof Button button
                && "Tools".equals(button.getAccessibleText())
                && button.getStyleClass().contains("pf-ribbon-side-toolbar-tab")
                && button.isVisible()
        ).queryAs(Button.class);
        robot.clickOn(tools);
        settle();

        assertTrue(FxTestUtil.callFx(ribbon::isMinimized));
        assertTrue(scroller == null || !FxTestUtil.callFx(scroller::isVisible));
        assertEquals("tools", FxTestUtil.callFx(ribbon::getSelectedTabId));
        assertTrue(robot.lookup(node ->
            node instanceof Button button
                && "Tools Action".equals(button.getText())
                && button.isVisible()
        ).queryAll().isEmpty());
    }

    @Test
    void minimizedSideKeyboardActivationSuppressesPopover(FxRobot robot) {
        FxTestUtil.runFx(() -> {
            host.setPlacement(RibbonPlacement.RIGHT);
            ribbon.setMinimized(true);
        });
        settle();

        Button home = robot.lookup(node ->
            node instanceof Button button
                && "Home".equals(button.getAccessibleText())
                && button.getStyleClass().contains("pf-ribbon-side-toolbar-tab")
                && button.isVisible()
        ).queryAs(Button.class);
        FxTestUtil.runFx(home::requestFocus);
        robot.type(KeyCode.SPACE);
        settle();

        assertSame(ribbon, FxTestUtil.callFx(host::getRight));
        assertTrue(FxTestUtil.callFx(ribbon::isMinimized));
        assertEquals("home", FxTestUtil.callFx(ribbon::getSelectedTabId));
        assertTrue(robot.lookup(node ->
            node instanceof Button button
                && "Home Action".equals(button.getText())
                && button.isVisible()
        ).queryAll().isEmpty());
    }

    private void assertPlacement(RibbonPlacement placement, RegionAccessor accessor) {
        FxTestUtil.runFx(() -> host.setPlacement(placement));
        settle();

        assertEquals(placement, FxTestUtil.callFx(host::getPlacement));
        assertEquals(placement, FxTestUtil.callFx(ribbon::getPlacement));
        assertSame(ribbon, FxTestUtil.callFx(() -> accessor.region(host)));
        assertSame(dockManager.getRootPane(), FxTestUtil.callFx(host::getCenter));
    }

    private static void settle() {
        FxTestUtil.waitForFxEvents();
        FxTestUtil.waitForFxEvents();
    }

    private static <T extends Node> T findDescendant(javafx.scene.Parent root, Class<T> type, Predicate<T> predicate) {
        for (Node child : root.getChildrenUnmodifiable()) {
            if (type.isInstance(child)) {
                T cast = type.cast(child);
                if (predicate.test(cast)) {
                    return cast;
                }
            }
            if (child instanceof javafx.scene.Parent parent) {
                T match = findDescendant(parent, type, predicate);
                if (match != null) {
                    return match;
                }
            }
        }
        return null;
    }

    @FunctionalInterface
    private interface RegionAccessor {
        Node region(BorderPane pane);
    }

    private static final class PlacementProvider implements RibbonProvider {
        @Override
        public String id() {
            return "placement-provider";
        }

        @Override
        public List<RibbonTabSpec> getTabs(RibbonContext context) {
            return List.of(
                tab("home", "Home", "home-actions", "Home Action", 0),
                tab("tools", "Tools", "tools-actions", "Tools Action", 10)
            );
        }

        private RibbonTabSpec tab(String id, String label, String groupId, String commandLabel, int order) {
            return new RibbonTabSpec(
                id,
                label,
                order,
                false,
                ribbonContext -> true,
                List.of(new RibbonGroupSpec(
                    groupId,
                    label + " Group",
                    0,
                    0,
                    null,
                    List.of(new RibbonButtonSpec(RibbonCommand.of(id + ".command", commandLabel, () -> {
                    })))
                ))
            );
        }
    }
}
