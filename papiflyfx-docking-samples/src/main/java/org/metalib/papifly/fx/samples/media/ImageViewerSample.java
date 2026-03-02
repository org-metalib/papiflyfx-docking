package org.metalib.papifly.fx.samples.media;

import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;
import javafx.stage.Stage;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.media.api.MediaViewer;
import org.metalib.papifly.fx.samples.SampleScene;

public class ImageViewerSample implements SampleScene {

    @Override
    public String category() { return "Media"; }

    @Override
    public String title() { return "Image Viewer"; }

    @Override
    public Node build(Stage stage, ObjectProperty<Theme> themeProperty) {
        MediaViewer viewer = new MediaViewer();
        viewer.bindThemeProperty(themeProperty);
        String url = getClass().getResource("/sample-media/sample.png").toExternalForm();
        viewer.loadMedia(url);
        return viewer;
    }
}
