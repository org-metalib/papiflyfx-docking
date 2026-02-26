package org.metalib.papifly.fx.tree.api;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.tree.render.TreeViewport;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApplicationExtension.class)
class TreeViewFxTest {

    private TreeView<String> treeView;

    @Start
    void start(Stage stage) {
        treeView = new TreeView<>();
        treeView.setEditCommitHandler(TreeItem::setValue);
        treeView.setRoot(createLargeTree());
        Scene scene = new Scene(treeView, 480, 320);
        stage.setScene(scene);
        stage.show();
    }

    @Test
    void viewportDirtyFlagCyclesThroughLayout() {
        flushLayout();
        assertFalse(callOnFx(() -> treeView.getViewport().isDirty()));

        runOnFx(() -> treeView.getViewport().markDirty());
        assertTrue(callOnFx(() -> treeView.getViewport().isDirty()));

        flushLayout();
        assertFalse(callOnFx(() -> treeView.getViewport().isDirty()));
    }

    @Test
    void viewportScrollOffsetUpdatesForLargeTree() {
        flushLayout();
        runOnFx(() -> treeView.getViewport().setScrollOffset(600.0));
        flushLayout();
        assertTrue(callOnFx(() -> treeView.getViewport().getScrollOffset()) > 0.0);
    }

    @Test
    void keyboardNavigationMovesFocusToNextVisibleRow() {
        flushLayout();
        runOnFx(() -> {
            treeView.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.HOME, false, false, false, false));
            treeView.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.DOWN, false, false, false, false));
        });
        TreeItem<String> focused = callOnFx(() -> treeView.getSelectionModel().getFocusedItem());
        assertNotNull(focused);
        assertEquals("node-0", focused.getValue());
    }

    @Test
    void keyboardNavigationSkipsRowsExcludedByNavigationPredicate() {
        runOnFx(() -> {
            TreeItem<String> root = new TreeItem<>("root");
            root.addChild(new TreeItem<>("heading"));
            root.addChild(new TreeItem<>("sample"));
            treeView.setRoot(root);
            treeView.getSelectionModel().clearSelection();
            treeView.getSelectionModel().setFocusedItem(null);
            treeView.setNavigationSelectablePredicate(item -> !"heading".equals(item.getValue()));
        });
        flushLayout();
        runOnFx(() -> {
            treeView.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.HOME, false, false, false, false));
            treeView.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.DOWN, false, false, false, false));
        });
        TreeItem<String> focused = callOnFx(() -> treeView.getSelectionModel().getFocusedItem());
        assertNotNull(focused);
        assertEquals("sample", focused.getValue());
    }

    @Test
    void keyboardNavigationConsumesBoundaryKeyWhenNoFurtherSelectableRow() {
        KeyEvent upEvent = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.UP, false, false, false, false);
        runOnFx(() -> {
            TreeItem<String> root = new TreeItem<>("root");
            root.addChild(new TreeItem<>("heading"));
            root.addChild(new TreeItem<>("sample-1"));
            root.addChild(new TreeItem<>("sample-2"));
            treeView.setShowRoot(false);
            treeView.setRoot(root);
            treeView.getSelectionModel().clearSelection();
            treeView.getSelectionModel().setFocusedItem(null);
            treeView.setNavigationSelectablePredicate(item -> !"heading".equals(item.getValue()));
            treeView.getOnKeyPressed().handle(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.HOME, false, false, false, false));
            treeView.getOnKeyPressed().handle(upEvent);
            treeView.getOnKeyPressed().handle(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.DOWN, false, false, false, false));
        });
        TreeItem<String> focused = callOnFx(() -> treeView.getSelectionModel().getFocusedItem());
        assertTrue(upEvent.isConsumed());
        assertNotNull(focused);
        assertEquals("sample-2", focused.getValue());
    }

    @Test
    void keyboardNavigationConsumesLeftWhenNoSelectableAncestor() {
        KeyEvent leftEvent = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.LEFT, false, false, false, false);
        runOnFx(() -> {
            TreeItem<String> root = new TreeItem<>("root");
            root.addChild(new TreeItem<>("heading"));
            root.addChild(new TreeItem<>("sample-1"));
            root.addChild(new TreeItem<>("sample-2"));
            treeView.setShowRoot(false);
            treeView.setRoot(root);
            treeView.getSelectionModel().clearSelection();
            treeView.getSelectionModel().setFocusedItem(null);
            treeView.setNavigationSelectablePredicate(item -> item != null && item.getValue() != null && item.getValue().startsWith("sample-"));
            treeView.getOnKeyPressed().handle(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.HOME, false, false, false, false));
            treeView.getOnKeyPressed().handle(leftEvent);
            treeView.getOnKeyPressed().handle(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.DOWN, false, false, false, false));
        });
        TreeItem<String> focused = callOnFx(() -> treeView.getSelectionModel().getFocusedItem());
        assertTrue(leftEvent.isConsumed());
        assertNotNull(focused);
        assertEquals("sample-2", focused.getValue());
    }

    @Test
    void keyboardNavigationConsumesRightOnLeaf() {
        KeyEvent rightEvent = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.RIGHT, false, false, false, false);
        runOnFx(() -> {
            TreeItem<String> root = new TreeItem<>("root");
            root.addChild(new TreeItem<>("heading"));
            root.addChild(new TreeItem<>("sample-1"));
            root.addChild(new TreeItem<>("sample-2"));
            treeView.setShowRoot(false);
            treeView.setRoot(root);
            treeView.getSelectionModel().clearSelection();
            treeView.getSelectionModel().setFocusedItem(null);
            treeView.setNavigationSelectablePredicate(item -> item != null && item.getValue() != null && item.getValue().startsWith("sample-"));
            treeView.getOnKeyPressed().handle(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.HOME, false, false, false, false));
            treeView.getOnKeyPressed().handle(rightEvent);
            treeView.getOnKeyPressed().handle(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.DOWN, false, false, false, false));
        });
        TreeItem<String> focused = callOnFx(() -> treeView.getSelectionModel().getFocusedItem());
        assertTrue(rightEvent.isConsumed());
        assertNotNull(focused);
        assertEquals("sample-2", focused.getValue());
    }

    @Test
    void hitTestIgnoresVerticalScrollbarArea() {
        flushLayout();
        assertTrue(callOnFx(() -> treeView.getViewport().isVerticalScrollbarVisible()));
        TreeViewport.ScrollbarGeometry geometry = callOnFx(() -> treeView.getViewport().getVerticalScrollbarGeometry());
        assertNotNull(geometry);

        TreeViewport.HitInfo<String> hitInfo = callOnFx(() -> treeView.getViewport().hitTest(
            geometry.trackX() + (geometry.trackWidth() * 0.5),
            geometry.trackY() + Math.max(1.0, geometry.trackHeight() * 0.5)
        ));
        assertNull(hitInfo);
    }

    private TreeItem<String> createLargeTree() {
        TreeItem<String> root = new TreeItem<>("root");
        for (int i = 0; i < 500; i++) {
            root.addChild(new TreeItem<>("node-" + i));
        }
        root.setExpanded(true);
        return root;
    }

    private void flushLayout() {
        runOnFx(() -> {
            treeView.applyCss();
            treeView.layout();
        });
        WaitForAsyncUtils.waitForFxEvents();
    }

    private void runOnFx(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
            return;
        }
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                action.run();
            } finally {
                latch.countDown();
            }
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private <R> R callOnFx(Callable<R> action) {
        if (Platform.isFxApplicationThread()) {
            try {
                return action.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        final Object[] result = new Object[1];
        final RuntimeException[] error = new RuntimeException[1];
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                result[0] = action.call();
            } catch (Exception e) {
                error[0] = new RuntimeException(e);
            } finally {
                latch.countDown();
            }
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        if (error[0] != null) {
            throw error[0];
        }
        @SuppressWarnings("unchecked")
        R typed = (R) result[0];
        return typed;
    }
}
