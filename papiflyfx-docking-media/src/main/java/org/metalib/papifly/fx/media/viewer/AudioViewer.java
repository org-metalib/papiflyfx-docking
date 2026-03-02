package org.metalib.papifly.fx.media.viewer;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.media.controls.TransportBar;
import org.metalib.papifly.fx.media.player.MediaPlayerService;
import org.metalib.papifly.fx.media.theme.MediaThemeMapper;

public class AudioViewer extends StackPane {

    private final MediaPlayerService playerService = new MediaPlayerService();
    private final TransportBar transportBar = new TransportBar(playerService);
    private final Canvas waveformPlaceholder = new Canvas(200, 80);
    private final ObjectProperty<Theme> themeProperty = new SimpleObjectProperty<>();

    public AudioViewer() {
        setAlignment(Pos.BOTTOM_CENTER);
        StackPane.setAlignment(transportBar, Pos.BOTTOM_CENTER);
        StackPane.setMargin(transportBar, new Insets(0, 8, 8, 8));

        getChildren().addAll(waveformPlaceholder, transportBar);
        transportBar.themeProperty().bind(themeProperty);
        transportBar.showFor(this);

        waveformPlaceholder.widthProperty().bind(widthProperty().multiply(0.8));
        wireTheme();
    }

    public void load(String url) { playerService.load(url); }

    public ObjectProperty<Theme> themeProperty() { return themeProperty; }

    public long getCurrentTimeMs() {
        var p = playerService.playerProperty().get();
        return p != null ? (long) p.getCurrentTime().toMillis() : 0L;
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

    public void dispose() { playerService.dispose(); }

    private void wireTheme() {
        themeProperty.addListener((obs, o, t) -> {
            if (t == null) return;
            setBackground(new javafx.scene.layout.Background(
                new javafx.scene.layout.BackgroundFill(t.background(),
                    javafx.scene.layout.CornerRadii.EMPTY, Insets.EMPTY)));
            paintWaveformPlaceholder(MediaThemeMapper.toColor(MediaThemeMapper.accent(t)));
        });
    }

    private void paintWaveformPlaceholder(Color c) {
        double w = waveformPlaceholder.getWidth();
        double h = waveformPlaceholder.getHeight();
        GraphicsContext gc = waveformPlaceholder.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);
        gc.setStroke(c.deriveColor(0, 1, 1, 0.5));
        gc.setLineWidth(1.5);
        double mid = h / 2.0;
        double step = w / 60.0;
        for (int i = 0; i < 60; i++) {
            double amp = mid * (0.2 + 0.8 * Math.abs(Math.sin(i * 0.42)));
            gc.strokeLine(i * step, mid - amp, i * step, mid + amp);
        }
    }
}
