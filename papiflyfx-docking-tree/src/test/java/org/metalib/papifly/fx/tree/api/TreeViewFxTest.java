package org.metalib.papifly.fx.tree.api;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.tree.render.TreeViewport;
import org.metalib.papifly.fx.tree.search.TreeSearchOverlay;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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

    @Test
    void cmdOrCtrlFOpensSearchOverlay() {
        runOnFx(() -> {
            treeView.requestFocus();
            treeView.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.F, false, true, false, false));
        });
        assertTrue(callOnFx(treeView::isSearchOpen));
    }

    @Test
    void searchOverlayRemainsCompactAndUsesSearchStyling() {
        runOnFx(() -> treeView.openSearch("node"));
        flushLayout();

        TreeSearchOverlay overlay = callOnFx(() -> treeView.getChildren().stream()
            .filter(TreeSearchOverlay.class::isInstance)
            .map(TreeSearchOverlay.class::cast)
            .findFirst()
            .orElseThrow());
        double overlayHeight = callOnFx(overlay::getHeight);
        double treeHeight = callOnFx(treeView::getHeight);

        assertEquals(Region.USE_PREF_SIZE, callOnFx(overlay::getMaxHeight));
        assertTrue(overlayHeight < treeHeight * 0.25);
        assertTrue(callOnFx(() -> overlay.getStyleClass().contains("pf-tree-search-overlay")));
        assertTrue(callOnFx(() -> overlay.lookupAll(".pf-tree-search-field").size() == 1));
    }

    @Test
    void searchOverlayAdaptsToNarrowTreeWidth() {
        runOnFx(() -> {
            treeView.getScene().getWindow().setWidth(210.0);
            treeView.openSearch("sample");
        });
        flushLayout();

        TreeSearchOverlay overlay = callOnFx(() -> treeView.getChildren().stream()
            .filter(TreeSearchOverlay.class::isInstance)
            .map(TreeSearchOverlay.class::cast)
            .findFirst()
            .orElseThrow());
        TextField searchField = callOnFx(() -> (TextField) overlay.lookup(".pf-tree-search-field"));
        Label countLabel = callOnFx(() -> (Label) overlay.lookup(".pf-tree-search-result-label"));

        assertNotNull(searchField);
        assertNotNull(countLabel);
        assertTrue(callOnFx(overlay::getMaxWidth) <= callOnFx(treeView::getWidth));
        assertTrue(callOnFx(searchField::getWidth) >= 40.0);
        assertFalse(callOnFx(countLabel::isVisible));
    }

    @Test
    void typingPrintableCharOpensSearchOverlayWithSeedQuery() {
        runOnFx(() -> {
            treeView.requestFocus();
            treeView.fireEvent(new KeyEvent(KeyEvent.KEY_TYPED, "n", "n", KeyCode.UNDEFINED, false, false, false, false));
        });

        assertTrue(callOnFx(treeView::isSearchOpen));
        TreeItem<String> focused = callOnFx(() -> treeView.getSelectionModel().getFocusedItem());
        assertNotNull(focused);
        assertEquals("node-0", focused.getValue());
    }

    @Test
    void searchSelectsAndExpandsAndRevealsMatch() {
        runOnFx(() -> {
            TreeItem<String> root = new TreeItem<>("root");
            for (int i = 0; i < 220; i++) {
                root.addChild(new TreeItem<>("noise-" + i));
            }
            TreeItem<String> branch = new TreeItem<>("branch");
            TreeItem<String> target = new TreeItem<>("target-node");
            branch.addChild(target);
            root.addChild(branch);
            treeView.setRoot(root);
            treeView.getExpansionModel().setExpanded(branch, false);
            treeView.getViewport().setScrollOffset(0.0);
        });
        flushLayout();

        TreeItem<String> match = callOnFx(() -> treeView.searchAndRevealFirst("target"));
        assertNotNull(match);
        assertEquals("target-node", match.getValue());
        TreeItem<String> focused = callOnFx(() -> treeView.getSelectionModel().getFocusedItem());
        assertSame(match, focused);
        assertTrue(callOnFx(() -> treeView.getExpansionModel().isExpanded(match.getParent())));
        assertTrue(callOnFx(() -> treeView.getViewport().getScrollOffset()) > 0.0);
    }

    @Test
    void searchNextAndPreviousCycleThroughMatches() {
        runOnFx(() -> {
            TreeItem<String> root = new TreeItem<>("root");
            root.addChild(new TreeItem<>("match-1"));
            root.addChild(new TreeItem<>("match-2"));
            root.addChild(new TreeItem<>("match-3"));
            treeView.setRoot(root);
            treeView.openSearch("match");
        });

        assertEquals("match-1", callOnFx(() -> treeView.getSelectionModel().getFocusedItem().getValue()));

        runOnFx(treeView::searchNext);
        assertEquals("match-2", callOnFx(() -> treeView.getSelectionModel().getFocusedItem().getValue()));

        runOnFx(treeView::searchNext);
        assertEquals("match-3", callOnFx(() -> treeView.getSelectionModel().getFocusedItem().getValue()));

        runOnFx(treeView::searchNext);
        assertEquals("match-1", callOnFx(() -> treeView.getSelectionModel().getFocusedItem().getValue()));

        runOnFx(treeView::searchPrevious);
        assertEquals("match-3", callOnFx(() -> treeView.getSelectionModel().getFocusedItem().getValue()));
    }

    @Test
    void escapeClosesSearchOverlayAndReturnsFocusToTree() {
        runOnFx(() -> {
            treeView.openSearch("node");
            treeView.requestFocus();
            treeView.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ESCAPE, false, false, false, false));
        });

        assertFalse(callOnFx(treeView::isSearchOpen));
        assertTrue(callOnFx(() -> treeView.getScene().getFocusOwner() == treeView));
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
