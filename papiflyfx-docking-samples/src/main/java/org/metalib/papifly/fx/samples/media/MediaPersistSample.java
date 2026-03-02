package org.metalib.papifly.fx.samples.media;

import javafx.beans.property.ObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.metalib.papifly.fx.docks.DockManager;
import org.metalib.papifly.fx.docks.core.DockTabGroup;
import org.metalib.papifly.fx.docks.layout.ContentStateRegistry;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.media.api.MediaViewer;
import org.metalib.papifly.fx.media.api.MediaViewerFactory;
import org.metalib.papifly.fx.media.api.MediaViewerStateAdapter;
import org.metalib.papifly.fx.samples.SampleScene;

public class MediaPersistSample implements SampleScene {

    @Override
    public String category() { return "Media"; }

    @Override
    public String title() { return "Persist & Restore"; }

    @Override
    public Node build(Stage ownerStage, ObjectProperty<Theme> themeProperty) {
        DockManager dm = buildDockManager(ownerStage, themeProperty);

        Button saveBtn = new Button("Save Session");
        Button loadBtn = new Button("Load Session");
        saveBtn.setDisable(false);
        loadBtn.setDisable(true);

        final String[] savedSession = {null};

        saveBtn.setOnAction(e -> {
            savedSession[0] = dm.saveSessionToString();
            loadBtn.setDisable(false);
        });
        loadBtn.setOnAction(e -> {
            if (savedSession[0] != null) {
                dm.restoreSessionFromString(savedSession[0]);
            }
        });

        HBox toolbar = new HBox(8, saveBtn, loadBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(6, 10, 6, 10));

        BorderPane root = new BorderPane();
        root.setTop(toolbar);
        root.setCenter(dm.getRootPane());
        return root;
    }

    private DockManager buildDockManager(Stage ownerStage, ObjectProperty<Theme> themeProperty) {
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
        imageViewer.setZoomLevel(2.0);  // pre-set zoom; captured by saveSessionToString()

        MediaViewer videoViewer = new MediaViewer();
        videoViewer.bindThemeProperty(themeProperty);
        videoViewer.loadMedia(getClass().getResource("/sample-media/sample.mp4").toExternalForm());

        var imageLeaf = dm.createLeaf("Image (zoom 2×)", imageViewer);
        imageLeaf.setContentFactoryId(MediaViewerFactory.FACTORY_ID);

        var videoLeaf = dm.createLeaf("Video", videoViewer);
        videoLeaf.setContentFactoryId(MediaViewerFactory.FACTORY_ID);

        DockTabGroup group = dm.createTabGroup();
        group.addLeaf(imageLeaf);
        group.addLeaf(videoLeaf);
        dm.setRoot(group);

        return dm;
    }
}
