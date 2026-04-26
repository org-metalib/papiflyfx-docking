package org.metalib.papifly.fx.samples.github;

import javafx.beans.property.ObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.metalib.papifly.fx.docks.DockManager;
import org.metalib.papifly.fx.docks.core.DockLeaf;
import org.metalib.papifly.fx.docks.core.DockTabGroup;
import org.metalib.papifly.fx.docks.ribbon.Ribbon;
import org.metalib.papifly.fx.docks.ribbon.RibbonDockHost;
import org.metalib.papifly.fx.docks.ribbon.RibbonManager;
import org.metalib.papifly.fx.docking.api.LeafContentData;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.github.api.GitHubRibbonActions;
import org.metalib.papifly.fx.github.ribbon.GitHubRibbonProvider;
import org.metalib.papifly.fx.samples.SampleScene;

import java.util.List;

/**
 * Dedicated sample for the GitHub ribbon provider. The provider list is kept
 * explicit so the demo focuses on GitHub actions while still using the shared
 * RibbonProvider and RibbonDockHost runtime path.
 */
public class GitHubRibbonSample implements SampleScene {

    public static final String STATUS_LABEL_ID = "github-ribbon-sample-status";
    public static final String LOG_AREA_ID = "github-ribbon-sample-log";

    private static final String CONTENT_TYPE = "sample.github.ribbon";

    @Override
    public String category() {
        return "GitHub";
    }

    @Override
    public String title() {
        return "GitHub Ribbon";
    }

    @Override
    public Node build(Stage ownerStage, ObjectProperty<Theme> themeProperty) {
        DockManager dockManager = new DockManager();
        dockManager.themeProperty().bind(themeProperty);
        dockManager.setOwnerStage(ownerStage);

        DockTabGroup workspace = dockManager.createTabGroup();
        workspace.addLeaf(createWorkbenchLeaf(dockManager));
        dockManager.setRoot(workspace);

        RibbonManager ribbonManager = new RibbonManager(List.of());
        RibbonDockHost host = new RibbonDockHost(dockManager, ribbonManager, new Ribbon());
        host.setMinSize(0, 0);

        ribbonManager.getProviders().setAll(new GitHubRibbonProvider());
        return host;
    }

    private static DockLeaf createWorkbenchLeaf(DockManager dockManager) {
        GitHubRibbonWorkbench content = new GitHubRibbonWorkbench();
        DockLeaf leaf = dockManager.createLeaf("GitHub Workspace", content);
        leaf.setContentFactoryId(CONTENT_TYPE);
        leaf.setContentData(LeafContentData.of(CONTENT_TYPE, "sample://github/ribbon", 1));
        return leaf;
    }

    private static final class GitHubRibbonWorkbench extends BorderPane implements GitHubRibbonActions {

        private final Label statusLabel = new Label("Ready for local sample actions");
        private final TextArea logArea = new TextArea();
        private int commits;
        private int stagedFiles;

        private GitHubRibbonWorkbench() {
            getStyleClass().add("pf-github-ribbon-sample");
            setMinSize(0, 0);
            setPadding(new Insets(18));

            Label title = new Label("Repository: org-metalib/papiflyfx-docking");
            title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

            statusLabel.setId(STATUS_LABEL_ID);
            statusLabel.setStyle("-fx-font-weight: bold;");

            GridPane summary = new GridPane();
            summary.setHgap(18);
            summary.setVgap(8);
            addSummary(summary, 0, "Branch", "feature/ribbon-samples");
            addSummary(summary, 1, "Ahead / behind", "2 ahead, 1 behind");
            addSummary(summary, 2, "Working tree", "3 local changes");
            addSummary(summary, 3, "Mode", "offline deterministic demo");

            logArea.setId(LOG_AREA_ID);
            logArea.setEditable(false);
            logArea.setWrapText(true);
            logArea.setText("Sample log initialized. Ribbon commands only update this local state.\n");
            VBox.setVgrow(logArea, Priority.ALWAYS);

            VBox content = new VBox(12, title, statusLabel, summary, logArea);
            content.setMinSize(0, 0);
            setCenter(content);
        }

        private static void addSummary(GridPane grid, int row, String label, String value) {
            Label key = new Label(label);
            key.setStyle("-fx-font-weight: bold;");
            Label text = new Label(value);
            grid.add(key, 0, row);
            grid.add(text, 1, row);
        }

        @Override
        public boolean canPull() {
            return true;
        }

        @Override
        public boolean canPush() {
            return true;
        }

        @Override
        public boolean canFetch() {
            return true;
        }

        @Override
        public boolean canCreateBranch() {
            return true;
        }

        @Override
        public boolean canMerge() {
            return false;
        }

        @Override
        public boolean canRebase() {
            return false;
        }

        @Override
        public boolean canPullRequest() {
            return true;
        }

        @Override
        public boolean canOpenIssues() {
            return true;
        }

        @Override
        public boolean canCommit() {
            return true;
        }

        @Override
        public boolean canStage() {
            return true;
        }

        @Override
        public boolean canDiscard() {
            return true;
        }

        @Override
        public void pull() {
            record("Pull", "Sample pull completed from origin/main");
        }

        @Override
        public void push() {
            record("Push", "Sample push published feature/ribbon-samples");
        }

        @Override
        public void fetch() {
            record("Fetch", "Fetched sample remote references");
        }

        @Override
        public void createBranch() {
            record("New Branch", "Created sample branch feature/ribbon-safe-action");
        }

        @Override
        public void merge() {
            record("Merge", "Merge is intentionally disabled in this demo");
        }

        @Override
        public void rebase() {
            record("Rebase", "Rebase is intentionally disabled in this demo");
        }

        @Override
        public void pullRequest() {
            record("Pull Request", "Prepared sample pull request #42");
        }

        @Override
        public void issues() {
            record("Issues", "Listed sample issues without opening a browser");
        }

        @Override
        public void commit() {
            commits++;
            record("Commit", "Created local sample commit " + commits);
        }

        @Override
        public void stage() {
            stagedFiles += 3;
            record("Stage", "Staged " + stagedFiles + " sample files total");
        }

        @Override
        public void discard() {
            stagedFiles = 0;
            record("Discard", "Cleared sample staged changes");
        }

        private void record(String command, String message) {
            statusLabel.setText(command + ": " + message);
            logArea.appendText(command + " - " + message + "\n");
        }
    }
}
