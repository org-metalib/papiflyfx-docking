package org.metalib.papifly.fx.samples.github;

import javafx.beans.property.ObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.metalib.papifly.fx.docks.DockManager;
import org.metalib.papifly.fx.docks.core.DockTabGroup;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.github.api.GitHubRepoContext;
import org.metalib.papifly.fx.github.api.GitHubToolbarContribution;
import org.metalib.papifly.fx.samples.SampleScene;

import java.net.URI;

public class GitHubToolbarSample implements SampleScene {

    @Override
    public String category() {
        return "GitHub";
    }

    @Override
    public String title() {
        return "GitHub Toolbar";
    }

    @Override
    public Node build(Stage ownerStage, ObjectProperty<Theme> themeProperty) {
        DockManager dockManager = new DockManager();
        dockManager.themeProperty().bind(themeProperty);
        dockManager.setOwnerStage(ownerStage);

        StackPane content = new StackPane(new Label(
            "GitHub toolbar mounted at top in remote-only mode.\n" +
                "Set a token to enable PR creation and push actions for authenticated workflows."
        ));
        content.setMinSize(0, 0);
        content.setPadding(new Insets(24));

        var leaf = dockManager.createLeaf("Workspace", content);
        DockTabGroup group = dockManager.createTabGroup();
        group.addLeaf(leaf);
        dockManager.setRoot(group);

        GitHubRepoContext context = GitHubRepoContext.remoteOnly(
            URI.create("https://github.com/org-metalib/papiflyfx-docking")
        );
        GitHubToolbarContribution contribution = new GitHubToolbarContribution(
            context,
            GitHubToolbarContribution.Position.TOP
        );
        contribution.toolbar().bindThemeProperty(themeProperty);

        BorderPane wrapper = new BorderPane();
        contribution.mount(wrapper);
        wrapper.setCenter(dockManager.getRootPane());
        return wrapper;
    }
}
