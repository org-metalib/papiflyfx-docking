package org.metalib.papifly.fx.docks.ribbon;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.metalib.papifly.fx.api.ribbon.MutableRibbonBooleanState;
import org.metalib.papifly.fx.api.ribbon.RibbonBooleanState;
import org.metalib.papifly.fx.api.ribbon.RibbonCommand;
import org.metalib.papifly.fx.api.ribbon.RibbonAttributeContributor;
import org.metalib.papifly.fx.api.ribbon.RibbonAttributeKey;
import org.metalib.papifly.fx.api.ribbon.RibbonButtonSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonCapabilityContributor;
import org.metalib.papifly.fx.api.ribbon.RibbonContext;
import org.metalib.papifly.fx.api.ribbon.RibbonContextAttributes;
import org.metalib.papifly.fx.api.ribbon.RibbonGroupSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonProvider;
import org.metalib.papifly.fx.api.ribbon.RibbonTabSpec;
import org.metalib.papifly.fx.docking.api.LeafContentData;
import org.metalib.papifly.fx.docks.DockManager;
import org.metalib.papifly.fx.docks.core.DockLeaf;
import org.metalib.papifly.fx.docks.core.DockTabGroup;
import org.metalib.papifly.fx.docks.testutil.FxTestUtil;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.metalib.papifly.fx.docks.ribbon.RibbonTestSupport.settleFx;

@ExtendWith(ApplicationExtension.class)
class RibbonFloatingContextFxTest {

    private static final String FLOATING_TYPE = "phase2.floating";
    private static final String FLOATING_CONTENT_ID = "phase2://floating";
    private static final String FLOATING_COMMAND_ID = "phase2.ribbon.floating.action";

    private DockManager dockManager;
    private RibbonManager ribbonManager;
    private DockLeaf floatingLeaf;
    private FloatingContent floatingContent;

    @Start
    void start(Stage stage) {
        dockManager = new DockManager();

        DockTabGroup group = dockManager.createTabGroup();
        DockLeaf plainLeaf = dockManager.createLeaf("Plain", content("plain-content", "Plain"));
        plainLeaf.setContentData(LeafContentData.of("phase2.plain", "phase2://plain", 1));

        floatingContent = new FloatingContent();
        floatingLeaf = dockManager.createLeaf("Floating", floatingContent);
        floatingLeaf.setContentData(LeafContentData.of(FLOATING_TYPE, FLOATING_CONTENT_ID, 1));

        group.addLeaf(plainLeaf);
        group.addLeaf(floatingLeaf);
        group.setActiveTab(floatingLeaf);
        dockManager.setRoot(group);

        ribbonManager = new RibbonManager(List.of(new FloatingProvider()));
        Ribbon ribbon = new Ribbon(ribbonManager);
        RibbonDockHost host = new RibbonDockHost(dockManager, ribbonManager, ribbon);
        stage.setScene(new Scene(host, 1100, 700));
        stage.show();
        settleFx();
    }

    @AfterEach
    void tearDown() {
        if (dockManager != null) {
            FxTestUtil.runFx(dockManager::dispose);
        }
    }

    @Test
    void floatingActiveContentContributesRibbonContextAndCapabilities() {
        assertFalse(FxTestUtil.callFx(() -> dockManager.getRibbonContext()
            .attribute(RibbonContextAttributes.FLOATING_KEY)
            .orElse(false)));

        FxTestUtil.runFx(() -> dockManager.floatLeaf(floatingLeaf));
        settleFx();

        RibbonContext context = FxTestUtil.callFx(dockManager::getRibbonContext);
        assertEquals(FLOATING_CONTENT_ID, context.activeContentId());
        assertEquals(FLOATING_TYPE, context.activeContentTypeKey());
        assertTrue(context.attribute(RibbonContextAttributes.FLOATING_KEY).orElseThrow());
        assertEquals("phase2", context.attribute(RibbonContextAttributes.CONTENT_DOMAIN_KEY).orElseThrow());
        assertEquals("floating", context.attribute(RibbonContextAttributes.CONTENT_KIND_KEY).orElseThrow());
        assertSame(floatingContent.actions, context.capability(FloatingActions.class).orElseThrow());

        List<String> tabIds = FxTestUtil.callFx(() -> ribbonManager.getTabs().stream()
            .map(RibbonTabSpec::id)
            .toList());
        assertTrue(tabIds.contains("floating-tools"));

        RibbonCommand command = FxTestUtil.callFx(() ->
            ribbonManager.getCommandRegistry().find(FLOATING_COMMAND_ID).orElseThrow());
        assertTrue(command.enabled().get());
    }

    private static StackPane content(String id, String text) {
        Label label = new Label(text);
        StackPane pane = new StackPane(label);
        pane.setId(id);
        pane.setMinSize(0, 0);
        pane.setPrefSize(420, 260);
        return pane;
    }

    private interface FloatingActions {
        boolean canAct();

        void act();
    }

    private static final class FloatingActionController implements FloatingActions {
        @Override
        public boolean canAct() {
            return true;
        }

        @Override
        public void act() {
        }
    }

    private static final class FloatingContent extends StackPane
        implements RibbonAttributeContributor, RibbonCapabilityContributor {
        private final FloatingActionController actions = new FloatingActionController();

        private FloatingContent() {
            setId("floating-content");
            setMinSize(0, 0);
            setPrefSize(420, 260);
            getChildren().add(new Label("Floating"));
        }

        @Override
        public Map<? extends RibbonAttributeKey<?>, ?> ribbonAttributes() {
            return Map.of(
                RibbonContextAttributes.CONTENT_DOMAIN_KEY, "phase2",
                RibbonContextAttributes.CONTENT_KIND_KEY, "floating"
            );
        }

        @Override
        public Map<? extends Class<?>, ?> ribbonCapabilities() {
            return Map.of(FloatingActions.class, actions);
        }
    }

    private static final class FloatingProvider implements RibbonProvider {
        @Override
        public String id() {
            return "phase2-floating-provider";
        }

        @Override
        public List<RibbonTabSpec> getTabs(RibbonContext context) {
            Optional<FloatingActions> actions = context.capability(FloatingActions.class);
            boolean floating = context.attribute(RibbonContextAttributes.FLOATING_KEY).orElse(false);
            return List.of(
                new RibbonTabSpec(
                    "home",
                    "Home",
                    0,
                    false,
                    ribbonContext -> true,
                    List.of(new RibbonGroupSpec(
                        "home-actions",
                        "Home",
                        0,
                        0,
                        null,
                        List.of(new RibbonButtonSpec(RibbonCommand.of("phase2.ribbon.home", "Home", () -> {
                        })))
                    ))
                ),
                new RibbonTabSpec(
                    "floating-tools",
                    "Floating",
                    10,
                    true,
                    ribbonContext -> ribbonContext.attribute(RibbonContextAttributes.FLOATING_KEY).orElse(false)
                        && ribbonContext.capability(FloatingActions.class).isPresent(),
                    List.of(new RibbonGroupSpec(
                        "floating-actions",
                        "Floating Actions",
                        0,
                        0,
                        null,
                        List.of(new RibbonButtonSpec(RibbonCommand.of(
                            FLOATING_COMMAND_ID,
                            "Act",
                            "Act on floating content",
                            null,
                            null,
                            RibbonBooleanState.mutable(actions.map(FloatingActions::canAct).orElse(false)),
                            () -> actions.ifPresent(FloatingActions::act)
                        )))
                    ))
                )
            );
        }
    }
}
