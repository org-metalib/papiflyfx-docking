package org.metalib.papifly.fx.media.viewer;

import javafx.scene.Scene;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.media.controls.TransportBar;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApplicationExtension.class)
class AudioViewerTransportBarFxTest {

    private AudioViewer viewer;
    private RuntimeException mediaLoadFailure;

    @Start
    void start(Stage stage) {
        viewer = new AudioViewer();
        viewer.themeProperty().set(Theme.dark());
        String url = getClass().getResource("/sample-media/sample.mp4").toExternalForm();
        try {
            viewer.load(url);
        } catch (RuntimeException ex) {
            mediaLoadFailure = ex;
        }
        stage.setScene(new Scene(viewer, 720, 300));
        stage.show();
    }

    @Test
    void transportBarRemainsCompactAndVisibleOnPause(FxRobot robot) {
        assumeMediaBackendAvailable();
        robot.interact(() -> {});
        robot.interact(() -> {
            TransportBar bar = viewer.transportBarForTesting();
            assertTrue(bar.getHeight() < viewer.getHeight() * 0.5);
        });

        robot.interact(() -> {
            MediaPlayer player = viewer.playerForTesting();
            assertNotNull(player);
            Runnable onPaused = player.getOnPaused();
            if (onPaused != null) onPaused.run();
        });

        robot.interact(() -> {
            TransportBar bar = viewer.transportBarForTesting();
            assertEquals(TransportBar.PlaybackState.PAUSED, bar.getPlaybackState());
            assertTrue(bar.isControlsVisible());
        });
    }

    private void assumeMediaBackendAvailable() {
        RuntimeException failure = mediaLoadFailure;
        Assumptions.assumeTrue(failure == null && !viewer.isErrorState(), () ->
            "JavaFX media backend unavailable in this environment: " + failure);
    }
}
