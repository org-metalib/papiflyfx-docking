package org.metalib.papifly.fx.docks;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.metalib.papifly.fx.docks.core.DockLeaf;
import org.metalib.papifly.fx.docks.core.DockState;
import org.metalib.papifly.fx.docks.core.DockTabGroup;
import org.metalib.papifly.fx.docks.testutil.FxTestUtil;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApplicationExtension.class)
class DockManagerFloatingStageResolutionFxTest {

    private DockManager dockManager;
    private Stage stage;

    @Start
    private void start(Stage stage) {
        this.stage = stage;
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
    void floatLeaf_resolvesOwnerStageFromSceneWhenNotExplicitlySet() {
        DockLeaf leaf = FxTestUtil.callFx(() -> dockManager.createLeaf("Leaf", new Label("Content")));
        DockTabGroup tabGroup = FxTestUtil.callFx(dockManager::createTabGroup);

        FxTestUtil.runFx(() -> {
            tabGroup.addLeaf(leaf);
            dockManager.setRoot(tabGroup);
        });
        FxTestUtil.waitForFxEvents();

        assertNull(FxTestUtil.callFx(dockManager::getFloatingWindowManager));

        FxTestUtil.runFx(() -> dockManager.floatLeaf(leaf));
        FxTestUtil.waitForFxEvents();

        assertNotNull(FxTestUtil.callFx(dockManager::getFloatingWindowManager));
        assertTrue(FxTestUtil.callFx(() -> dockManager.getFloatingWindowManager().isFloating(leaf)));
    }

    @Test
    void floatLeaf_withoutOwnerStageAndWithoutAttachedScene_keepsLeafDocked() {
        DockManager detachedManager = FxTestUtil.callFx(DockManager::new);
        DockLeaf leaf = FxTestUtil.callFx(() -> detachedManager.createLeaf("Leaf", new Label("Content")));
        DockTabGroup tabGroup = FxTestUtil.callFx(detachedManager::createTabGroup);

        FxTestUtil.runFx(() -> {
            tabGroup.addLeaf(leaf);
            detachedManager.setRoot(tabGroup);
        });
        FxTestUtil.waitForFxEvents();

        FxTestUtil.runFx(() -> detachedManager.floatLeaf(leaf));
        FxTestUtil.waitForFxEvents();

        assertNull(FxTestUtil.callFx(detachedManager::getFloatingWindowManager));
        assertSame(tabGroup, FxTestUtil.callFx(leaf::getParent));
        assertEquals(DockState.DOCKED, FxTestUtil.callFx(() -> leaf.getMetadata().state()));
        assertSame(tabGroup, FxTestUtil.callFx(detachedManager::getRoot));

        FxTestUtil.runFx(detachedManager::dispose);
    }
}
