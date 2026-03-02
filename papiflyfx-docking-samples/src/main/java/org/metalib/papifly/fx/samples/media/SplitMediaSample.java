package org.metalib.papifly.fx.samples.media;

import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;
import javafx.stage.Stage;
import org.metalib.papifly.fx.docks.DockManager;
import org.metalib.papifly.fx.docks.core.DockTabGroup;
import org.metalib.papifly.fx.docks.layout.ContentStateRegistry;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.media.api.MediaViewer;
import org.metalib.papifly.fx.media.api.MediaViewerFactory;
import org.metalib.papifly.fx.media.api.MediaViewerStateAdapter;
import org.metalib.papifly.fx.samples.SampleScene;

public class SplitMediaSample implements SampleScene {

    @Override
    public String category() { return "Media"; }

    @Override
    public String title() { return "Split: Image + Video"; }

    @Override
    public Node build(Stage ownerStage, ObjectProperty<Theme> themeProperty) {
        DockManager dm = new DockManager();
        dm.themeProperty().bind(themeProperty);
        dm.setOwnerStage(ownerStage);

        ContentStateRegistry registry = new ContentStateRegistry();
        registry.register(new MediaViewerStateAdapter());
        dm.setContentStateRegistry(registry);
        dm.setContentFactory(new MediaViewerFactory());

        MediaViewer imageViewer = new MediaViewer();
        imageViewer.bindThemeProperty(themeProperty);
        imageViewer.loadMedia(getClass().getResource("/sample-media/sample.png").toExternalForm());

        MediaViewer videoViewer = new MediaViewer();
        videoViewer.bindThemeProperty(themeProperty);
        videoViewer.loadMedia(getClass().getResource("/sample-media/sample.mp4").toExternalForm());

        var imageLeaf = dm.createLeaf("Image", imageViewer);
        imageLeaf.setContentFactoryId(MediaViewerFactory.FACTORY_ID);

        var videoLeaf = dm.createLeaf("Video", videoViewer);
        videoLeaf.setContentFactoryId(MediaViewerFactory.FACTORY_ID);

        DockTabGroup leftGroup = dm.createTabGroup();
        leftGroup.addLeaf(imageLeaf);

        DockTabGroup rightGroup = dm.createTabGroup();
        rightGroup.addLeaf(videoLeaf);

        dm.setRoot(dm.createHorizontalSplit(leftGroup, rightGroup, 0.5));

        return dm.getRootPane();
    }
}
