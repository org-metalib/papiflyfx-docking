package org.metalib.papifly.fx.media.controls;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.media.player.MediaPlayerService;
import org.metalib.papifly.fx.media.theme.MediaThemeMapper;

public class TransportBar extends HBox {

    private static final double ICON_SIZE = 16.0;
    private static final double AUTO_HIDE_SECS = 2.0;

    private final ObjectProperty<Theme> themeProperty = new SimpleObjectProperty<>();
    private final MediaPlayerService service;

    private final Canvas playBtn  = iconCanvas();
    private final Canvas stopBtn  = iconCanvas();
    private final Canvas muteBtn  = iconCanvas();
    private final Slider seekBar  = new Slider(0, 1, 0);
    private final Label  timeLabel = new Label("0:00 / 0:00");
    private final Slider volSlider = new Slider(0, 1, 1);

    private boolean isPlaying = false;
    private boolean seeking   = false;

    private final FadeTransition fadeIn  = new FadeTransition(Duration.millis(300), this);
    private final FadeTransition fadeOut = new FadeTransition(Duration.millis(500), this);
    private final PauseTransition idleTimer = new PauseTransition(Duration.seconds(AUTO_HIDE_SECS));

    public TransportBar(MediaPlayerService service) {
        this.service = service;
        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(4, 8, 4, 8));
        setSpacing(8.0);
        setOpacity(0.0);

        HBox.setHgrow(seekBar, Priority.ALWAYS);
        volSlider.setMaxWidth(80.0);
        volSlider.valueProperty().bindBidirectional(service.volumeProperty());

        getChildren().addAll(playBtn, stopBtn, seekBar, timeLabel, muteBtn, volSlider);

        wirePlayButton();
        wireStopButton();
        wireMuteButton();
        wireSeekBar();
        wirePlayerStatus();
        wireAutoHide();
        wireTheme();
    }

    public void showFor(javafx.scene.Node parent) {
        parent.addEventFilter(MouseEvent.MOUSE_MOVED, e -> resetIdle());
        parent.addEventFilter(MouseEvent.MOUSE_ENTERED, e -> fadeInNow());
    }

    public ObjectProperty<Theme> themeProperty() { return themeProperty; }

    public void applyTheme(Theme t) {
        Color bg = MediaThemeMapper.toColor(MediaThemeMapper.controlBackground(t));
        setBackground(new javafx.scene.layout.Background(
            new javafx.scene.layout.BackgroundFill(bg.deriveColor(0, 1, 1, 0.85),
                new javafx.scene.layout.CornerRadii(t.cornerRadius()),
                Insets.EMPTY)));
        timeLabel.setFont(t.contentFont());
        timeLabel.setTextFill(MediaThemeMapper.controlForeground(t));
        paintPlayIcon(MediaThemeMapper.toColor(MediaThemeMapper.controlForeground(t)));
        paintStopIcon(MediaThemeMapper.toColor(MediaThemeMapper.controlForeground(t)));
        paintMuteIcon(MediaThemeMapper.toColor(MediaThemeMapper.controlForeground(t)));
    }

    // --- private wiring ---

    private void wirePlayButton() {
        playBtn.setOnMouseClicked(e -> {
            MediaPlayer p = service.playerProperty().get();
            if (p == null) return;
            if (p.getStatus() == MediaPlayer.Status.PLAYING) {
                service.pause();
            } else {
                service.play();
            }
        });
    }

    private void wireStopButton() {
        stopBtn.setOnMouseClicked(e -> service.stop());
    }

    private void wireMuteButton() {
        muteBtn.setOnMouseClicked(e -> {
            MediaPlayer p = service.playerProperty().get();
            if (p != null) p.setMute(!p.isMute());
        });
    }

    private void wireSeekBar() {
        seekBar.setOnMousePressed(e  -> seeking = true);
        seekBar.setOnMouseReleased(e -> {
            seeking = false;
            service.seek(Duration.seconds(seekBar.getValue()));
        });
    }

    private void wirePlayerStatus() {
        service.playerProperty().addListener((obs, old, player) -> {
            if (old != null) {
                old.currentTimeProperty().removeListener(this::onTimeChanged);
                old.statusProperty().removeListener(this::onStatusChanged);
            }
            if (player != null) {
                player.currentTimeProperty().addListener(this::onTimeChanged);
                player.statusProperty().addListener(this::onStatusChanged);
                player.setOnReady(() -> {
                    Duration total = player.getTotalDuration();
                    seekBar.setMax(total.toSeconds());
                });
            }
        });
    }

    private void onTimeChanged(javafx.beans.value.ObservableValue<? extends Duration> obs,
                               Duration old, Duration now) {
        if (seeking) return;
        seekBar.setValue(now.toSeconds());
        MediaPlayer p = service.playerProperty().get();
        if (p != null) {
            timeLabel.setText(fmt(now) + " / " + fmt(p.getTotalDuration()));
        }
    }

    private void onStatusChanged(javafx.beans.value.ObservableValue<? extends MediaPlayer.Status> obs,
                                 MediaPlayer.Status old, MediaPlayer.Status now) {
        isPlaying = now == MediaPlayer.Status.PLAYING;
        Theme t = themeProperty.get();
        Color c = t != null
            ? MediaThemeMapper.toColor(MediaThemeMapper.controlForeground(t))
            : Color.WHITE;
        paintPlayIcon(c);
        if (isPlaying) resetIdle(); else fadeInNow();
    }

    private void wireAutoHide() {
        fadeIn.setToValue(1.0);
        fadeOut.setToValue(0.0);
        idleTimer.setOnFinished(e -> { if (isPlaying) fadeOut.playFromStart(); });
    }

    private void wireTheme() {
        themeProperty.addListener((obs, o, t) -> { if (t != null) applyTheme(t); });
    }

    private void fadeInNow() {
        fadeOut.stop();
        fadeIn.playFromStart();
        resetIdle();
    }

    private void resetIdle() {
        idleTimer.stop();
        idleTimer.playFromStart();
    }

    private static String fmt(Duration d) {
        if (d == null || d.isUnknown()) return "0:00";
        int s = (int) d.toSeconds();
        return (s / 60) + ":" + String.format("%02d", s % 60);
    }

    private static Canvas iconCanvas() {
        return new Canvas(ICON_SIZE, ICON_SIZE);
    }

    private void paintPlayIcon(Color c) {
        GraphicsContext gc = playBtn.getGraphicsContext2D();
        gc.clearRect(0, 0, ICON_SIZE, ICON_SIZE);
        gc.setFill(c);
        if (isPlaying) {
            gc.fillRect(2, 2, 4, 12);
            gc.fillRect(10, 2, 4, 12);
        } else {
            gc.fillPolygon(new double[]{2, 14, 2}, new double[]{1, 8, 15}, 3);
        }
    }

    private void paintStopIcon(Color c) {
        GraphicsContext gc = stopBtn.getGraphicsContext2D();
        gc.clearRect(0, 0, ICON_SIZE, ICON_SIZE);
        gc.setFill(c);
        gc.fillRect(2, 2, 12, 12);
    }

    private void paintMuteIcon(Color c) {
        GraphicsContext gc = muteBtn.getGraphicsContext2D();
        gc.clearRect(0, 0, ICON_SIZE, ICON_SIZE);
        gc.setFill(c);
        gc.fillPolygon(new double[]{2, 8, 8, 2}, new double[]{5, 2, 14, 11}, 4);
        gc.fillRect(9, 5, 5, 6);
    }
}
