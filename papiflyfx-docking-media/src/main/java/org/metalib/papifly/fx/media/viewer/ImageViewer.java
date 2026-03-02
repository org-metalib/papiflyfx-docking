package org.metalib.papifly.fx.media.viewer;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.media.controls.ZoomControls;
import org.metalib.papifly.fx.media.player.ImageLoaderService;
import org.metalib.papifly.fx.media.theme.MediaThemeMapper;

public class ImageViewer extends StackPane {

    private static final double MIN_ZOOM = 0.05;
    private static final double MAX_ZOOM = 16.0;

    private final ImageLoaderService loaderService = new ImageLoaderService();
    private final ImageView imageView = new ImageView();
    private final ProgressIndicator progress = new ProgressIndicator();
    private final Scale scaleXform = new Scale(1, 1, 0, 0);
    private final Translate panXform = new Translate(0, 0);

    private final DoubleProperty zoomLevel = new SimpleDoubleProperty(1.0);
    private final ObjectProperty<Theme> themeProperty = new SimpleObjectProperty<>();

    private double dragStartX, dragStartY;
    private final ZoomControls zoomControls;

    public ImageViewer() {
        setAlignment(Pos.CENTER);

        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.getTransforms().addAll(scaleXform, panXform);
        imageView.fitWidthProperty().bind(widthProperty());
        imageView.fitHeightProperty().bind(heightProperty());

        zoomControls = new ZoomControls(this::adjustZoom, this::resetZoom);
        StackPane.setAlignment(zoomControls, Pos.TOP_RIGHT);

        getChildren().addAll(imageView, progress, zoomControls);

        wireLoader();
        wireZoom();
        wirePan();
        wireTheme();
    }

    public void load(String url) { loaderService.load(url); }

    public ObjectProperty<Theme> themeProperty() { return themeProperty; }

    public double getZoomLevel() { return zoomLevel.get(); }
    public void setZoomLevel(double z) { zoomLevel.set(clamp(z, MIN_ZOOM, MAX_ZOOM)); }

    public double getPanX() { return panXform.getX(); }
    public double getPanY() { return panXform.getY(); }

    public void dispose() { loaderService.dispose(); }

    private void wireLoader() {
        loaderService.imageProperty().addListener((obs, o, img) -> {
            imageView.setImage(img);
            progress.setVisible(false);
        });
        loaderService.progressProperty().addListener((obs, o, n) -> {
            progress.setVisible(n.doubleValue() < 1.0);
            progress.setProgress(n.doubleValue());
        });
        loaderService.errorProperty().addListener((obs, o, ex) -> {
            if (ex != null) progress.setVisible(false);
        });
    }

    private void wireZoom() {
        zoomLevel.addListener((obs, o, n) -> {
            double z = n.doubleValue();
            scaleXform.setX(z);
            scaleXform.setY(z);
            zoomControls.setZoomLevel(z);
        });

        setOnScroll((ScrollEvent e) -> {
            double factor = e.getDeltaY() > 0 ? 1.1 : 0.9;
            adjustZoom(factor - 1.0);
            e.consume();
        });

        setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                resetZoom();
            }
        });
    }

    private void wirePan() {
        setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                dragStartX = e.getSceneX() - panXform.getX();
                dragStartY = e.getSceneY() - panXform.getY();
            }
        });
        setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                panXform.setX(e.getSceneX() - dragStartX);
                panXform.setY(e.getSceneY() - dragStartY);
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
                    javafx.geometry.Insets.EMPTY)));
            zoomControls.applyTheme(t);
        });
    }

    private void adjustZoom(double delta) {
        setZoomLevel(zoomLevel.get() + delta * zoomLevel.get());
    }

    private void resetZoom() {
        zoomLevel.set(1.0);
        panXform.setX(0);
        panXform.setY(0);
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
