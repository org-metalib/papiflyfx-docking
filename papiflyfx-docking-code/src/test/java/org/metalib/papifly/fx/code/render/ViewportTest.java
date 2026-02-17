package org.metalib.papifly.fx.code.render;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.metalib.papifly.fx.code.document.Document;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApplicationExtension.class)
class ViewportTest {

    private Viewport viewport;
    private SelectionModel selectionModel;
    private Document document;

    @Start
    void start(Stage stage) {
        document = new Document("line0\nline1\nline2\nline3\nline4\nline5\nline6\nline7\nline8\nline9");
        selectionModel = new SelectionModel();
        viewport = new Viewport(selectionModel);
        viewport.setDocument(document);

        Scene scene = new Scene(viewport, 400, 200);
        stage.setScene(scene);
        stage.show();
    }

    @Test
    void viewportHasDocumentAfterSetup() {
        flushLayout();
        assertEquals(document, viewport.getDocument());
    }

    @Test
    void visibleLineCountIsPositive() {
        flushLayout();
        assertTrue(viewport.getVisibleLineCount() > 0, "Should have visible lines");
    }

    @Test
    void firstVisibleLineStartsAtZero() {
        flushLayout();
        assertEquals(0, viewport.getFirstVisibleLine());
    }

    @Test
    void scrollOffsetChangesFirstVisibleLine() {
        flushLayout();
        double lineHeight = viewport.getGlyphCache().getLineHeight();
        runOnFx(() -> viewport.setScrollOffset(lineHeight * 3));
        flushLayout();
        assertTrue(viewport.getFirstVisibleLine() <= 3);
    }

    @Test
    void documentChangeTriggersRedraw() {
        flushLayout();
        runOnFx(() -> document.insert(0, "new text\n"));
        flushLayout();
        assertTrue(viewport.getVisibleLineCount() > 0);
    }

    @Test
    void dirtyFlagClearedAfterLayout() {
        flushLayout();
        assertFalse(viewport.isDirty(), "dirty should be false after layout");
        runOnFx(() -> viewport.markDirty());
        assertTrue(viewport.isDirty());
        flushLayout();
        assertFalse(viewport.isDirty());
    }

    @Test
    void getLineAtYReturnsCorrectLine() {
        flushLayout();
        double lineHeight = viewport.getGlyphCache().getLineHeight();
        assertEquals(0, viewport.getLineAtY(0));
        assertEquals(1, viewport.getLineAtY(lineHeight));
        assertEquals(2, viewport.getLineAtY(lineHeight * 2 + 1));
    }

    @Test
    void getLineAtYReturnsMinusOneAboveViewportAndClampsBottom() {
        flushLayout();
        assertEquals(-1, viewport.getLineAtY(-10));
        assertEquals(9, viewport.getLineAtY(10000));
    }

    @Test
    void getColumnAtXReturnsNonNegative() {
        flushLayout();
        assertTrue(viewport.getColumnAtX(0) >= 0);
        assertTrue(viewport.getColumnAtX(100) > 0);
    }

    @Test
    void setDocumentNullRemovesListener() {
        flushLayout();
        runOnFx(() -> viewport.setDocument(null));
        flushLayout();
        document.insert(0, "test");
    }

    @Test
    void ensureCaretVisibleAdjustsScroll() {
        // Add enough lines so content height exceeds viewport height (200px)
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            if (i > 0) sb.append('\n');
            sb.append("line").append(i);
        }
        runOnFx(() -> {
            document.setText(sb.toString());
            viewport.setDocument(document);
        });
        flushLayout();

        runOnFx(() -> {
            selectionModel.moveCaret(49, 0);
            viewport.ensureCaretVisible();
        });
        flushLayout();
        assertTrue(viewport.getScrollOffset() > 0,
            "Scroll should adjust to show caret at line 49");
    }

    private void flushLayout() {
        // Apply CSS + layout on FX thread, then wait for completion
        runOnFx(() -> {
            viewport.applyCss();
            viewport.layout();
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
}
