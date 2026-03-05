package org.metalib.papifly.fx.samples.hugo;

import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.metalib.papifly.fx.docks.DockManager;
import org.metalib.papifly.fx.docks.core.DockLeaf;
import org.metalib.papifly.fx.docks.core.DockTabGroup;
import org.metalib.papifly.fx.docks.layout.ContentStateRegistry;
import org.metalib.papifly.fx.docking.api.LeafContentData;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.hugo.api.HugoPreviewConfig;
import org.metalib.papifly.fx.hugo.api.HugoPreviewFactory;
import org.metalib.papifly.fx.hugo.api.HugoPreviewPane;
import org.metalib.papifly.fx.hugo.api.HugoPreviewStateAdapter;
import org.metalib.papifly.fx.samples.SampleScene;

import java.nio.file.Files;
import java.nio.file.Path;

public class HugoPreviewSample implements SampleScene {

    @Override
    public String category() {
        return "Hugo";
    }

    @Override
    public String title() {
        return "Hugo Preview";
    }

    @Override
    public Node build(Stage ownerStage, ObjectProperty<Theme> themeProperty) {
        Path sampleSite = createSampleSite();
        if (sampleSite == null) {
            Label label = new Label("Unable to create Hugo sample site");
            StackPane pane = new StackPane(label);
            pane.setMinSize(0, 0);
            return pane;
        }

        DockManager dockManager = new DockManager();
        dockManager.themeProperty().bind(themeProperty);
        dockManager.setOwnerStage(ownerStage);

        ContentStateRegistry registry = new ContentStateRegistry();
        registry.register(new HugoPreviewStateAdapter());
        dockManager.setContentStateRegistry(registry);
        dockManager.setContentFactory(new HugoPreviewFactory(sampleSite));

        HugoPreviewPane previewPane = new HugoPreviewPane(new HugoPreviewConfig(
            sampleSite,
            "hugo:sample",
            "/",
            1313,
            true,
            false
        ));

        DockLeaf leaf = dockManager.createLeaf("Hugo Preview", previewPane);
        leaf.setContentFactoryId(HugoPreviewFactory.FACTORY_ID);
        leaf.setContentData(LeafContentData.of(
            HugoPreviewFactory.FACTORY_ID,
            "hugo:sample",
            HugoPreviewStateAdapter.VERSION
        ));

        DockTabGroup group = dockManager.createTabGroup();
        group.addLeaf(leaf);
        dockManager.setRoot(group);

        return dockManager.getRootPane();
    }

    private Path createSampleSite() {
        try {
            Path site = Files.createTempDirectory("papiflyfx-hugo-sample-");
            Files.createDirectories(site.resolve("content"));
            Files.createDirectories(site.resolve("layouts"));

            Files.writeString(site.resolve("hugo.toml"), """
                baseURL = "http://127.0.0.1:1313/"
                languageCode = "en-us"
                title = "PapiflyFX Hugo Sample"
                """);
            Files.writeString(site.resolve("content").resolve("_index.md"), """
                ---
                title: "PapiflyFX Hugo Preview"
                ---

                # PapiflyFX Hugo Preview

                This page is served by `hugo server` and rendered inside JavaFX WebView.
                """);
            Files.writeString(site.resolve("layouts").resolve("index.html"), """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                  <title>{{ .Site.Title }}</title>
                  <style>
                    body { font-family: sans-serif; margin: 24px; line-height: 1.4; }
                    .hint { color: #666; margin-top: 12px; }
                  </style>
                </head>
                <body>
                  {{ .Content }}
                  <p class="hint">Live reload and navigation are managed by the Hugo preview dock content.</p>
                  <p><a href="https://gohugo.io/">Open Hugo website</a></p>
                </body>
                </html>
                """);
            return site;
        } catch (Exception ex) {
            return null;
        }
    }
}
