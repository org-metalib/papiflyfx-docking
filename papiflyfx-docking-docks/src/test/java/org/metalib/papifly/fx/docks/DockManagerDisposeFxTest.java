package org.metalib.papifly.fx.docks;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.docks.testutil.FxTestUtil;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApplicationExtension.class)
class DockManagerDisposeFxTest {

    private DockManager dockManager;

    @Start
    private void start(Stage stage) {
        dockManager = new DockManager();
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
    void rootPaneStoresDockManagerReferenceForLifecycleCleanup() {
        Object marker = FxTestUtil.callFx(
            () -> dockManager.getRootPane().getProperties().get(DockManager.ROOT_PANE_MANAGER_PROPERTY)
        );
        assertSame(dockManager, marker);
    }

    @Test
    void disposeUnbindsThemePropertyWhenBound() {
        ObjectProperty<Theme> appTheme = new SimpleObjectProperty<>(Theme.dark());
        FxTestUtil.runFx(() -> dockManager.themeProperty().bind(appTheme));
        assertTrue(FxTestUtil.callFx(() -> dockManager.themeProperty().isBound()));

        FxTestUtil.runFx(dockManager::dispose);

        assertFalse(FxTestUtil.callFx(() -> dockManager.themeProperty().isBound()));
    }
}
