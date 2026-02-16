package org.metalib.papifly.fx.code.api;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.metalib.papifly.fx.code.document.Document;
import org.metalib.papifly.fx.code.state.EditorStateCodec;
import org.metalib.papifly.fx.code.state.EditorStateData;
import org.metalib.papifly.fx.docks.layout.data.LeafContentData;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApplicationExtension.class)
class CodeEditorIntegrationTest {

    private CodeEditor editor;

    @Start
    void start(Stage stage) {
        editor = new CodeEditor();
        Scene scene = new Scene(editor, 640, 480);
        stage.setScene(scene);
        stage.show();
    }

    @Test
    void setTextAndGetText() {
        runOnFx(() -> editor.setText("hello world"));
        WaitForAsyncUtils.waitForFxEvents();
        assertEquals("hello world", callOnFx(() -> editor.getText()));
    }

    @Test
    void setTextResetsCaretToOrigin() {
        runOnFx(() -> {
            editor.setText("hello\nworld");
            editor.getSelectionModel().moveCaret(1, 3);
            editor.setText("new text");
        });
        WaitForAsyncUtils.waitForFxEvents();
        assertEquals(0, callOnFx(() -> editor.getSelectionModel().getCaretLine()));
        assertEquals(0, callOnFx(() -> editor.getSelectionModel().getCaretColumn()));
    }

    @Test
    void documentIsAccessible() {
        assertNotNull(callOnFx(() -> editor.getDocument()));
    }

    @Test
    void viewportIsAccessible() {
        assertNotNull(callOnFx(() -> editor.getViewport()));
    }

    @Test
    void selectionModelIsAccessible() {
        assertNotNull(callOnFx(() -> editor.getSelectionModel()));
    }

    @Test
    void documentTextReflectsSetText() {
        runOnFx(() -> editor.setText("abc\ndef"));
        WaitForAsyncUtils.waitForFxEvents();
        assertEquals("abc\ndef", callOnFx(() -> editor.getDocument().getText()));
    }

    @Test
    void cursorLinePropertyUpdatesWithSelectionModel() {
        runOnFx(() -> {
            editor.setText("hello\nworld\nfoo");
            editor.getSelectionModel().moveCaret(2, 1);
        });
        WaitForAsyncUtils.waitForFxEvents();
        assertEquals(2, callOnFx(() -> editor.getCursorLine()));
    }

    @Test
    void cursorColumnPropertyUpdatesWithSelectionModel() {
        runOnFx(() -> {
            editor.setText("hello");
            editor.getSelectionModel().moveCaret(0, 3);
        });
        WaitForAsyncUtils.waitForFxEvents();
        assertEquals(3, callOnFx(() -> editor.getCursorColumn()));
    }

    @Test
    void undoRedoViaDocument() {
        runOnFx(() -> {
            editor.setText("initial");
            editor.getDocument().insert(7, " text");
        });
        WaitForAsyncUtils.waitForFxEvents();
        assertEquals("initial text", callOnFx(() -> editor.getText()));

        runOnFx(() -> editor.getDocument().undo());
        WaitForAsyncUtils.waitForFxEvents();
        assertEquals("initial", callOnFx(() -> editor.getText()));

        runOnFx(() -> editor.getDocument().redo());
        WaitForAsyncUtils.waitForFxEvents();
        assertEquals("initial text", callOnFx(() -> editor.getText()));
    }

    @Test
    void selectAllViaSelectionModel() {
        runOnFx(() -> {
            editor.setText("hello\nworld");
            editor.getSelectionModel().selectAll(editor.getDocument());
        });
        WaitForAsyncUtils.waitForFxEvents();
        assertTrue(callOnFx(() -> editor.getSelectionModel().hasSelection()));
        assertEquals("hello\nworld",
            callOnFx(() -> editor.getSelectionModel().getSelectedText(editor.getDocument())));
    }

    @Test
    void captureAndApplyStateRoundTrip() {
        runOnFx(() -> {
            editor.setFilePath("/test/file.java");
            editor.setLanguageId("java");
            editor.setText("content");
            editor.getSelectionModel().moveCaret(0, 3);
        });
        WaitForAsyncUtils.waitForFxEvents();

        var state = callOnFx(() -> editor.captureState());
        assertNotNull(state);
        assertEquals("/test/file.java", state.filePath());
        assertEquals("java", state.languageId());
    }

    @Test
    void applyStateMovesActualCaretModel() {
        runOnFx(() -> {
            editor.setText("line0\nline1\nline2");
            editor.applyState(new EditorStateData("", 1, 3, 0.0, "plain-text", List.of()));
        });
        WaitForAsyncUtils.waitForFxEvents();
        assertEquals(1, callOnFx(() -> editor.getSelectionModel().getCaretLine()));
        assertEquals(3, callOnFx(() -> editor.getSelectionModel().getCaretColumn()));
    }

    @Test
    void applyStateClampsCaretToDocumentBounds() {
        runOnFx(() -> {
            editor.setText("aa\nbbb");
            editor.applyState(new EditorStateData("", 99, 99, 0.0, "plain-text", List.of()));
        });
        WaitForAsyncUtils.waitForFxEvents();
        assertEquals(1, callOnFx(() -> editor.getSelectionModel().getCaretLine()));
        assertEquals(3, callOnFx(() -> editor.getSelectionModel().getCaretColumn()));
    }

    @Test
    void undoRedoShortcutKeepsCaretNearEditLocation() {
        runOnFx(() -> {
            editor.requestFocus();
            editor.setText("abc");
            editor.getDocument().insert(3, "d");
            editor.getSelectionModel().moveCaret(0, 4);
        });
        WaitForAsyncUtils.waitForFxEvents();

        fireShortcut(KeyCode.Z, false);
        assertEquals("abc", callOnFx(() -> editor.getText()));
        assertEquals(3, callOnFx(() -> editor.getSelectionModel().getCaretColumn()));

        fireShortcut(KeyCode.Y, false);
        assertEquals("abcd", callOnFx(() -> editor.getText()));
        assertEquals(4, callOnFx(() -> editor.getSelectionModel().getCaretColumn()));
    }

    @Test
    void captureStateUsesActualViewportScrollOffset() {
        runOnFx(() -> {
            editor.setText(buildLines(100));
            editor.applyCss();
            editor.layout();
            editor.setVerticalScrollOffset(100_000);
            editor.applyCss();
            editor.layout();
        });
        WaitForAsyncUtils.waitForFxEvents();

        double actualOffset = callOnFx(() -> editor.getViewport().getScrollOffset());
        EditorStateData state = callOnFx(() -> editor.captureState());
        assertEquals(actualOffset, state.verticalScrollOffset(), 0.0001);
    }

    @Test
    void disposeRemovesInputHandlers() {
        runOnFx(() -> editor.dispose());
        WaitForAsyncUtils.waitForFxEvents();
        assertNull(callOnFx(editor::getOnKeyPressed));
        assertNull(callOnFx(editor::getOnScroll));
    }

    @Test
    void adapterRestoreVersionZeroMigratesState() {
        CodeEditorStateAdapter adapter = new CodeEditorStateAdapter();
        Map<String, Object> state = EditorStateCodec.toMap(
            new EditorStateData("/tmp/demo.java", 0, 0, 0.0, "java", List.of(1, 3))
        );

        CodeEditor restored = callOnFx(() -> (CodeEditor) adapter.restore(
            new LeafContentData(CodeEditorFactory.FACTORY_ID, "editor-1", 0, state)
        ));

        assertEquals("/tmp/demo.java", callOnFx(restored::getFilePath));
        assertEquals("java", callOnFx(restored::getLanguageId));
        assertEquals(List.of(1, 3), callOnFx(restored::getFoldedLines));
        callOnFx(() -> {
            restored.dispose();
            return null;
        });
    }

    @Test
    void adapterRestoreUnknownVersionFallsBackToEmptyState() {
        CodeEditorStateAdapter adapter = new CodeEditorStateAdapter();
        Map<String, Object> state = EditorStateCodec.toMap(
            new EditorStateData("/tmp/wrong.java", 0, 0, 0.0, "java", List.of(9))
        );

        CodeEditor restored = callOnFx(() -> (CodeEditor) adapter.restore(
            new LeafContentData(CodeEditorFactory.FACTORY_ID, "editor-2", 99, state)
        ));

        assertEquals("", callOnFx(restored::getFilePath));
        assertEquals("plain-text", callOnFx(restored::getLanguageId));
        assertEquals(List.of(), callOnFx(restored::getFoldedLines));
        callOnFx(() -> {
            restored.dispose();
            return null;
        });
    }

    // --- Helpers ---

    private String buildLines(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append("line").append(i);
        }
        return sb.toString();
    }

    private void fireShortcut(KeyCode keyCode, boolean shift) {
        runOnFx(() -> editor.fireEvent(new KeyEvent(
            KeyEvent.KEY_PRESSED,
            "",
            "",
            keyCode,
            shift,
            true,
            false,
            false
        )));
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
    private <T> T callOnFx(java.util.concurrent.Callable<T> action) {
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
}
