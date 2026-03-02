package org.metalib.papifly.fx.media.player;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;

public class MediaPlayerService {

    private final ObjectProperty<MediaPlayer> playerProperty = new SimpleObjectProperty<>();
    private final DoubleProperty volume = new SimpleDoubleProperty(1.0);
    private MediaView boundView;

    public void load(String url) {
        disposePlayer();
        Media media = new Media(url);
        MediaPlayer player = new MediaPlayer(media);
        player.volumeProperty().bindBidirectional(volume);
        playerProperty.set(player);
        if (boundView != null) {
            boundView.setMediaPlayer(player);
        }
    }

    public void bind(MediaView view) {
        this.boundView = view;
        MediaPlayer player = playerProperty.get();
        if (player != null) {
            view.setMediaPlayer(player);
        }
    }

    public void unbind(MediaView view) {
        view.setMediaPlayer(null);
        if (view == boundView) {
            boundView = null;
        }
    }

    public void play()  { withPlayer(MediaPlayer::play); }
    public void pause() { withPlayer(MediaPlayer::pause); }
    public void stop()  { withPlayer(p -> { p.stop(); p.seek(Duration.ZERO); }); }

    public void seek(Duration d) { withPlayer(p -> p.seek(d)); }

    public void stepForward() {
        withPlayer(p -> p.seek(p.getCurrentTime().add(Duration.seconds(1.0 / 30.0))));
    }

    public void stepBackward() {
        withPlayer(p -> p.seek(p.getCurrentTime().subtract(Duration.seconds(1.0 / 30.0))));
    }

    public void seekRelative(double seconds) {
        withPlayer(p -> p.seek(p.getCurrentTime().add(Duration.seconds(seconds))));
    }

    public DoubleProperty volumeProperty() { return volume; }

    public ReadOnlyObjectProperty<MediaPlayer> playerProperty() { return playerProperty; }

    public void dispose() {
        if (boundView != null) {
            boundView.setMediaPlayer(null);
            boundView = null;
        }
        disposePlayer();
    }

    private void disposePlayer() {
        MediaPlayer old = playerProperty.get();
        if (old != null) {
            old.dispose();
            playerProperty.set(null);
        }
    }

    private void withPlayer(java.util.function.Consumer<MediaPlayer> action) {
        MediaPlayer p = playerProperty.get();
        if (p != null) action.accept(p);
    }
}
