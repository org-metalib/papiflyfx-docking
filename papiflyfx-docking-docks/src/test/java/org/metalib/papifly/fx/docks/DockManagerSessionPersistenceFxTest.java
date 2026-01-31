package org.metalib.papifly.fx.docks;

import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.metalib.papifly.fx.docks.core.DockLeaf;
import org.metalib.papifly.fx.docks.core.DockSplitGroup;
import org.metalib.papifly.fx.docks.core.DockTabGroup;
import org.metalib.papifly.fx.docks.layout.data.DockSessionData;
import org.metalib.papifly.fx.docks.serial.DockSessionPersistence;
import org.metalib.papifly.fx.docks.testutil.FxTestUtil;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApplicationExtension.class)
class DockManagerSessionPersistenceFxTest {

    private DockManager dockManager;

    @Start
    private void start(Stage stage) {
        dockManager = new DockManager();
        stage.setScene(new Scene((Region) dockManager.getRootPane(), 800, 600));
        stage.show();
    }

    @Test
    void testSaveToString_emptySession() {
        String json = FxTestUtil.callFx(dockManager::saveSessionToString);
        assertNotNull(json);
        assertFalse(json.isEmpty());

        DockSessionData session = new DockSessionPersistence().fromJsonString(json);
        assertNotNull(session);
        assertNull(session.layout());
    }

    @Test
    void testSaveAndRestoreFromString() {
        // Setup initial layout
        org.metalib.papifly.fx.docks.core.DockElement layout = DemoApp.createInitialLayout(dockManager);
        FxTestUtil.runFx(() -> dockManager.setRoot(layout));

        // Save to string
        DockSessionData originalSession = FxTestUtil.callFx(dockManager::captureSession);
        String json = FxTestUtil.callFx(dockManager::saveSessionToString);
        assertNotNull(json);
        assertFalse(json.isEmpty());

        // Clear the layout
        FxTestUtil.runFx(() -> dockManager.setRoot((org.metalib.papifly.fx.docks.core.DockElement) null));

        // Restore from string
        FxTestUtil.runFx(() -> dockManager.restoreSessionFromString(json));

        // Verify structure
        org.metalib.papifly.fx.docks.core.DockElement restored = FxTestUtil.callFx(dockManager::getRoot);
        assertNotNull(restored);
        assertInstanceOf(DockSplitGroup.class, restored);

        DockSessionData restoredSession = FxTestUtil.callFx(dockManager::captureSession);
        assertEquals(originalSession, restoredSession);
    }

    @Test
    void testSaveToFile(@TempDir Path tempDir) {
        Path file = tempDir.resolve("session.json");

        // Setup initial layout
        org.metalib.papifly.fx.docks.core.DockElement layout = DemoApp.createInitialLayout(dockManager);
        FxTestUtil.runFx(() -> dockManager.setRoot(layout));

        // Save to file
        FxTestUtil.runFx(() -> dockManager.saveSessionToFile(file));

        // Verify file was created
        assertTrue(file.toFile().exists());
        assertTrue(file.toFile().length() > 0);
    }

    @Test
    void testLoadFromFile(@TempDir Path tempDir) {
        Path file = tempDir.resolve("session.json");

        // Setup and save initial layout
        org.metalib.papifly.fx.docks.core.DockElement layout = DemoApp.createInitialLayout(dockManager);
        FxTestUtil.runFx(() -> dockManager.setRoot(layout));
        FxTestUtil.runFx(() -> dockManager.saveSessionToFile(file));

        // Capture the saved layout
        DockSessionData originalCaptured = FxTestUtil.callFx(dockManager::captureSession);

        // Clear and reload
        FxTestUtil.runFx(() -> dockManager.setRoot((org.metalib.papifly.fx.docks.core.DockElement) null));
        FxTestUtil.runFx(() -> dockManager.loadSessionFromFile(file));

        // Verify structure is restored
        DockSessionData restoredCaptured = FxTestUtil.callFx(dockManager::captureSession);
        assertNotNull(restoredCaptured);
        assertEquals(originalCaptured, restoredCaptured);
    }

    @Test
    void testLoadFromFile_nonExistent(@TempDir Path tempDir) {
        Path file = tempDir.resolve("nonexistent.json");

        assertThrows(
            DockSessionPersistence.SessionFileIOException.class,
            () -> FxTestUtil.runFx(() -> dockManager.loadSessionFromFile(file))
        );
    }

    @Test
    void testRoundTrip_preservesDividerPositions(@TempDir Path tempDir) {
        Path file = tempDir.resolve("session.json");

        // Setup initial layout with specific divider positions
        org.metalib.papifly.fx.docks.core.DockElement layout = DemoApp.createInitialLayout(dockManager);
        FxTestUtil.runFx(() -> dockManager.setRoot(layout));

        // Capture original divider positions
        DockSessionData originalSession = FxTestUtil.callFx(dockManager::captureSession);

        // Save and restore
        FxTestUtil.runFx(() -> dockManager.saveSessionToFile(file));
        FxTestUtil.runFx(() -> dockManager.setRoot((org.metalib.papifly.fx.docks.core.DockElement) null));
        FxTestUtil.runFx(() -> dockManager.loadSessionFromFile(file));

        // Verify divider positions are preserved
        DockSessionData restoredSession = FxTestUtil.callFx(dockManager::captureSession);
        assertEquals(originalSession, restoredSession);
    }

    @Test
    void testRoundTrip_preservesActiveTabs(@TempDir Path tempDir) {
        Path file = tempDir.resolve("session.json");

        // Setup initial layout
        org.metalib.papifly.fx.docks.core.DockElement layout = DemoApp.createInitialLayout(dockManager);
        FxTestUtil.runFx(() -> dockManager.setRoot(layout));

        // Change active tab (if there is one)
        org.metalib.papifly.fx.docks.core.DockElement root = FxTestUtil.callFx(dockManager::getRoot);
        if (root instanceof DockSplitGroup split && split.getFirst() instanceof DockSplitGroup innerSplit) {
            if (innerSplit.getFirst() instanceof DockTabGroup tabGroup && tabGroup.getTabs().size() > 1) {
                FxTestUtil.runFx(() -> tabGroup.setActiveTab(1));
            }
        }

        // Capture original state
        DockSessionData originalSession = FxTestUtil.callFx(dockManager::captureSession);

        // Save and restore
        FxTestUtil.runFx(() -> dockManager.saveSessionToFile(file));
        FxTestUtil.runFx(() -> dockManager.setRoot((org.metalib.papifly.fx.docks.core.DockElement) null));
        FxTestUtil.runFx(() -> dockManager.loadSessionFromFile(file));

        // Verify active tab is preserved
        DockSessionData restoredSession = FxTestUtil.callFx(dockManager::captureSession);
        assertEquals(originalSession, restoredSession);
    }

    @Test
    void testSaveToString_containsValidJson() {
        // Setup initial layout
        org.metalib.papifly.fx.docks.core.DockElement layout = DemoApp.createInitialLayout(dockManager);
        FxTestUtil.runFx(() -> dockManager.setRoot(layout));

        // Save to string
        String json = FxTestUtil.callFx(dockManager::saveSessionToString);

        // Verify it's valid JSON by parsing it
        assertDoesNotThrow(() -> {
            DockSessionPersistence persistence = new DockSessionPersistence();
            persistence.fromJsonString(json);
        });
    }

    @Test
    void testMultipleSaveLoad(@TempDir Path tempDir) {
        Path file1 = tempDir.resolve("session1.json");
        Path file2 = tempDir.resolve("session2.json");

        // Create first layout
        org.metalib.papifly.fx.docks.core.DockElement layout1 = DemoApp.createInitialLayout(dockManager);
        FxTestUtil.runFx(() -> dockManager.setRoot(layout1));
        FxTestUtil.runFx(() -> dockManager.saveSessionToFile(file1));

        // Create a simple second layout
        DockLeaf leaf = FxTestUtil.callFx(() -> dockManager.createLeaf("Simple Leaf", new javafx.scene.control.Label("Content")));
        DockTabGroup group = FxTestUtil.callFx(dockManager::createTabGroup);
        FxTestUtil.runFx(() -> group.addLeaf(leaf));
        FxTestUtil.runFx(() -> dockManager.setRoot(group));
        FxTestUtil.runFx(() -> dockManager.saveSessionToFile(file2));

        // Load first layout
        FxTestUtil.runFx(() -> dockManager.loadSessionFromFile(file1));
        DockSessionData restoredSession1 = FxTestUtil.callFx(dockManager::captureSession);
        assertNotNull(restoredSession1);

        // Load second layout
        FxTestUtil.runFx(() -> dockManager.loadSessionFromFile(file2));
        org.metalib.papifly.fx.docks.core.DockElement restoredRoot = FxTestUtil.callFx(dockManager::getRoot);
        assertNotNull(restoredRoot);
        assertInstanceOf(DockTabGroup.class, restoredRoot);
    }
}
