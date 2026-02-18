package org.metalib.papifly.fx.code.theme;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.metalib.papifly.fx.code.api.CodeEditor;
import org.metalib.papifly.fx.docking.api.Theme;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ApplicationExtension.class)
class CodeEditorThemeIntegrationTest {

    private CodeEditor editor;
    private ObjectProperty<Theme> themeProperty;

    @Start
    void start(Stage stage) {
        editor = new CodeEditor();
        editor.setText("hello world");
        themeProperty = new SimpleObjectProperty<>(Theme.dark());

        Scene scene = new Scene(editor, 400, 300);
        stage.setScene(scene);
        stage.show();
    }

    @Test
    void bindThemePropertyAppliesDarkTheme() {
        runOnFx(() -> editor.bindThemeProperty(themeProperty));
        WaitForAsyncUtils.waitForFxEvents();

        CodeEditorTheme editorTheme = editor.getEditorTheme();
        assertNotNull(editorTheme);
        assertEquals(Theme.dark().background(), editorTheme.editorBackground());
    }

    @Test
    void switchToLightThemeUpdatesEditor() {
        runOnFx(() -> editor.bindThemeProperty(themeProperty));
        WaitForAsyncUtils.waitForFxEvents();

        runOnFx(() -> themeProperty.set(Theme.light()));
        WaitForAsyncUtils.waitForFxEvents();

        CodeEditorTheme editorTheme = editor.getEditorTheme();
        assertEquals(Theme.light().background(), editorTheme.editorBackground());
        // Foreground should be dark for light theme
        Color fg = (Color) editorTheme.editorForeground();
        assertTrue(fg.getBrightness() < 0.3);
    }

    @Test
    void unbindStopsUpdates() {
        runOnFx(() -> editor.bindThemeProperty(themeProperty));
        WaitForAsyncUtils.waitForFxEvents();

        runOnFx(() -> editor.unbindThemeProperty());
        runOnFx(() -> themeProperty.set(Theme.light()));
        WaitForAsyncUtils.waitForFxEvents();

        // After unbinding, editor should still have the dark background from before unbind
        CodeEditorTheme editorTheme = editor.getEditorTheme();
        assertEquals(Theme.dark().background(), editorTheme.editorBackground());
    }

    @Test
    void setEditorThemeDirectly() {
        CodeEditorTheme light = CodeEditorTheme.light();
        runOnFx(() -> editor.setEditorTheme(light));
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals(light, editor.getEditorTheme());
        assertEquals(light, editor.getViewport().getTheme());
        assertEquals(light, editor.getGutterView().getTheme());
        assertEquals(light, editor.getSearchController().getTheme());
    }

    @Test
    void setNullEditorThemeDefaultsToDark() {
        runOnFx(() -> editor.setEditorTheme(null));
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals(CodeEditorTheme.dark(), editor.getEditorTheme());
    }

    @Test
    void disposeUnbindsTheme() {
        runOnFx(() -> editor.bindThemeProperty(themeProperty));
        WaitForAsyncUtils.waitForFxEvents();

        runOnFx(() -> editor.dispose());
        // Should not throw when theme changes after dispose
        runOnFx(() -> themeProperty.set(Theme.light()));
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    void viewportGutterSearchAllReceiveTheme() {
        runOnFx(() -> editor.bindThemeProperty(themeProperty));
        WaitForAsyncUtils.waitForFxEvents();

        CodeEditorTheme expected = CodeEditorThemeMapper.map(Theme.dark());
        assertEquals(expected, editor.getViewport().getTheme());
        assertEquals(expected, editor.getGutterView().getTheme());
        assertEquals(expected, editor.getSearchController().getTheme());
    }

    private void runOnFx(Runnable action) {
        if (javafx.application.Platform.isFxApplicationThread()) {
            action.run();
            return;
        }
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        javafx.application.Platform.runLater(() -> {
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
