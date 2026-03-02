package org.metalib.papifly.fx.media.controls;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.media.theme.MediaThemeMapper;

import java.util.function.Consumer;

public class ZoomControls extends HBox {

    private final Label zoomLabel = new Label("100%");
    private final Canvas plusBtn  = new Canvas(14, 14);
    private final Canvas minusBtn = new Canvas(14, 14);
    private final Canvas resetBtn = new Canvas(14, 14);

    public ZoomControls(Consumer<Double> onZoom, Runnable onReset) {
        setAlignment(Pos.CENTER);
        setSpacing(4);
        setPadding(new Insets(2, 6, 2, 6));
        getChildren().addAll(minusBtn, zoomLabel, plusBtn, resetBtn);

        minusBtn.setOnMouseClicked(e -> onZoom.accept(-0.1));
        plusBtn.setOnMouseClicked(e  -> onZoom.accept(+0.1));
        resetBtn.setOnMouseClicked(e -> onReset.run());
    }

    public void setZoomLevel(double level) {
        zoomLabel.setText((int)(level * 100) + "%");
    }

    public void applyTheme(Theme t) {
        Color c = MediaThemeMapper.toColor(MediaThemeMapper.controlForeground(t));
        zoomLabel.setFont(t.contentFont());
        zoomLabel.setTextFill(c);
        paintPlus(c);
        paintMinus(c);
        paintReset(c);
    }

    private void paintPlus(Color c) {
        GraphicsContext gc = plusBtn.getGraphicsContext2D();
        gc.clearRect(0, 0, 14, 14);
        gc.setFill(c);
        gc.fillRect(6, 1, 2, 12);
        gc.fillRect(1, 6, 12, 2);
    }

    private void paintMinus(Color c) {
        GraphicsContext gc = minusBtn.getGraphicsContext2D();
        gc.clearRect(0, 0, 14, 14);
        gc.setFill(c);
        gc.fillRect(1, 6, 12, 2);
    }

    private void paintReset(Color c) {
        GraphicsContext gc = resetBtn.getGraphicsContext2D();
        gc.clearRect(0, 0, 14, 14);
        gc.setFill(c);
        gc.strokeOval(2, 2, 10, 10);
    }
}
