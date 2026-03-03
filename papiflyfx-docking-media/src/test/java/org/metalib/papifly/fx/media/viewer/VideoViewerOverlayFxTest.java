package org.metalib.papifly.fx.media.viewer;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.media.MediaPlayer;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.media.controls.TransportBar;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApplicationExtension.class)
class VideoViewerOverlayFxTest {

    private VideoViewer viewer;

    @Start
    void start(Stage stage) {
        viewer = new VideoViewer();
        viewer.themeProperty().set(Theme.dark());
        String url = getClass().getResource("/sample-media/sample.mp4").toExternalForm();
        viewer.load(url);
        stage.setScene(new Scene(viewer, 640, 360));
        stage.show();
    }

    @Test
    void bottomScrimStaysInLowerBoundedArea(FxRobot robot) {
        robot.interact(() -> {});
        robot.interact(() -> {
            Region scrim = viewer.bottomScrimForTesting();
            double ratio = scrim.getHeight() / viewer.getHeight();
            assertTrue(ratio >= 0.14);
            assertTrue(ratio <= 0.31);
            assertEquals(viewer.getWidth(), scrim.getWidth(), 1.0);
            assertEquals(viewer.getHeight(), scrim.getLayoutY() + scrim.getHeight(), 1.0);
        });
    }

    @Test
    void controlsAutoHideOnlyWhenPlaying(FxRobot robot) throws Exception {
        AtomicReference<MediaPlayer> playerRef = new AtomicReference<>();
        AtomicReference<TransportBar> barRef = new AtomicReference<>();
        robot.interact(() -> {
            playerRef.set(viewer.playerForTesting());
            barRef.set(viewer.transportBarForTesting());
        });
        MediaPlayer player = playerRef.get();
        TransportBar bar = barRef.get();
        assertNotNull(player);
        assertNotNull(bar);

        robot.interact(() -> {
            Runnable onReady = player.getOnReady();
            if (onReady != null) onReady.run();
            Runnable onPaused = player.getOnPaused();
            if (onPaused != null) onPaused.run();
            bar.triggerIdleTimeout();
        });
        Thread.sleep(450L);
        robot.interact(() -> {
            assertEquals(TransportBar.PlaybackState.PAUSED, bar.getPlaybackState());
            assertTrue(bar.isControlsVisible());
        });

        robot.interact(() -> {
            Runnable onPlaying = player.getOnPlaying();
            if (onPlaying != null) onPlaying.run();
            bar.triggerIdleTimeout();
        });
        Thread.sleep(500L);
        robot.interact(() -> {
            assertEquals(TransportBar.PlaybackState.PLAYING, bar.getPlaybackState());
            assertFalse(bar.isControlsVisible());
        });
    }

    @Test
    void centerAffordanceTracksPlaybackState(FxRobot robot) {
        AtomicReference<MediaPlayer> playerRef = new AtomicReference<>();
        AtomicReference<StackPane> centerRef = new AtomicReference<>();
        robot.interact(() -> {
            playerRef.set(viewer.playerForTesting());
            centerRef.set(viewer.centerAffordanceForTesting());
        });
        MediaPlayer player = playerRef.get();
        StackPane center = centerRef.get();
        assertNotNull(player);
        assertNotNull(center);

        robot.interact(() -> {
            Runnable onReady = player.getOnReady();
            if (onReady != null) onReady.run();
            assertTrue(center.isVisible());
            Label glyph = (Label) center.getChildren().get(0);
            assertEquals("\u25B6", glyph.getText());
        });

        robot.interact(() -> {
            Runnable onPlaying = player.getOnPlaying();
            if (onPlaying != null) onPlaying.run();
            assertFalse(center.isVisible());
        });

        robot.interact(() -> {
            Runnable onPaused = player.getOnPaused();
            if (onPaused != null) onPaused.run();
            assertTrue(center.isVisible());
            Label glyph = (Label) center.getChildren().get(0);
            assertEquals("\u25B6", glyph.getText());
        });

        robot.interact(() -> {
            Runnable onEnd = player.getOnEndOfMedia();
            if (onEnd != null) onEnd.run();
            assertTrue(center.isVisible());
            Label glyph = (Label) center.getChildren().get(0);
            assertEquals("\u21BA", glyph.getText());
        });
    }
}
