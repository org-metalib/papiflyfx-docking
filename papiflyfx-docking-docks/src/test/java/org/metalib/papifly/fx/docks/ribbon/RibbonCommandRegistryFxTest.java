package org.metalib.papifly.fx.docks.ribbon;

import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.metalib.papifly.fx.api.ribbon.PapiflyCommand;
import org.metalib.papifly.fx.api.ribbon.RibbonButtonSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonContext;
import org.metalib.papifly.fx.api.ribbon.RibbonGroupSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonProvider;
import org.metalib.papifly.fx.api.ribbon.RibbonTabSpec;
import org.metalib.papifly.fx.docks.layout.data.RibbonSessionData;
import org.metalib.papifly.fx.docks.testutil.FxTestUtil;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.metalib.papifly.fx.docks.ribbon.RibbonTestSupport.contextWithType;
import static org.metalib.papifly.fx.docks.ribbon.RibbonTestSupport.settleFx;
import static org.metalib.papifly.fx.docks.ribbon.RibbonTestSupport.tabIds;

/**
 * Phase 2 invariants — command registry identity stability, QAT restore with
 * hidden contextual tabs, and tolerance of missing command identifiers on
 * restore.
 *
 * <p>The test mounts a bare {@link Ribbon} + {@link RibbonManager} pair
 * without a {@link RibbonDockHost} so the manager's context property remains
 * unbound and can be driven directly by the test.</p>
 */
@ExtendWith(ApplicationExtension.class)
class RibbonCommandRegistryFxTest {

    private static final String CONTEXTUAL_CONTENT_TYPE = "phase2.markdown";
    private static final String CMD_HOME_SAVE = "home.save";
    private static final String CMD_MD_PREVIEW = "markdown.preview";

    private RibbonManager ribbonManager;
    private Ribbon ribbon;

    @Start
    void start(Stage stage) {
        ribbonManager = new RibbonManager(List.of(new ContextualProvider()));
        ribbon = new Ribbon(ribbonManager);
        stage.setScene(new Scene(ribbon, 1100, 300));
        stage.show();
        settleFx();
    }

    @Test
    void commandIdentityIsStableAcrossContextRefreshes() {
        PapiflyCommand originalHome = FxTestUtil.callFx(
            () -> ribbonManager.getCommandRegistry().find(CMD_HOME_SAVE).orElseThrow());

        FxTestUtil.runFx(() -> ribbonManager.setContext(
            contextWithType(CONTEXTUAL_CONTENT_TYPE)));
        settleFx();
        FxTestUtil.runFx(() -> ribbonManager.setContext(RibbonContext.empty()));
        settleFx();

        PapiflyCommand afterRefreshes = FxTestUtil.callFx(
            () -> ribbonManager.getCommandRegistry().find(CMD_HOME_SAVE).orElseThrow());
        assertSame(originalHome, afterRefreshes,
            "command instance must be stable across context-driven refreshes");
    }

    @Test
    void qatRestore_preservesHiddenContextualCommandIds() {
        // Show the contextual tab and pin both the home and the contextual
        // command to the QAT.
        FxTestUtil.runFx(() -> {
            ribbonManager.setContext(new RibbonContext(null, null, CONTEXTUAL_CONTENT_TYPE, Map.of()));
            ribbonManager.getQuickAccessCommandIds().setAll(CMD_HOME_SAVE, CMD_MD_PREVIEW);
        });
        settleFx();

        RibbonSessionData state = FxTestUtil.callFx(ribbon::captureSessionState);
        assertTrue(state.quickAccessCommandIds().contains(CMD_MD_PREVIEW),
            "captured session must retain contextual QAT identifier");

        // Hide the contextual tab, clear QAT state, and force a refresh so the
        // contextual command is evicted from the registry before we restore
        // from the captured state.
        FxTestUtil.runFx(() -> {
            ribbonManager.setContext(RibbonContext.empty());
            ribbonManager.getQuickAccessCommandIds().clear();
            ribbonManager.refresh();
            ribbon.restoreSessionState(state);
        });
        settleFx();
        // After restore, only the currently resolvable command appears in the
        // derived view while both identifiers remain pinned.
        assertEquals(List.of(CMD_HOME_SAVE, CMD_MD_PREVIEW),
            FxTestUtil.callFx(() -> List.copyOf(ribbonManager.getQuickAccessCommandIds())));
        assertEquals(List.of(CMD_HOME_SAVE),
            FxTestUtil.callFx(() -> ribbonManager.getQuickAccessCommands().stream()
                .map(PapiflyCommand::id).toList()));

        // When the contextual tab becomes visible again, the contextual
        // command resolves and the derived view updates automatically.
        FxTestUtil.runFx(() -> ribbonManager.setContext(
            contextWithType(CONTEXTUAL_CONTENT_TYPE)));
        settleFx();
        assertEquals(List.of(CMD_HOME_SAVE, CMD_MD_PREVIEW),
            FxTestUtil.callFx(() -> ribbonManager.getQuickAccessCommands().stream()
                .map(PapiflyCommand::id).toList()));
    }

    @Test
    void qatRestore_toleratesCompletelyUnknownCommandIds() {
        FxTestUtil.runFx(() -> ribbonManager.getQuickAccessCommandIds()
            .setAll(CMD_HOME_SAVE, "phase2.ghost"));
        settleFx();

        RibbonSessionData state = FxTestUtil.callFx(ribbon::captureSessionState);

        FxTestUtil.runFx(() -> {
            ribbonManager.getQuickAccessCommandIds().clear();
            ribbon.restoreSessionState(state);
        });
        settleFx();

        // Unknown identifiers survive in the list but do not render.
        assertTrue(FxTestUtil.callFx(() -> ribbonManager.getQuickAccessCommandIds().contains("phase2.ghost")));
        assertFalse(FxTestUtil.callFx(() -> ribbonManager.getQuickAccessCommands().stream()
            .anyMatch(cmd -> "phase2.ghost".equals(cmd.id()))));
    }

    @Test
    void rapidContextChurnInSingleFxTurnSettlesToFinalContext() {
        FxTestUtil.runFx(() -> {
            ribbonManager.setContext(contextWithType(CONTEXTUAL_CONTENT_TYPE));
            ribbon.setSelectedTabId("markdown");
            ribbonManager.setContext(contextWithType("phase2.code"));
            ribbonManager.setContext(RibbonContext.empty());
        });
        settleFx();

        assertEquals(List.of("home"), tabIds(ribbonManager));
        assertEquals("home", FxTestUtil.callFx(ribbon::getSelectedTabId));
        assertTrue(FxTestUtil.callFx(() -> ribbonManager.getCommandRegistry().contains(CMD_HOME_SAVE)));
        assertFalse(FxTestUtil.callFx(() -> ribbonManager.getCommandRegistry().contains(CMD_MD_PREVIEW)));
    }

    private static final class ContextualProvider implements RibbonProvider {

        @Override
        public String id() {
            return "phase2-contextual-provider";
        }

        @Override
        public List<RibbonTabSpec> getTabs(RibbonContext context) {
            RibbonTabSpec homeTab = new RibbonTabSpec(
                "home",
                "Home",
                0,
                false,
                ribbonContext -> true,
                List.of(new RibbonGroupSpec(
                    "home-actions",
                    "Home Actions",
                    0,
                    0,
                    null,
                    List.of(new RibbonButtonSpec(PapiflyCommand.of(CMD_HOME_SAVE, "Save", () -> {
                    })))
                ))
            );
            RibbonTabSpec markdownTab = new RibbonTabSpec(
                "markdown",
                "Markdown",
                10,
                true,
                ribbonContext -> ribbonContext.activeContentTypeKeyOptional()
                    .map(CONTEXTUAL_CONTENT_TYPE::equals).orElse(false),
                List.of(new RibbonGroupSpec(
                    "markdown-actions",
                    "Markdown Actions",
                    0,
                    0,
                    null,
                    List.of(new RibbonButtonSpec(PapiflyCommand.of(CMD_MD_PREVIEW, "Preview", () -> {
                    })))
                ))
            );
            return List.of(homeTab, markdownTab);
        }
    }
}
