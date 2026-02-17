package org.metalib.papifly.fx.docks;

import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.metalib.papifly.fx.docks.core.DockLeaf;
import org.metalib.papifly.fx.docks.core.DockTabGroup;
import org.metalib.papifly.fx.docks.floating.FloatingDockWindow;
import org.metalib.papifly.fx.docks.layout.ContentStateAdapter;
import org.metalib.papifly.fx.docks.layout.ContentStateRegistry;
import org.metalib.papifly.fx.docks.layout.ContentFactory;
import org.metalib.papifly.fx.docks.layout.data.DockSessionData;
import org.metalib.papifly.fx.docks.layout.data.LeafContentData;
import org.metalib.papifly.fx.docks.layout.data.LeafData;
import org.metalib.papifly.fx.docks.layout.data.TabGroupData;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests session save/restore with floating windows.
 */
@ExtendWith(ApplicationExtension.class)
class DockManagerSessionFxTest {

    private DockManager dockManager;
    private Stage stage;

    @Start
    void start(Stage stage) {
        this.stage = stage;
        this.dockManager = new DockManager();
        dockManager.setOwnerStage(stage);

        // Set content factory
        dockManager.setContentFactory(id -> {
            if (id != null && id.startsWith("editor:")) {
                String title = id.substring("editor:".length());
                return new StackPane(new Label("Editor: " + title));
            } else if ("files".equals(id)) {
                return new StackPane(new Label("Files"));
            } else if ("properties".equals(id)) {
                return new StackPane(new Label("Properties"));
            }
            return new StackPane(new Label(id));
        });

        Scene scene = new Scene(dockManager.getRootPane(), 800, 600);
        stage.setScene(scene);
        stage.show();
    }

    @Test
    void testSessionSaveRestoreWithFloating(@TempDir Path tempDir) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                // Create initial layout with 3 leaves
                DockLeaf files = dockManager.createLeaf("Files", new StackPane(new Label("Files")));
                files.setContentFactoryId("files");
                DockLeaf editor1 = dockManager.createLeaf("Editor 1", new StackPane(new Label("Editor 1")));
                editor1.setContentFactoryId("editor:Editor 1");
                DockLeaf editor2 = dockManager.createLeaf("Editor 2", new StackPane(new Label("Editor 2")));
                editor2.setContentFactoryId("editor:Editor 2");

                // Create layout
                var filesTabGroup = dockManager.createTabGroup();
                filesTabGroup.addLeaf(files);

                var editorTabGroup = dockManager.createTabGroup();
                editorTabGroup.addLeaf(editor1);
                editorTabGroup.addLeaf(editor2);

                var split = dockManager.createHorizontalSplit(
                    filesTabGroup,
                    editorTabGroup,
                    0.3
                );
                dockManager.setRoot((org.metalib.papifly.fx.docks.core.DockElement) split);

                // Wait for layout to be applied
                WaitForAsyncUtils.waitForFxEvents();

                // Float editor2
                dockManager.floatLeaf(editor2);
                WaitForAsyncUtils.waitForFxEvents();

                // Verify editor2 is floating
                assertTrue(dockManager.getFloatingWindowManager().isFloating(editor2));
                FloatingDockWindow window = dockManager.getFloatingWindowManager().getWindow(editor2);
                assertNotNull(window);

                // Set specific bounds for the floating window
                Rectangle2D bounds = new Rectangle2D(100, 100, 400, 300);
                window.setBounds(bounds);

                // Capture session
                DockSessionData session = dockManager.captureSession();
                assertNotNull(session);
                assertNotNull(session.layout());
                assertEquals(1, session.floating().size());
                assertEquals(0, session.minimized().size());
                assertNull(session.maximized());

                // Verify floating data
                var floatingData = session.floating().get(0);
                assertEquals("Editor 2", floatingData.leaf().title());
                assertNotNull(floatingData.bounds());
                assertEquals(100, floatingData.bounds().x(), 0.1);
                assertEquals(100, floatingData.bounds().y(), 0.1);
                assertEquals(400, floatingData.bounds().width(), 0.1);
                assertEquals(300, floatingData.bounds().height(), 0.1);

                // Save to file
                Path sessionFile = tempDir.resolve("session.json");
                dockManager.saveSessionToFile(sessionFile);
                assertTrue(sessionFile.toFile().exists());

                // Clear current state (close all)
                dockManager.getFloatingWindowManager().closeAll();
                dockManager.setRoot((org.metalib.papifly.fx.docks.core.DockElement) null);
                WaitForAsyncUtils.waitForFxEvents();

                // Verify cleared
                assertEquals(0, dockManager.getFloatingWindowManager().getFloatingCount());

                // Restore session from file
                dockManager.loadSessionFromFile(sessionFile);
                WaitForAsyncUtils.waitForFxEvents();

                // Verify layout restored
                assertNotNull(dockManager.getRoot());

                // Verify editor2 is floating again
                assertEquals(1, dockManager.getFloatingWindowManager().getFloatingCount());

                // Find the restored editor2 leaf
                FloatingDockWindow restoredWindow = null;
                for (FloatingDockWindow win : dockManager.getFloatingWindowManager().getFloatingWindows()) {
                    if ("Editor 2".equals(win.getLeaf().getMetadata().title())) {
                        restoredWindow = win;
                        break;
                    }
                }
                assertNotNull(restoredWindow, "Editor 2 should be floating after restore");

                // Verify bounds were restored
                Rectangle2D restoredBounds = restoredWindow.getBounds();
                assertNotNull(restoredBounds);
                assertEquals(100, restoredBounds.getMinX(), 0.1);
                assertEquals(100, restoredBounds.getMinY(), 0.1);
                assertEquals(400, restoredBounds.getWidth(), 0.1);
                assertEquals(300, restoredBounds.getHeight(), 0.1);

                latch.countDown();
            } catch (Exception e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
        });

        latch.await();
    }

    @Test
    void testSessionSaveRestoreToString() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                // Create simple layout
                DockLeaf leaf = dockManager.createLeaf("Test", new StackPane(new Label("Test")));
                leaf.setContentFactoryId("test");
                var tabGroup = dockManager.createTabGroup();
                tabGroup.addLeaf(leaf);
                dockManager.setRoot((org.metalib.papifly.fx.docks.core.DockElement) tabGroup);

                WaitForAsyncUtils.waitForFxEvents();

                // Float the leaf
                dockManager.floatLeaf(leaf);
                WaitForAsyncUtils.waitForFxEvents();

                // Save to string
                String json = dockManager.saveSessionToString();
                assertNotNull(json);
                assertFalse(json.isEmpty());
                assertTrue(json.contains("\"type\": \"dockSession\""));
                assertTrue(json.contains("\"floating\""));

                // Clear and restore
                dockManager.getFloatingWindowManager().closeAll();
                dockManager.setRoot((org.metalib.papifly.fx.docks.core.DockElement) null);
                WaitForAsyncUtils.waitForFxEvents();

                dockManager.restoreSessionFromString(json);
                WaitForAsyncUtils.waitForFxEvents();

                // Verify restored
                assertEquals(1, dockManager.getFloatingWindowManager().getFloatingCount());

                latch.countDown();
            } catch (Exception e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
        });

        latch.await();
    }

    @Test
    void testSessionWithMinimizedAndFloating() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                // Create layout with multiple leaves
                DockLeaf leaf1 = dockManager.createLeaf("Leaf 1", new StackPane(new Label("Leaf 1")));
                leaf1.setContentFactoryId("leaf1");
                DockLeaf leaf2 = dockManager.createLeaf("Leaf 2", new StackPane(new Label("Leaf 2")));
                leaf2.setContentFactoryId("leaf2");
                DockLeaf leaf3 = dockManager.createLeaf("Leaf 3", new StackPane(new Label("Leaf 3")));
                leaf3.setContentFactoryId("leaf3");

                var tabGroup = dockManager.createTabGroup();
                tabGroup.addLeaf(leaf1);
                tabGroup.addLeaf(leaf2);
                tabGroup.addLeaf(leaf3);
                dockManager.setRoot((org.metalib.papifly.fx.docks.core.DockElement) tabGroup);

                WaitForAsyncUtils.waitForFxEvents();

                // Float leaf2
                dockManager.floatLeaf(leaf2);
                WaitForAsyncUtils.waitForFxEvents();

                // Minimize leaf3
                dockManager.minimizeLeaf(leaf3);
                WaitForAsyncUtils.waitForFxEvents();

                // Capture session
                DockSessionData session = dockManager.captureSession();
                assertEquals(1, session.floating().size());
                assertEquals(1, session.minimized().size());
                assertEquals("Leaf 2", session.floating().get(0).leaf().title());
                assertEquals("Leaf 3", session.minimized().get(0).leaf().title());

                // Save and restore
                String json = dockManager.saveSessionToString();

                dockManager.getFloatingWindowManager().closeAll();
                dockManager.setRoot((org.metalib.papifly.fx.docks.core.DockElement) null);
                WaitForAsyncUtils.waitForFxEvents();

                dockManager.restoreSessionFromString(json);
                WaitForAsyncUtils.waitForFxEvents();

                // Verify both states restored
                assertEquals(1, dockManager.getFloatingWindowManager().getFloatingCount());
                assertEquals(1, dockManager.getMinimizedStore().getMinimizedCount());

                latch.countDown();
            } catch (Exception e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
        });

        latch.await();
    }

    @Test
    void restoredFloatingLeaf_canCloseAfterRestore() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                DockLeaf leaf = dockManager.createLeaf("Closable", new StackPane(new Label("Closable")));
                leaf.setContentFactoryId("closable");
                var tabGroup = dockManager.createTabGroup();
                tabGroup.addLeaf(leaf);
                dockManager.setRoot((org.metalib.papifly.fx.docks.core.DockElement) tabGroup);

                WaitForAsyncUtils.waitForFxEvents();

                dockManager.floatLeaf(leaf);
                WaitForAsyncUtils.waitForFxEvents();

                String json = dockManager.saveSessionToString();

                dockManager.getFloatingWindowManager().closeAll();
                dockManager.setRoot((org.metalib.papifly.fx.docks.core.DockElement) null);
                WaitForAsyncUtils.waitForFxEvents();

                dockManager.restoreSessionFromString(json);
                WaitForAsyncUtils.waitForFxEvents();

                assertEquals(1, dockManager.getFloatingWindowManager().getFloatingCount());
                FloatingDockWindow restoredWindow = dockManager.getFloatingWindowManager()
                    .getFloatingWindows()
                    .iterator()
                    .next();
                DockLeaf restoredLeaf = restoredWindow.getLeaf();

                restoredLeaf.requestClose();
                WaitForAsyncUtils.waitForFxEvents();

                assertEquals(0, dockManager.getFloatingWindowManager().getFloatingCount());
                assertNull(restoredLeaf.getContent());

                latch.countDown();
            } catch (Exception e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
        });

        latch.await();
    }

    @Test
    void restoreSession_adapterRestoreFailureFallsBackAndCaptureStillWorks() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                ContentStateRegistry registry = new ContentStateRegistry();
                registry.register(new ContentStateAdapter() {
                    @Override
                    public String getTypeKey() {
                        return "throwing-type";
                    }

                    @Override
                    public int getVersion() {
                        return 1;
                    }

                    @Override
                    public Map<String, Object> saveState(String contentId, Node content) {
                        throw new RuntimeException("simulated save failure");
                    }

                    @Override
                    public Node restore(LeafContentData content) {
                        throw new RuntimeException("simulated restore failure");
                    }
                });
                dockManager.setContentStateRegistry(registry);

                LeafContentData contentData = LeafContentData.of(
                    "throwing-type",
                    "leaf-content-1",
                    1,
                    Map.of("payload", "value")
                );
                LeafData leaf = LeafData.of(
                    "leaf-throwing",
                    "Throwing Leaf",
                    "editor:Throwing Leaf",
                    contentData
                );
                DockSessionData session = DockSessionData.of(
                    TabGroupData.of("group-throwing", List.of(leaf), 0),
                    List.of(),
                    List.of(),
                    null
                );

                dockManager.restoreSession(session);
                WaitForAsyncUtils.waitForFxEvents();

                assertNotNull(dockManager.getRoot());
                assertTrue(dockManager.getRoot() instanceof DockTabGroup);
                DockTabGroup group = (DockTabGroup) dockManager.getRoot();
                assertEquals(1, group.getTabs().size());
                assertNotNull(group.getTabs().getFirst().getContent());

                DockSessionData captured = dockManager.captureSession();
                assertNotNull(captured);
                assertNotNull(captured.layout());
                assertTrue(captured.layout() instanceof TabGroupData);
                TabGroupData capturedGroup = (TabGroupData) captured.layout();
                assertEquals(1, capturedGroup.tabs().size());
                assertNotNull(capturedGroup.tabs().getFirst().content());
                assertEquals("throwing-type", capturedGroup.tabs().getFirst().content().typeKey());

                latch.countDown();
            } catch (Exception e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
        });

        latch.await();
    }
}
