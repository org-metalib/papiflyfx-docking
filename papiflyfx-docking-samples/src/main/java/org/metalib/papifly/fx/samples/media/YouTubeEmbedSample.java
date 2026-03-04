package org.metalib.papifly.fx.samples.media;

import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;
import javafx.stage.Stage;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.media.api.MediaViewer;
import org.metalib.papifly.fx.samples.SampleScene;

public class YouTubeEmbedSample implements SampleScene {

    private static final String YOUTUBE_WATCH =
        "https://youtu.be/1jrHOfCofoI?si=0jC8iFMwkx7GJugl";
    //"https://www.youtube.com/embed/1jrHOfCofoI?si=Wpgy3uLNSOWkZKgP";

    @Override
    public String category() { return "Media"; }

    @Override
    public String title() { return "YouTube Embed"; }

    @Override
    public Node build(Stage stage, ObjectProperty<Theme> themeProperty) {
        MediaViewer viewer = new MediaViewer();
        viewer.bindThemeProperty(themeProperty);
        viewer.loadMedia(YOUTUBE_WATCH);   // converted to embed URL internally
        return viewer;
    }
}
