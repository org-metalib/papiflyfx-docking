package org.metalib.papifly.fx.media.viewer;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.media.stream.EmbedUrlResolver;

public class EmbedViewer extends StackPane {

    private final WebView webView = new WebView();
    private final ObjectProperty<Theme> themeProperty = new SimpleObjectProperty<>();

    public EmbedViewer(String url) {
        webView.setContextMenuEnabled(false);
        webView.prefWidthProperty().bind(widthProperty());
        webView.prefHeightProperty().bind(heightProperty());
        getChildren().add(webView);
        wireTheme();
        load(url);
    }

    public void load(String url) {
        String embedUrl = EmbedUrlResolver.resolve(url);
        webView.getEngine().load(embedUrl);
    }

    public ObjectProperty<Theme> themeProperty() { return themeProperty; }

    public void dispose() {
        webView.getEngine().load(null);
    }

    private void wireTheme() {
        themeProperty.addListener((obs, o, t) -> {
            if (t == null) return;
            setBackground(new javafx.scene.layout.Background(
                new javafx.scene.layout.BackgroundFill(t.background(),
                    javafx.scene.layout.CornerRadii.EMPTY, Insets.EMPTY)));
        });
    }
}
