package org.metalib.papifly.fx.docks.ribbon;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.metalib.papifly.fx.api.ribbon.PapiflyCommand;
import org.metalib.papifly.fx.api.ribbon.RibbonButtonSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonContext;
import org.metalib.papifly.fx.api.ribbon.RibbonGroupSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonProvider;
import org.metalib.papifly.fx.api.ribbon.RibbonTabSpec;
import org.metalib.papifly.fx.docks.DockManager;
import org.metalib.papifly.fx.docks.serial.DockSessionPersistence;
import org.metalib.papifly.fx.docks.core.DockTabGroup;
import org.metalib.papifly.fx.docks.testutil.FxTestUtil;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApplicationExtension.class)
class RibbonSessionPersistenceFxTest {

    private static final String TAB_HOME = "home";
    private static final String TAB_HUGO = "hugo";
    private static final String TAB_LEGACY = "legacy";

    private static final String CMD_SAVE = "cmd.save";
    private static final String CMD_PREVIEW = "cmd.preview";
    private static final String CMD_LEGACY = "cmd.legacy";

    private DockManager dockManager;
    private final DockSessionPersistence persistence = new DockSessionPersistence();
    private RibbonManager ribbonManager;
    private Ribbon ribbon;
    private TestProvider provider;

    @Start
    void start(Stage stage) {
        dockManager = new DockManager();
        DockTabGroup group = dockManager.createTabGroup();
        group.addLeaf(dockManager.createLeaf("Editor", new StackPane(new Label("Editor"))));
        dockManager.setRoot(group);

        provider = new TestProvider(true);
        ribbonManager = new RibbonManager(List.of(provider));
        ribbon = new Ribbon(ribbonManager);
        RibbonDockHost host = new RibbonDockHost(dockManager, ribbonManager, ribbon);
        stage.setScene(new Scene(host, 1100, 700));
        stage.show();
        settle();
    }

    @Test
    void saveRestore_roundTripIncludesRibbonState() {
        FxTestUtil.runFx(() -> {
            ribbonManager.getQuickAccessCommandIds().setAll(CMD_SAVE, CMD_PREVIEW);
            ribbon.setSelectedTabId(TAB_HUGO);
            ribbon.setMinimized(true);
        });
        settle();

        String json = FxTestUtil.callFx(dockManager::saveSessionToString);
        Map<String, Object> sessionMap = persistence.getSerializer().fromJson(json);
        Map<String, Object> extensions = castMap(sessionMap.get("extensions"));
        Map<String, Object> ribbonExtension = castMap(extensions.get("ribbon"));

        assertFalse(sessionMap.containsKey("ribbon"));
        assertEquals("hugo", ribbonExtension.get("selectedTabId"));
        assertEquals(List.of(CMD_SAVE, CMD_PREVIEW), ribbonExtension.get("quickAccessCommandIds"));

        FxTestUtil.runFx(() -> {
            ribbon.setMinimized(false);
            ribbon.setSelectedTabId(TAB_HOME);
            ribbonManager.getQuickAccessCommandIds().clear();
            dockManager.restoreSessionFromString(json);
        });
        settle();

        assertTrue(FxTestUtil.callFx(ribbon::isMinimized));
        assertEquals(TAB_HUGO, FxTestUtil.callFx(ribbon::getSelectedTabId));
        assertEquals(
            List.of(CMD_SAVE, CMD_PREVIEW),
            FxTestUtil.callFx(() -> ribbonManager.getQuickAccessCommands().stream().map(PapiflyCommand::id).toList())
        );
    }

    @Test
    void restore_malformedRibbonExtensionDoesNotBlockCoreSessionRestore() {
        String json = FxTestUtil.callFx(() -> {
            ribbonManager.getQuickAccessCommandIds().setAll(CMD_SAVE, CMD_PREVIEW);
            ribbon.setSelectedTabId(TAB_HUGO);
            ribbon.setMinimized(true);
            return dockManager.saveSessionToString();
        });

        Map<String, Object> sessionMap = persistence.getSerializer().fromJson(json);
        Map<String, Object> extensions = castMap(sessionMap.get("extensions"));
        Map<String, Object> ribbonExtension = castMap(extensions.get("ribbon"));
        ribbonExtension.put("quickAccessCommandIds", "broken");
        String malformedJson = persistence.getSerializer().toJson(sessionMap);

        FxTestUtil.runFx(() -> {
            ribbon.setMinimized(false);
            ribbon.setSelectedTabId(TAB_HOME);
            ribbonManager.getQuickAccessCommandIds().clear();
            dockManager.setRoot((org.metalib.papifly.fx.docks.core.DockElement) null);
            dockManager.restoreSessionFromString(malformedJson);
        });
        settle();

        assertNotNull(FxTestUtil.callFx(dockManager::getRoot));
        assertEquals(TAB_HOME, FxTestUtil.callFx(ribbon::getSelectedTabId));
        assertTrue(FxTestUtil.callFx(() -> ribbonManager.getQuickAccessCommandIds().isEmpty()));
        assertTrue(FxTestUtil.callFx(() -> !ribbon.isMinimized()));
    }

    @Test
    void restore_missingTabAndCommandFallsBackGracefully() {
        String json = FxTestUtil.callFx(() -> {
            ribbonManager.getQuickAccessCommandIds().setAll(CMD_SAVE, CMD_LEGACY);
            ribbon.setSelectedTabId(TAB_LEGACY);
            ribbon.setMinimized(false);
            return dockManager.saveSessionToString();
        });

        FxTestUtil.runFx(() -> {
            ribbonManager.getProviders().setAll(new TestProvider(false));
            ribbonManager.getQuickAccessCommandIds().clear();
            dockManager.restoreSessionFromString(json);
        });
        settle();

        assertEquals(TAB_HOME, FxTestUtil.callFx(ribbon::getSelectedTabId));
        // Both identifiers remain pinned, but only resolvable commands surface
        // in the derived Quick Access Toolbar view.
        assertEquals(
            List.of(CMD_SAVE, CMD_LEGACY),
            FxTestUtil.callFx(() -> List.copyOf(ribbonManager.getQuickAccessCommandIds()))
        );
        assertEquals(
            List.of(CMD_SAVE),
            FxTestUtil.callFx(() -> ribbonManager.getQuickAccessCommands().stream().map(PapiflyCommand::id).toList())
        );
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    private static void settle() {
        FxTestUtil.waitForFxEvents();
        FxTestUtil.waitForFxEvents();
    }

    private static final class TestProvider implements RibbonProvider {
        private final boolean includeLegacy;
        private final PapiflyCommand save = PapiflyCommand.of(CMD_SAVE, "Save", () -> {
        });
        private final PapiflyCommand preview = PapiflyCommand.of(CMD_PREVIEW, "Preview", () -> {
        });
        private final PapiflyCommand legacy = PapiflyCommand.of(CMD_LEGACY, "Legacy", () -> {
        });

        private TestProvider(boolean includeLegacy) {
            this.includeLegacy = includeLegacy;
        }

        @Override
        public String id() {
            return "test-provider-" + includeLegacy;
        }

        @Override
        public List<RibbonTabSpec> getTabs(RibbonContext context) {
            List<RibbonTabSpec> baseTabs = List.of(
                new RibbonTabSpec(
                    TAB_HOME,
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
                        List.of(new RibbonButtonSpec(save))
                    ))
                ),
                new RibbonTabSpec(
                    TAB_HUGO,
                    "Hugo",
                    10,
                    false,
                    ribbonContext -> true,
                    List.of(new RibbonGroupSpec(
                        "hugo-actions",
                        "Hugo Actions",
                        0,
                        0,
                        null,
                        List.of(new RibbonButtonSpec(preview))
                    ))
                )
            );
            if (!includeLegacy) {
                return baseTabs;
            }
            return List.of(
                baseTabs.get(0),
                baseTabs.get(1),
                new RibbonTabSpec(
                    TAB_LEGACY,
                    "Legacy",
                    20,
                    false,
                    ribbonContext -> true,
                    List.of(new RibbonGroupSpec(
                        "legacy-actions",
                        "Legacy Actions",
                        0,
                        0,
                        null,
                        List.of(new RibbonButtonSpec(legacy))
                    ))
                )
            );
        }

        private PapiflyCommand save() {
            return save;
        }

        private PapiflyCommand preview() {
            return preview;
        }

        private PapiflyCommand legacy() {
            return legacy;
        }
    }
}
