package org.metalib.papifly.fx.samples.media;

import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;
import javafx.stage.Stage;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.media.api.MediaViewer;
import org.metalib.papifly.fx.samples.SampleScene;

public class HlsStreamSample implements SampleScene {

    // Public domain Apple HLS test stream
    private static final String HLS_URL =
        "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_4x3/bipbop_4x3_variant.m3u8";

    @Override
    public String category() { return "Media"; }

    @Override
    public String title() { return "HLS Stream"; }

    @Override
    public Node build(Stage stage, ObjectProperty<Theme> themeProperty) {
        MediaViewer viewer = new MediaViewer();
        viewer.bindThemeProperty(themeProperty);
        viewer.loadMedia(HLS_URL);
        return viewer;
    }
}
