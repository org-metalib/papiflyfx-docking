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

public class VideoPlayerSample implements SampleScene {

    @Override
    public String category() { return "Media"; }

    @Override
    public String title() { return "Video Player"; }

    @Override
    public Node build(Stage ownerStage, ObjectProperty<Theme> themeProperty) {
        DockManager dm = new DockManager();
        dm.themeProperty().bind(themeProperty);
        dm.setOwnerStage(ownerStage);

        ContentStateRegistry registry = new ContentStateRegistry();
        registry.register(new MediaViewerStateAdapter());
        dm.setContentStateRegistry(registry);
        dm.setContentFactory(new MediaViewerFactory());

        MediaViewer viewer = new MediaViewer();
        viewer.bindThemeProperty(themeProperty);
        String url = getClass().getResource("/sample-media/sample.mp4").toExternalForm();
        viewer.loadMedia(url);

        var leaf = dm.createLeaf("sample.mp4", viewer);
        leaf.setContentFactoryId(MediaViewerFactory.FACTORY_ID);

        DockTabGroup group = dm.createTabGroup();
        group.addLeaf(leaf);
        dm.setRoot(group);

        return dm.getRootPane();
    }
}
