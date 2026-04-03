package org.metalib.papifly.fx.docks;

import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.docks.layout.data.DockSessionData;
import org.metalib.papifly.fx.docks.layout.data.LayoutNode;
import org.metalib.papifly.fx.docks.testutil.FxTestUtil;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApplicationExtension.class)
class DockManagerServicesFxTest {

    private final AtomicBoolean captureCalled = new AtomicBoolean();
    private final DockSessionData injectedSession = DockSessionData.of(null, List.of(), List.of(), null);

    private DockManager dockManager;

    @Start
    private void start(Stage stage) {
        DockManagerServices defaults = DockManagerServices.defaults();
        DockSessionService customSessionService = new DockSessionService() {
            @Override
            public LayoutNode captureLayout() {
                return null;
            }

            @Override
            public DockSessionData captureSession() {
                captureCalled.set(true);
                return injectedSession;
            }

            @Override
            public void restoreSession(DockSessionData session) {
            }

            @Override
            public String saveSessionToString() {
                return "";
            }

            @Override
            public void restoreSessionFromString(String json) {
            }

            @Override
            public void saveSessionToFile(Path path) {
            }

            @Override
            public void loadSessionFromFile(Path path) {
            }
        };

        DockManagerServices services = new DockManagerServices(
            defaults.themeServiceFactory(),
            defaults.floatingServiceFactory(),
            defaults.minMaxServiceFactory(),
            (context, treeService, floatingService, minMaxService) -> customSessionService
        );

        dockManager = new DockManager(Theme.dark(), services);
        stage.setScene(new Scene((Region) dockManager.getRootPane(), 800, 600));
        stage.show();
    }

    @AfterEach
    void tearDown() {
        if (dockManager != null) {
            FxTestUtil.runFx(dockManager::dispose);
        }
    }

    @Test
    void captureSession_usesInjectedSessionService() {
        DockSessionData session = FxTestUtil.callFx(dockManager::captureSession);

        assertTrue(captureCalled.get());
        assertSame(injectedSession, session);
    }
}
