package org.metalib.papifly.fx.docks.ribbon;

import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.metalib.papifly.fx.api.ribbon.MutableRibbonBooleanState;
import org.metalib.papifly.fx.api.ribbon.RibbonBooleanState;
import org.metalib.papifly.fx.api.ribbon.RibbonCommand;
import org.metalib.papifly.fx.api.ribbon.RibbonButtonSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonGroupSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonIconHandle;
import org.metalib.papifly.fx.api.ribbon.RibbonMenuSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonProvider;
import org.metalib.papifly.fx.api.ribbon.RibbonTabSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonToggleCommand;
import org.metalib.papifly.fx.api.ribbon.RibbonToggleSpec;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.docks.testutil.FxTestUtil;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApplicationExtension.class)
class RibbonAdaptiveLayoutFxTest {

    private static final byte[] SAMPLE_PNG_BYTES = Base64.getDecoder().decode(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAIAAACQd1PeAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
    );

    private final AtomicInteger alphaExecutions = new AtomicInteger();
    private final AtomicReference<List<RibbonTabSpec>> tabs = new AtomicReference<>();
    private final RibbonLayoutTelemetryRecorder telemetry = new RibbonLayoutTelemetryRecorder();

    private StackPane host;
    private Ribbon ribbon;
    private RibbonManager manager;

    @BeforeEach
    void resetProviderState() {
        tabs.set(defaultTabs());
        telemetry.clear();
        alphaExecutions.set(0);
    }

    @Start
    private void start(Stage stage) {
        if (tabs.get() == null || tabs.get().isEmpty()) {
            tabs.set(defaultTabs());
        }
        manager = new RibbonManager(List.of(new TestRibbonProvider()));
        ribbon = new Ribbon(manager);
        ribbon.setLayoutTelemetry(telemetry);
        host = new StackPane(ribbon);
        host.setAlignment(javafx.geometry.Pos.TOP_LEFT);
        host.setPrefSize(1200, 280);
        host.setMinSize(0, 0);
        ribbon.setMaxWidth(Double.MAX_VALUE);
        stage.setScene(new Scene(host, 1200, 280));
        stage.show();
        settleFx();
        telemetry.clear();
    }

    @Test
    void ribbonCollapseControlIsIconOnlyAndKeepsAccessibleState(FxRobot robot) {
        Button collapseButton = FxTestUtil.callFx(() -> findDescendant(
            ribbon,
            Button.class,
            button -> button.getStyleClass().contains("pf-ribbon-collapse-button")
        ));
        assertNotNull(collapseButton);

        assertEquals("", FxTestUtil.callFx(collapseButton::getText));
        assertEquals("Collapse ribbon", FxTestUtil.callFx(collapseButton::getAccessibleText));
        assertNotNull(FxTestUtil.callFx(collapseButton::getGraphic));
        assertNotNull(FxTestUtil.callFx(() -> findDescendant(
            assertInstanceOf(Parent.class, collapseButton.getGraphic()),
            SVGPath.class,
            icon -> icon.getStyleClass().contains("pf-ribbon-collapse-icon")
        )));

        robot.clickOn(collapseButton);
        settleFx();

        assertTrue(FxTestUtil.callFx(ribbon::isMinimized));
        assertEquals("", FxTestUtil.callFx(collapseButton::getText));
        assertEquals("Expand ribbon", FxTestUtil.callFx(collapseButton::getAccessibleText));
        assertNotNull(FxTestUtil.callFx(collapseButton::getGraphic));
    }

    @Test
    void minimizedTabActivationShowsRenderedCommandPanel(FxRobot robot) {
        FxTestUtil.runFx(() -> {
            tabs.set(twoTabRibbonTabs());
            manager.refresh();
        });
        settleFx();

        Button collapseButton = FxTestUtil.callFx(() -> findDescendant(
            ribbon,
            Button.class,
            button -> button.getStyleClass().contains("pf-ribbon-collapse-button")
        ));
        assertNotNull(collapseButton);
        ScrollPane groupScroller = FxTestUtil.callFx(() -> findDescendant(
            ribbon,
            ScrollPane.class,
            scroller -> scroller.getStyleClass().contains("pf-ribbon-scroll")
        ));
        assertNotNull(groupScroller);

        robot.clickOn(collapseButton);
        settleFx();

        assertTrue(FxTestUtil.callFx(ribbon::isMinimized));
        assertFalse(FxTestUtil.callFx(groupScroller::isVisible));

        ButtonBase toolsTab = robot.lookup(node ->
            node instanceof ButtonBase button
                && "Tools".equals(button.getText())
                && button.isVisible()
        ).queryAs(ButtonBase.class);
        robot.clickOn(toolsTab);
        settleFx();

        assertTrue(FxTestUtil.callFx(ribbon::isMinimized));
        assertTrue(FxTestUtil.callFx(groupScroller::isVisible));
        assertEquals("tools", FxTestUtil.callFx(ribbon::getSelectedTabId));
        assertTrue(FxTestUtil.callFx(() -> ribbon.getRenderedGroups().stream()
            .anyMatch(group -> group.getSpec().id().equals("delta"))));
        assertNotNull(robot.lookup(node ->
            node instanceof ButtonBase button
                && "Delta One".equals(button.getText())
                && button.isVisible()
        ).queryAs(ButtonBase.class));
    }

    @Test
    void collapsedGroupPopupKeepsCommandsReachable(FxRobot robot) {
        shrinkUntil(() -> group("alpha").getSizeMode() == RibbonGroupSizeMode.COLLAPSED, 560.0, 520.0, 480.0, 440.0, 400.0, 360.0, 320.0, 280.0);

        assertEquals(RibbonGroupSizeMode.COLLAPSED, group("alpha").getSizeMode());
        assertPriorityOrder();

        robot.clickOn("#pf-ribbon-group-collapsed-alpha");
        settleFx();

        assertFalse(robot.lookup(".pf-ribbon-collapsed-popup").queryAll().isEmpty());

        ButtonBase popupButton = robot.lookup(node ->
            node instanceof ButtonBase button
                && "Alpha One".equals(button.getText())
                && button.isVisible()
        ).queryAs(ButtonBase.class);

        robot.clickOn(popupButton);
        settleFx();

        assertEquals(1, alphaExecutions.get());
    }

    @Test
    void collapsedGroupPopupClosesOnEscapeAndRestoresFocus(FxRobot robot) {
        shrinkUntil(() -> group("alpha").getSizeMode() == RibbonGroupSizeMode.COLLAPSED, 560.0, 520.0, 480.0, 440.0, 400.0, 360.0, 320.0, 280.0);

        Button trigger = robot.lookup("#pf-ribbon-group-collapsed-alpha").queryAs(Button.class);
        robot.clickOn(trigger);
        settleFx();
        assertFalse(robot.lookup(".pf-ribbon-collapsed-popup").queryAll().isEmpty());

        robot.press(KeyCode.ESCAPE).release(KeyCode.ESCAPE);
        settleFx();

        assertTrue(robot.lookup(".pf-ribbon-collapsed-popup").queryAll().isEmpty());
        assertSame(trigger, FxTestUtil.callFx(() -> trigger.getScene().getFocusOwner()));
    }

    @Test
    void themeSwitchUpdatesLiveRibbonAndOpenCollapsedPopup(FxRobot robot) {
        shrinkUntil(() -> group("alpha").getSizeMode() == RibbonGroupSizeMode.COLLAPSED, 560.0, 520.0, 480.0, 440.0, 400.0, 360.0, 320.0, 280.0);
        robot.clickOn("#pf-ribbon-group-collapsed-alpha");
        settleFx();

        Parent popupRoot = robot.lookup(".pf-ribbon-collapsed-popup").queryAs(Parent.class);
        String darkRibbonStyle = FxTestUtil.callFx(ribbon::getStyle);
        String darkPopupStyle = FxTestUtil.callFx(popupRoot::getStyle);

        FxTestUtil.runFx(() -> ribbon.themeProperty().set(Theme.light()));
        settleFx();

        assertNotEquals(darkRibbonStyle, FxTestUtil.callFx(ribbon::getStyle));
        assertNotEquals(darkPopupStyle, FxTestUtil.callFx(popupRoot::getStyle));
    }

    @Test
    void menuPopupBackgroundTracksDarkAndLightRibbonThemes(FxRobot robot) {
        FxTestUtil.runFx(() -> {
            tabs.set(menuTabs());
            manager.refresh();
        });
        resizeTo(1200.0);

        MenuButton menuButton = robot.lookup(node ->
            node instanceof MenuButton button
                && "Options".equals(button.getText())
                && button.isVisible()
        ).queryAs(MenuButton.class);

        robot.clickOn(menuButton);
        settleFx();

        ContextMenu popup = FxTestUtil.callFx(() -> menuButton.getItems().getFirst().getParentPopup());
        assertNotNull(popup);
        String darkStyle = FxTestUtil.callFx(popup::getStyle);
        assertTrue(darkStyle.contains("rgba(45, 45, 45"), darkStyle);
        assertTrue(FxTestUtil.callFx(() -> popup.getStyleClass().contains("pf-ribbon-menu-popup")));

        Label optionLabel = robot.lookup(node ->
            node instanceof Label label
                && "Option One".equals(label.getText())
                && label.isVisible()
        ).queryAs(Label.class);
        Region menuItem = FxTestUtil.callFx(() ->
            assertInstanceOf(Region.class, findAncestor(optionLabel, parent -> parent.getStyleClass().contains("menu-item"))));
        String restingBackground = FxTestUtil.callFx(() -> backgroundDescription(menuItem));

        robot.moveTo(optionLabel);
        settleFx();

        String hoverBackground = FxTestUtil.callFx(() -> backgroundDescription(menuItem));
        assertNotEquals(restingBackground, hoverBackground);

        FxTestUtil.runFx(() -> ribbon.themeProperty().set(Theme.light()));
        settleFx();

        String lightStyle = FxTestUtil.callFx(popup::getStyle);
        assertTrue(lightStyle.contains("rgba(220, 220, 220"), lightStyle);
        assertNotEquals(darkStyle, lightStyle);

        Label optionTwoLabel = robot.lookup(node ->
            node instanceof Label label
                && "Option Two".equals(label.getText())
                && label.isVisible()
        ).queryAs(Label.class);
        Region lightMenuItem = FxTestUtil.callFx(() ->
            assertInstanceOf(Region.class, findAncestor(optionTwoLabel, parent -> parent.getStyleClass().contains("menu-item"))));
        String lightRestingBackground = FxTestUtil.callFx(() -> backgroundDescription(lightMenuItem));
        String lightRestingBorder = FxTestUtil.callFx(() -> borderDescription(lightMenuItem));

        robot.moveTo(optionTwoLabel);
        settleFx();

        String lightHoverBackground = FxTestUtil.callFx(() -> backgroundDescription(lightMenuItem));
        String lightHoverBorder = FxTestUtil.callFx(() -> borderDescription(lightMenuItem));
        assertNotEquals(lightRestingBackground, lightHoverBackground);
        assertNotEquals(lightRestingBorder, lightHoverBorder);
    }

    @Test
    void iconOnlyControlsExposeAccessibleNamesAndDisabledVectorIcons() {
        MutableRibbonBooleanState enabled = RibbonBooleanState.mutable(false);
        RibbonCommand command = RibbonCommand.of(
            "icon-only",
            "Icon Only",
            "Icon Only",
            RibbonIconHandle.of("octicon:sync"),
            null,
            enabled,
            () -> {
            }
        );

        FxTestUtil.runFx(() -> {
            tabs.set(iconOnlyTabs(command));
            manager.getQuickAccessCommandIds().setAll(command.id());
            manager.refresh();
        });
        settleFx();

        Button qatButton = FxTestUtil.callFx(() -> findDescendant(
            ribbon,
            Button.class,
            button -> button.getStyleClass().contains("pf-ribbon-qat-button")
        ));
        assertNotNull(qatButton);
        assertEquals("Icon Only", FxTestUtil.callFx(qatButton::getAccessibleText));
        assertTrue(FxTestUtil.callFx(qatButton::isDisabled));

        ButtonBase smallButton = FxTestUtil.callFx(() -> {
            ButtonBase button = (ButtonBase) RibbonControlFactory.createGroupControl(
                new RibbonButtonSpec(command),
                getClass().getClassLoader(),
                RibbonGroupSizeMode.SMALL
            );
            return button;
        });
        assertEquals("Icon Only", FxTestUtil.callFx(smallButton::getAccessibleText));
        FxTestUtil.runFx(() -> RibbonControlFactory.dispose(smallButton));

        SVGPath iconPath = FxTestUtil.callFx(() -> {
            host.applyCss();
            host.layout();
            assertInstanceOf(Parent.class, qatButton.getGraphic());
            return findDescendant(
                (Parent) qatButton.getGraphic(),
                SVGPath.class,
                path -> path.getStyleClass().contains("pf-ribbon-octicon")
            );
        });
        assertNotNull(iconPath);
        Paint disabledFill = FxTestUtil.callFx(iconPath::getFill);
        assertNotNull(disabledFill);

        FxTestUtil.runFx(() -> {
            enabled.set(true);
            host.applyCss();
            host.layout();
        });
        settleFx();

        Paint enabledFill = FxTestUtil.callFx(iconPath::getFill);
        assertNotNull(enabledFill);
        assertNotEquals(disabledFill, enabledFill);
    }

    @Test
    void groupsReturnToLargeModeWhenWidthExpandsAfterCollapse() {
        shrinkUntil(() -> group("alpha").getSizeMode() == RibbonGroupSizeMode.COLLAPSED, 560.0, 520.0, 480.0, 440.0, 400.0, 360.0, 320.0, 280.0);
        assertEquals(RibbonGroupSizeMode.COLLAPSED, group("alpha").getSizeMode());

        resizeTo(1200.0);

        assertEquals(RibbonGroupSizeMode.LARGE, group("alpha").getSizeMode());
        assertEquals(RibbonGroupSizeMode.LARGE, group("beta").getSizeMode());
        assertEquals(RibbonGroupSizeMode.LARGE, group("gamma").getSizeMode());
    }

    @Test
    void verticalPlacementUsesHeightForAdaptiveCollapseAndRestore() {
        FxTestUtil.runFx(() -> {
            ribbon.setPlacement(RibbonPlacement.LEFT);
            host.setPrefSize(360, 520);
            host.setMaxSize(360, 520);
            ribbon.setPrefSize(360, 520);
            ribbon.setMaxSize(360, 520);
            host.applyCss();
            host.layout();
        });
        settleFx();

        shrinkHeightUntil(() -> group("alpha").getSizeMode() == RibbonGroupSizeMode.COLLAPSED, 480.0, 420.0, 360.0, 300.0, 240.0, 190.0);
        assertEquals(RibbonGroupSizeMode.COLLAPSED, group("alpha").getSizeMode());
        assertPriorityOrder();
    }

    @Test
    void repeatedRefreshWithIdenticalTabsProducesOnlyCacheHits() {
        FxTestUtil.runFx(manager::refresh);
        settleFx();

        assertTrue(telemetry.tabRebuilds().isEmpty());
        assertTrue(telemetry.groupRebuilds().isEmpty());
        assertTrue(telemetry.controlRebuilds().isEmpty());
        assertTrue(telemetry.cacheHits().stream().anyMatch(event -> event.kind() == RibbonLayoutTelemetry.CacheKind.TAB));
        assertTrue(telemetry.cacheHits().stream().anyMatch(event -> event.kind() == RibbonLayoutTelemetry.CacheKind.GROUP));
        assertTrue(telemetry.cacheHits().stream().anyMatch(event -> event.kind() == RibbonLayoutTelemetry.CacheKind.CONTROL));
    }

    @Test
    void structuralChangeEmitsExactlyOneControlRebuild() {
        FxTestUtil.runFx(() -> tabs.set(structuralChangeTabs()));
        FxTestUtil.runFx(manager::refresh);
        settleFx();

        assertEquals(1, telemetry.controlRebuilds().stream()
            .filter(event -> event.reason() == RibbonLayoutTelemetry.RebuildReason.STRUCTURAL)
            .count());
        assertEquals(1, telemetry.controlRebuilds().stream()
            .filter(event -> "alpha-4".equals(event.controlId()))
            .filter(event -> event.reason() == RibbonLayoutTelemetry.RebuildReason.STRUCTURAL)
            .count());
    }

    @Test
    void collapseTransitionsShrinkThenRestoreInReverseOrderWithoutControlLoss() {
        shrinkUntil(() -> group("gamma").getSizeMode() == RibbonGroupSizeMode.COLLAPSED, 560.0, 520.0, 480.0, 440.0, 400.0, 360.0, 320.0, 280.0, 240.0, 220.0, 200.0, 180.0, 160.0);
        List<String> shrinkOrder = telemetry.collapseTransitions().stream()
            .map(RibbonLayoutTelemetryRecorder.CollapseTransitionEvent::groupId)
            .distinct()
            .toList();
        assertEquals(List.of("alpha", "beta", "gamma"), shrinkOrder);

        telemetry.clear();
        resizeTo(1200.0);

        List<String> growOrder = telemetry.collapseTransitions().stream()
            .map(RibbonLayoutTelemetryRecorder.CollapseTransitionEvent::groupId)
            .distinct()
            .toList();
        assertEquals(List.of("gamma", "beta", "alpha"), growOrder);
        assertEquals(3, group("alpha").getSpec().controls().size());
        assertEquals(3, group("beta").getSpec().controls().size());
        assertEquals(3, group("gamma").getSpec().controls().size());
    }

    @Test
    void sampleRibbonLabelsStayInsideControlsAndFooters() {
        FxTestUtil.runFx(() -> {
            tabs.set(sampleRibbonTabs());
            manager.refresh();
        });
        resizeTo(1200.0);

        assertEquals(RibbonGroupSizeMode.LARGE, group("clipboard").getSizeMode());
        assertEquals(RibbonGroupSizeMode.LARGE, group("layout").getSizeMode());
        assertCommandLabelContained("clipboard", "Paste");
        assertCommandLabelContained("clipboard", "Copy");
        assertCommandLabelContained("clipboard", "Duplicate");
        assertCommandLabelContained("layout", "Pin Preview");
        assertMenuButtonTextReadable("layout", "Presets");
        assertGroupCaptionContained("clipboard", "Clipboard");
        assertGroupCaptionContained("layout", "Layout");

        FxTestUtil.runFx(() -> {
            group("clipboard").setSizeMode(RibbonGroupSizeMode.MEDIUM);
            group("layout").setSizeMode(RibbonGroupSizeMode.MEDIUM);
            host.applyCss();
            host.layout();
        });
        settleFx();

        assertCommandLabelContained("clipboard", "Paste");
        assertCommandLabelContained("clipboard", "Copy");
        assertCommandLabelContained("clipboard", "Duplicate");
        assertCommandLabelContained("layout", "Pin Preview");
        assertMenuButtonTextReadable("layout", "Presets");
        assertGroupCaptionContained("clipboard", "Clipboard");
        assertGroupCaptionContained("layout", "Layout");
    }

    @Test
    void brokenSvgFallsBackToRasterWithoutThrowing() throws IOException {
        Path brokenSvg = Files.createTempFile("ribbon-icon", ".svg");
        Path rasterIcon = Files.createTempFile("ribbon-icon", ".png");
        try {
            Files.writeString(brokenSvg, "<svg><path d=\"broken", StandardCharsets.UTF_8);
            Files.write(rasterIcon, SAMPLE_PNG_BYTES);
            RibbonCommand command = RibbonCommand.of(
                "icon-test",
                "Icon Test",
                "Icon Test",
                RibbonIconHandle.of(brokenSvg.toUri().toString()),
                RibbonIconHandle.of(rasterIcon.toUri().toString()),
                null,
                () -> {
                }
            );

            Button button = FxTestUtil.callFx(() ->
                RibbonControlFactory.createQuickAccessButton(command, getClass().getClassLoader()));

            assertTrue(button.getGraphic() != null);
            assertInstanceOf(ImageView.class, button.getGraphic());
        } finally {
            Files.deleteIfExists(brokenSvg);
            Files.deleteIfExists(rasterIcon);
        }
    }

    private void assertPriorityOrder() {
        RibbonGroup alpha = group("alpha");
        RibbonGroup beta = group("beta");
        RibbonGroup gamma = group("gamma");

        assertTrue(alpha.getSizeMode().compareTo(beta.getSizeMode()) >= 0);
        assertTrue(beta.getSizeMode().compareTo(gamma.getSizeMode()) >= 0);
    }

    private void assertCommandLabelContained(String groupId, String label) {
        FxTestUtil.runFx(() -> {
            RibbonGroup ribbonGroup = group(groupId);
            ButtonBase control = findDescendant(
                ribbonGroup,
                ButtonBase.class,
                button -> label.equals(button.getText())
            );
            assertNotNull(control, "Expected command control for " + label);
            control.applyCss();
            control.layout();

            List<Text> labelTextNodes = labelTextNodes(control, label);
            String renderedText = normalizedRenderedText(control);
            assertFalse(
                labelTextNodes.isEmpty(),
                () -> "Expected command label text for " + label + " but rendered " + renderedText
            );
            labelTextNodes.forEach(text -> assertContained(control, text, "command label " + label));
        });
    }

    private void assertMenuButtonTextReadable(String groupId, String label) {
        FxTestUtil.runFx(() -> {
            RibbonGroup ribbonGroup = group(groupId);
            MenuButton control = findDescendant(
                ribbonGroup,
                MenuButton.class,
                button -> label.equals(button.getText())
            );
            assertNotNull(control, "Expected menu control for " + label);
            Text text = findDescendant(control, Text.class, node -> label.equals(node.getText()));
            assertNotNull(text, "Expected menu label text for " + label);
            control.applyCss();
            Paint fill = text.getFill();
            assertInstanceOf(Color.class, fill, "Expected CSS-resolved text fill for " + label);
            Color color = (Color) fill;
            assertTrue(
                color.getBrightness() >= 0.55,
                () -> "Expected readable menu text for " + label + " but got " + color
            );
        });
    }

    private void assertGroupCaptionContained(String groupId, String caption) {
        FxTestUtil.runFx(() -> {
            RibbonGroup ribbonGroup = group(groupId);
            Label label = findDescendant(
                ribbonGroup,
                Label.class,
                node -> caption.equals(node.getText()) && node.getStyleClass().contains("pf-ribbon-group-label")
            );
            assertNotNull(label, "Expected group caption for " + caption);
            Parent footer = findAncestor(label, node -> node.getStyleClass().contains("pf-ribbon-group-footer"));
            assertContained(footer, label, "group caption " + caption);
        });
    }

    private static void assertContained(Parent owner, Node child, String description) {
        Bounds ownerBounds = owner.localToScene(owner.getBoundsInLocal());
        Bounds childBounds = child.localToScene(child.getBoundsInLocal());
        double tolerance = 0.5;
        assertTrue(
            childBounds.getMinY() >= ownerBounds.getMinY() - tolerance
                && childBounds.getMaxY() <= ownerBounds.getMaxY() + tolerance,
            () -> description + " vertical bounds " + childBounds + " exceed owner bounds " + ownerBounds
        );
    }

    private static String backgroundDescription(Region region) {
        region.applyCss();
        return region.getBackground() == null ? "<none>" : region.getBackground().getFills().toString();
    }

    private static String borderDescription(Region region) {
        region.applyCss();
        return region.getBorder() == null ? "<none>" : region.getBorder().getStrokes().toString();
    }

    private static <T extends Node> T findDescendant(Parent root, Class<T> type, Predicate<T> predicate) {
        for (Node child : root.getChildrenUnmodifiable()) {
            if (type.isInstance(child)) {
                T cast = type.cast(child);
                if (predicate.test(cast)) {
                    return cast;
                }
            }
            if (child instanceof Parent parent) {
                T match = findDescendant(parent, type, predicate);
                if (match != null) {
                    return match;
                }
            }
        }
        return null;
    }

    private static List<Text> labelTextNodes(Parent root, String label) {
        List<Text> matches = new ArrayList<>();
        collectDescendants(root, Text.class, text -> {
            String value = normalizeWhitespace(text.getText());
            String expandedValue = removeEllipsis(value);
            return !expandedValue.isBlank()
                && (label.equals(value) || label.contains(value) || label.startsWith(expandedValue));
        }, matches);
        return matches;
    }

    private static String normalizedRenderedText(Parent root) {
        List<Text> texts = new ArrayList<>();
        collectDescendants(root, Text.class, text -> !normalizeWhitespace(text.getText()).isBlank(), texts);
        return normalizeWhitespace(String.join(" ", texts.stream().map(Text::getText).toList()));
    }

    private static <T extends Node> void collectDescendants(
        Parent root,
        Class<T> type,
        Predicate<T> predicate,
        List<T> matches
    ) {
        for (Node child : root.getChildrenUnmodifiable()) {
            if (type.isInstance(child)) {
                T cast = type.cast(child);
                if (predicate.test(cast)) {
                    matches.add(cast);
                }
            }
            if (child instanceof Parent parent) {
                collectDescendants(parent, type, predicate, matches);
            }
        }
    }

    private static String normalizeWhitespace(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }

    private static String removeEllipsis(String value) {
        return normalizeWhitespace(value)
            .replace("...", "")
            .replace("\u2026", "")
            .trim();
    }

    private static Parent findAncestor(Node child, Predicate<Parent> predicate) {
        Parent parent = child.getParent();
        while (parent != null) {
            if (predicate.test(parent)) {
                return parent;
            }
            parent = parent.getParent();
        }
        throw new AssertionError("No matching ancestor for " + child);
    }

    private RibbonGroup group(String id) {
        return FxTestUtil.callFx(() -> ribbon.getRenderedGroups().stream()
            .filter(group -> group.getSpec().id().equals(id))
            .findFirst()
            .orElseThrow());
    }

    private void resizeTo(double width) {
        FxTestUtil.runFx(() -> {
            host.setPrefWidth(width);
            host.setMaxWidth(width);
            ribbon.setPrefWidth(width);
            ribbon.setMaxWidth(width);
            host.applyCss();
            host.layout();
        });
        settleFx();
        FxTestUtil.runFx(() -> {
            host.applyCss();
            host.layout();
        });
        settleFx();
    }

    private void settleFx() {
        FxTestUtil.waitForFxEvents();
        FxTestUtil.waitForFxEvents();
    }

    private void shrinkUntil(BooleanSupplier condition, double... widths) {
        for (double width : widths) {
            resizeTo(width);
            if (condition.getAsBoolean()) {
                return;
            }
        }
        assertTrue(condition.getAsBoolean(), "Ribbon did not reach the expected adaptive state");
    }

    private void shrinkHeightUntil(BooleanSupplier condition, double... heights) {
        for (double height : heights) {
            resizeHeightTo(height);
            if (condition.getAsBoolean()) {
                return;
            }
        }
        assertTrue(condition.getAsBoolean(), "Ribbon did not reach the expected vertical adaptive state");
    }

    private void resizeHeightTo(double height) {
        FxTestUtil.runFx(() -> {
            host.setPrefHeight(height);
            host.setMaxHeight(height);
            ribbon.setPrefHeight(height);
            ribbon.setMaxHeight(height);
            host.applyCss();
            host.layout();
        });
        settleFx();
        FxTestUtil.runFx(() -> {
            host.applyCss();
            host.layout();
        });
        settleFx();
    }

    private List<RibbonTabSpec> defaultTabs() {
        return List.of(new RibbonTabSpec(
            "home",
            "Home",
            0,
            false,
            ribbonContext -> true,
            List.of(
                new RibbonGroupSpec(
                    "alpha",
                    "Alpha",
                    0,
                    0,
                    null,
                    List.of(
                        new RibbonButtonSpec(RibbonCommand.of("alpha-1", "Alpha One", "Alpha One", null, null, null, alphaExecutions::incrementAndGet)),
                        new RibbonButtonSpec(RibbonCommand.of("alpha-2", "Alpha Two", () -> {})),
                        new RibbonButtonSpec(RibbonCommand.of("alpha-3", "Alpha Three", () -> {}))
                    )
                ),
                new RibbonGroupSpec(
                    "beta",
                    "Beta",
                    10,
                    10,
                    null,
                    List.of(
                        new RibbonButtonSpec(RibbonCommand.of("beta-1", "Beta One", () -> {})),
                        new RibbonButtonSpec(RibbonCommand.of("beta-2", "Beta Two", () -> {})),
                        new RibbonButtonSpec(RibbonCommand.of("beta-3", "Beta Three", () -> {}))
                    )
                ),
                new RibbonGroupSpec(
                    "gamma",
                    "Gamma",
                    20,
                    20,
                    null,
                    List.of(
                        new RibbonButtonSpec(RibbonCommand.of("gamma-1", "Gamma One", () -> {})),
                        new RibbonButtonSpec(RibbonCommand.of("gamma-2", "Gamma Two", () -> {})),
                        new RibbonButtonSpec(RibbonCommand.of("gamma-3", "Gamma Three", () -> {}))
                    )
                )
            )
        ));
    }

    private List<RibbonTabSpec> sampleRibbonTabs() {
        return List.of(new RibbonTabSpec(
            "home",
            "Home",
            0,
            false,
            ribbonContext -> true,
            List.of(
                new RibbonGroupSpec(
                    "clipboard",
                    "Clipboard",
                    0,
                    10,
                    RibbonCommand.of("clipboard-settings", "Clipboard settings", () -> {}),
                    List.of(
                        new RibbonButtonSpec(RibbonCommand.of("paste", "Paste", () -> {})),
                        new RibbonButtonSpec(RibbonCommand.of("copy", "Copy", () -> {})),
                        new RibbonButtonSpec(RibbonCommand.of("duplicate", "Duplicate", () -> {}))
                    )
                ),
                new RibbonGroupSpec(
                    "layout",
                    "Layout",
                    10,
                    20,
                    null,
                    List.of(
                        new RibbonToggleSpec(RibbonToggleCommand.of("pin-preview", "Pin Preview", RibbonBooleanState.mutable(false), () -> {})),
                        new RibbonMenuSpec(
                            "presets",
                            "Presets",
                            "Presets",
                            null,
                            null,
                            List.of(
                                RibbonCommand.of("two-column", "Two Column", () -> {}),
                                RibbonCommand.of("wide-preview", "Wide Preview", () -> {})
                            )
                        )
                    )
                )
            )
        ));
    }

    private List<RibbonTabSpec> twoTabRibbonTabs() {
        RibbonTabSpec home = defaultTabs().getFirst();
        RibbonTabSpec tools = new RibbonTabSpec(
            "tools",
            "Tools",
            10,
            false,
            ribbonContext -> true,
            List.of(new RibbonGroupSpec(
                "delta",
                "Delta",
                0,
                0,
                null,
                List.of(new RibbonButtonSpec(RibbonCommand.of("delta-1", "Delta One", () -> {})))
            ))
        );
        return List.of(home, tools);
    }

    private List<RibbonTabSpec> menuTabs() {
        return List.of(new RibbonTabSpec(
            "home",
            "Home",
            0,
            false,
            ribbonContext -> true,
            List.of(new RibbonGroupSpec(
                "menus",
                "Menus",
                0,
                0,
                null,
                List.of(new RibbonMenuSpec(
                    "options-menu",
                    "Options",
                    "Options",
                    null,
                    null,
                    List.of(
                        RibbonCommand.of("options-one", "Option One", () -> {}),
                        RibbonCommand.of("options-two", "Option Two", () -> {})
                    )
                ))
            ))
        ));
    }

    private List<RibbonTabSpec> iconOnlyTabs(RibbonCommand command) {
        return List.of(new RibbonTabSpec(
            "home",
            "Home",
            0,
            false,
            ribbonContext -> true,
            List.of(new RibbonGroupSpec(
                "icons",
                "Icons",
                0,
                0,
                null,
                List.of(new RibbonButtonSpec(command))
            ))
        ));
    }

    private List<RibbonTabSpec> structuralChangeTabs() {
        RibbonTabSpec baseline = defaultTabs().getFirst();
        return List.of(new RibbonTabSpec(
            baseline.id(),
            baseline.label(),
            baseline.order(),
            baseline.contextual(),
            ribbonContext -> true,
            List.of(
                new RibbonGroupSpec(
                    "alpha",
                    "Alpha",
                    0,
                    0,
                    null,
                    List.of(
                        new RibbonButtonSpec(RibbonCommand.of("alpha-1", "Alpha One", "Alpha One", null, null, null, alphaExecutions::incrementAndGet)),
                        new RibbonButtonSpec(RibbonCommand.of("alpha-2", "Alpha Two", () -> {})),
                        new RibbonButtonSpec(RibbonCommand.of("alpha-3", "Alpha Three", () -> {})),
                        new RibbonButtonSpec(RibbonCommand.of("alpha-4", "Alpha Four", () -> {}))
                    )
                ),
                baseline.groups().get(1),
                baseline.groups().get(2)
            )
        ));
    }

    private final class TestRibbonProvider implements RibbonProvider {

        @Override
        public String id() {
            return "test-ribbon-provider";
        }

        @Override
        public List<RibbonTabSpec> getTabs(org.metalib.papifly.fx.api.ribbon.RibbonContext context) {
            return tabs.get();
        }
    }
}
