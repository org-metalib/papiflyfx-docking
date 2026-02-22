package org.metalib.papifly.fx.code.render;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.metalib.papifly.fx.code.document.Document;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.function.BooleanSupplier;

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
    void horizontalOffsetShiftsHitTestColumnsWhenWrapIsOff() {
        runOnFx(() -> {
            document.setText("0123456789abcdef".repeat(20));
            viewport.setHorizontalScrollOffset(viewport.getGlyphCache().getCharWidth() * 5);
        });
        flushLayout();

        int columnAtLeftEdge = callOnFx(() -> viewport.getHitPosition(0, 0).column());
        assertEquals(5, columnAtLeftEdge);
    }

    @Test
    void horizontalOffsetIsForcedToZeroInWrapMode() {
        runOnFx(() -> {
            document.setText("x".repeat(500));
            viewport.setHorizontalScrollOffset(200);
            viewport.setWordWrap(true);
        });
        flushLayout();

        assertEquals(0.0, callOnFx(viewport::getHorizontalScrollOffset), 0.0001);
        assertFalse(callOnFx(viewport::isHorizontalScrollbarVisible));
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

    @Test
    void collapsingSelectionClearsHighlightFromPreviouslySelectedLines() {
        runOnFx(() -> {
            document.setText("line0\nline1\nline2\nline3\nline4\nline5");
            selectionModel.moveCaret(0, 0);
            selectionModel.moveCaretWithSelection(3, 2);
        });
        flushLayout();

        Color expectedSelectionColor = themeColor(true);
        Color selectedLineColor = sampleLineBackgroundColor(1);
        assertTrue(colorsClose(selectedLineColor, expectedSelectionColor),
            "Line should be painted with selection color before collapsing selection");

        runOnFx(() -> selectionModel.moveCaret(4, 0));
        flushLayout();

        Color expectedBackground = themeColor(false);
        Color clearedLineColor = sampleLineBackgroundColor(1);
        assertTrue(colorsClose(clearedLineColor, expectedBackground),
            "Previously selected line should be repainted with editor background");
    }

    @Test
    void caretBlinkTogglesVisibilityWhenActive() {
        runOnFx(() -> {
            viewport.setCaretBlinkTimings(Duration.millis(20), Duration.millis(80));
            viewport.setCaretBlinkActive(true);
        });
        flushLayout();

        assertTrue(callOnFx(viewport::isCaretVisible));
        assertTrue(waitForCondition(() -> callOnFx(() -> !viewport.isCaretVisible()), 1_500),
            "Caret should toggle to hidden while blinking is active");
    }

    @Test
    void caretMovementResetsBlinkToVisible() {
        runOnFx(() -> {
            viewport.setCaretBlinkTimings(Duration.millis(20), Duration.millis(80));
            viewport.setCaretBlinkActive(true);
        });
        flushLayout();

        assertTrue(waitForCondition(() -> callOnFx(() -> !viewport.isCaretVisible()), 1_500),
            "Caret should become hidden after blink cycle");

        runOnFx(() -> selectionModel.moveCaret(2, 1));
        WaitForAsyncUtils.waitForFxEvents();
        assertTrue(callOnFx(viewport::isCaretVisible),
            "Caret should be immediately visible after caret movement");
    }

    @Test
    void caretHiddenWhenBlinkInactive() {
        runOnFx(() -> {
            viewport.setCaretBlinkTimings(Duration.millis(20), Duration.millis(80));
            viewport.setCaretBlinkActive(true);
            viewport.setCaretBlinkActive(false);
        });
        flushLayout();
        assertFalse(callOnFx(viewport::isCaretVisible));
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

    @SuppressWarnings("unchecked")
    private <T> T callOnFx(Callable<T> action) {
        if (Platform.isFxApplicationThread()) {
            try {
                return action.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        CountDownLatch latch = new CountDownLatch(1);
        Object[] result = new Object[1];
        Platform.runLater(() -> {
            try {
                result[0] = action.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
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
        return (T) result[0];
    }

    private boolean waitForCondition(BooleanSupplier condition, long timeoutMillis) {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return true;
            }
            WaitForAsyncUtils.waitForFxEvents();
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return condition.getAsBoolean();
    }

    private Color sampleLineBackgroundColor(int lineIndex) {
        final Color[] color = new Color[1];
        runOnFx(() -> {
            double lineHeight = viewport.getGlyphCache().getLineHeight();
            double y = lineIndex * lineHeight + (lineHeight / 2.0);
            WritableImage image = viewport.snapshot(null, null);
            int sampleX = Math.max(0, (int) Math.min(image.getWidth() - 2, image.getWidth() * 0.25));
            int sampleY = Math.max(0, (int) Math.min(image.getHeight() - 2, y));
            color[0] = image.getPixelReader().getColor(sampleX, sampleY);
        });
        return color[0];
    }

    private boolean colorsClose(Color actual, Color expected) {
        double tolerance = 0.03;
        return Math.abs(actual.getRed() - expected.getRed()) <= tolerance
            && Math.abs(actual.getGreen() - expected.getGreen()) <= tolerance
            && Math.abs(actual.getBlue() - expected.getBlue()) <= tolerance;
    }

    private Color themeColor(boolean selection) {
        final Color[] color = new Color[1];
        runOnFx(() -> color[0] = (Color) (selection
            ? viewport.getTheme().selectionColor()
            : viewport.getTheme().editorBackground()));
        return color[0];
    }
}
