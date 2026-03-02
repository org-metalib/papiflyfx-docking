package org.metalib.papifly.fx.media.viewer;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.media.MediaView;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.media.controls.TransportBar;
import org.metalib.papifly.fx.media.player.MediaPlayerService;

public class VideoViewer extends StackPane {

    private final MediaPlayerService playerService = new MediaPlayerService();
    private final MediaView mediaView = new MediaView();
    private final TransportBar transportBar = new TransportBar(playerService);
    private final ObjectProperty<Theme> themeProperty = new SimpleObjectProperty<>();

    public VideoViewer() {
        setAlignment(Pos.BOTTOM_CENTER);

        mediaView.setPreserveRatio(true);
        mediaView.setSmooth(true);
        mediaView.fitWidthProperty().bind(widthProperty());
        mediaView.fitHeightProperty().bind(heightProperty());

        StackPane.setAlignment(transportBar, Pos.BOTTOM_CENTER);
        StackPane.setMargin(transportBar, new Insets(0, 8, 8, 8));

        getChildren().addAll(mediaView, transportBar);

        transportBar.themeProperty().bind(themeProperty);
        transportBar.showFor(this);

        wireSceneReparent();
        wireKeyboard();
        wireTheme();
        wireVisibility();
    }

    public void load(String url) {
        playerService.load(url);
        playerService.bind(mediaView);
    }

    public ObjectProperty<Theme> themeProperty() { return themeProperty; }

    public long getCurrentTimeMs() {
        var p = playerService.playerProperty().get();
        return p != null ? (long) p.getCurrentTime().toMillis() : 0L;
    }

    public double getVolume() {
        return playerService.volumeProperty().get();
    }

    public void applyPlaybackState(long timeMs, double volume, boolean muted) {
        playerService.playerProperty().addListener((obs, o, player) -> {
            if (player != null) {
                player.setOnReady(() -> {
                    player.seek(javafx.util.Duration.millis(timeMs));
                    player.volumeProperty().set(volume);
                    player.setMute(muted);
                });
            }
        });
    }

    public void dispose() {
        playerService.dispose();
    }

    private void wireSceneReparent() {
        sceneProperty().addListener((obs, o, scene) -> {
            if (scene != null) {
                playerService.bind(mediaView);
            } else {
                playerService.unbind(mediaView);
            }
        });
    }

    private void wireKeyboard() {
        setFocusTraversable(true);
        addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            switch (e.getCode()) {
                case SPACE -> { if (playerService.playerProperty().get() != null
                        && playerService.playerProperty().get().getStatus()
                            == javafx.scene.media.MediaPlayer.Status.PLAYING) {
                        playerService.pause();
                    } else {
                        playerService.play();
                    }
                    e.consume(); }
                case M     -> { var p = playerService.playerProperty().get();
                                if (p != null) p.setMute(!p.isMute()); e.consume(); }
                case LEFT  -> { playerService.seekRelative(-5); e.consume(); }
                case RIGHT -> { playerService.seekRelative(+5); e.consume(); }
                case J     -> { playerService.stepBackward();   e.consume(); }
                case L     -> { playerService.stepForward();    e.consume(); }
                default    -> {}
            }
        });
    }

    private void wireTheme() {
        themeProperty.addListener((obs, o, t) -> {
            if (t == null) return;
            setBackground(new javafx.scene.layout.Background(
                new javafx.scene.layout.BackgroundFill(
                    t.background(),
                    javafx.scene.layout.CornerRadii.EMPTY,
                    Insets.EMPTY)));
        });
    }

    private void wireVisibility() {
        visibleProperty().addListener((obs, o, visible) -> {
            var p = playerService.playerProperty().get();
            if (p != null) p.setMute(!visible);
        });
    }
}
