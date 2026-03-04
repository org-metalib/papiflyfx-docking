package org.metalib.papifly.fx.media.viewer;

import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.layout.Region;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.layout.StackPane;
import javafx.geometry.Pos;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.junit.jupiter.api.Assumptions;
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
    private Stage stage;
    private RuntimeException mediaLoadFailure;

    @Start
    void start(Stage stage) {
        this.stage = stage;
        viewer = new VideoViewer();
        viewer.themeProperty().set(Theme.dark());
        String url = getClass().getResource("/sample-media/sample.mp4").toExternalForm();
        try {
            viewer.load(url);
        } catch (RuntimeException ex) {
            mediaLoadFailure = ex;
        }
        stage.setScene(new Scene(viewer, 640, 360));
        stage.show();
    }

    @Test
    void bottomScrimStaysInLowerBoundedArea(FxRobot robot) {
        assumeMediaBackendAvailable();
        robot.interact(() -> {});
        robot.interact(() -> {
            Region scrim = viewer.bottomScrimForTesting();
            MediaView mediaView = findDescendant(viewer, MediaView.class);
            assertNotNull(mediaView);
            Bounds mediaBounds = effectiveMediaBounds(mediaView);
            double ratio = scrim.getHeight() / mediaBounds.getHeight();
            assertTrue(ratio >= 0.14);
            assertTrue(ratio <= 0.31);
            assertEquals(mediaBounds.getWidth(), scrim.getWidth(), 1.0);
            assertEquals(mediaBounds.getMinX(), scrim.getLayoutX(), 1.0);
            assertEquals(mediaBounds.getMaxY(), scrim.getLayoutY() + scrim.getHeight(), 1.0);
        });
    }

    @Test
    void controlsAutoHideOnlyWhenPlaying(FxRobot robot) {
        assumeMediaBackendAvailable();
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
        robot.interact(() -> {
            assertEquals(TransportBar.PlaybackState.PAUSED, bar.getPlaybackState());
            assertTrue(bar.isControlsVisible());
        });

        robot.interact(() -> {
            Runnable onPlaying = player.getOnPlaying();
            if (onPlaying != null) onPlaying.run();
            bar.triggerIdleTimeout();
        });
        robot.interact(() -> {});
        robot.interact(() -> {
            assertEquals(TransportBar.PlaybackState.PLAYING, bar.getPlaybackState());
            assertFalse(bar.isControlsVisible());
        });
    }

    @Test
    void centerAffordanceTracksPlaybackState(FxRobot robot) {
        assumeMediaBackendAvailable();
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

    @Test
    void mediaStaysCenteredAfterViewportResize(FxRobot robot) {
        assumeMediaBackendAvailable();
        robot.interact(() -> {
            stage.setWidth(360);
            stage.setHeight(720);
        });
        robot.interact(() -> {});
        robot.interact(() -> {});

        robot.interact(() -> {
            MediaView mediaView = findDescendant(viewer, MediaView.class);
            assertNotNull(mediaView);
            assertEquals(Pos.CENTER, StackPane.getAlignment(mediaView));
            double renderedWidth = mediaView.getBoundsInParent().getWidth();
            double renderedHeight = mediaView.getBoundsInParent().getHeight();
            double expectedX = (viewer.getWidth() - renderedWidth) / 2.0;
            double expectedY = (viewer.getHeight() - renderedHeight) / 2.0;
            assertEquals(expectedX, mediaView.getBoundsInParent().getMinX(), 1.5);
            assertEquals(expectedY, mediaView.getBoundsInParent().getMinY(), 1.5);
        });
    }

    @Test
    void viewerClipTracksViewportResize(FxRobot robot) {
        assumeMediaBackendAvailable();
        robot.interact(() -> {
            assertTrue(viewer.getClip() instanceof Rectangle);
            Rectangle clip = (Rectangle) viewer.getClip();
            assertEquals(viewer.getWidth(), clip.getWidth(), 1.0);
            assertEquals(viewer.getHeight(), clip.getHeight(), 1.0);
        });

        robot.interact(() -> {
            stage.setWidth(380);
            stage.setHeight(760);
        });
        robot.interact(() -> {});
        robot.interact(() -> {});

        robot.interact(() -> {
            assertTrue(viewer.getClip() instanceof Rectangle);
            Rectangle clip = (Rectangle) viewer.getClip();
            assertEquals(viewer.getWidth(), clip.getWidth(), 1.0);
            assertEquals(viewer.getHeight(), clip.getHeight(), 1.0);
        });
    }

    private Bounds effectiveMediaBounds(MediaView mediaView) {
        Bounds bounds = mediaView.getBoundsInParent();
        if (bounds.getWidth() <= 0.0 || bounds.getHeight() <= 0.0) {
            return new BoundingBox(0.0, 0.0, viewer.getWidth(), viewer.getHeight());
        }
        return bounds;
    }

    private static <T> T findDescendant(Node root, Class<T> type) {
        if (type.isInstance(root)) return type.cast(root);
        if (root instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                T found = findDescendant(child, type);
                if (found != null) return found;
            }
        }
        return null;
    }

    private void assumeMediaBackendAvailable() {
        RuntimeException failure = mediaLoadFailure;
        Assumptions.assumeTrue(failure == null, () ->
            "JavaFX media backend unavailable in this environment: " + failure);
    }
}
