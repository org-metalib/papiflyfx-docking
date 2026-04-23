package org.metalib.papifly.fx.samples.hugo;

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
import org.metalib.papifly.fx.hugo.api.HugoPreviewFactory;
import org.metalib.papifly.fx.hugo.api.HugoRibbonActions;
import org.metalib.papifly.fx.hugo.ribbon.HugoRibbonProvider;
import org.metalib.papifly.fx.samples.SampleScene;

import java.util.List;

/**
 * Dedicated sample for the Hugo ribbon provider. The content exposes
 * HugoRibbonActions directly so commands stay deterministic and never start
 * hugo, mutate a real site, or open external tools.
 */
public class HugoRibbonSample implements SampleScene {

    public static final String STATUS_LABEL_ID = "hugo-ribbon-sample-status";
    public static final String LOG_AREA_ID = "hugo-ribbon-sample-log";

    private static final String MARKDOWN_TYPE = "sample.hugo.markdown";
    private static final String MARKDOWN_CONTENT_ID = "/content/posts/ribbon-sample.md";

    @Override
    public String category() {
        return "Hugo";
    }

    @Override
    public String title() {
        return "Hugo Ribbon";
    }

    @Override
    public Node build(Stage ownerStage, ObjectProperty<Theme> themeProperty) {
        DockManager dockManager = new DockManager();
        dockManager.themeProperty().bind(themeProperty);
        dockManager.setOwnerStage(ownerStage);

        DockTabGroup workspace = dockManager.createTabGroup();
        workspace.addLeaf(createEditorLeaf(dockManager));
        workspace.addLeaf(createPreviewLeaf(dockManager));
        workspace.setActiveTab(0);
        dockManager.setRoot(workspace);

        RibbonManager ribbonManager = new RibbonManager(List.of());
        RibbonDockHost host = new RibbonDockHost(dockManager, ribbonManager, new Ribbon());
        host.setMinSize(0, 0);

        ribbonManager.getProviders().setAll(new HugoRibbonProvider());
        return host;
    }

    private static DockLeaf createEditorLeaf(DockManager dockManager) {
        HugoRibbonEditor content = new HugoRibbonEditor();
        DockLeaf leaf = dockManager.createLeaf("content/posts/ribbon-sample.md", content);
        leaf.setContentFactoryId(MARKDOWN_TYPE);
        leaf.setContentData(LeafContentData.of(MARKDOWN_TYPE, MARKDOWN_CONTENT_ID, 1));
        return leaf;
    }

    private static DockLeaf createPreviewLeaf(DockManager dockManager) {
        Label label = new Label("Static local preview placeholder. No Hugo CLI is started by this sample.");
        BorderPane preview = new BorderPane(label);
        preview.setPadding(new Insets(18));
        preview.setMinSize(0, 0);

        DockLeaf leaf = dockManager.createLeaf("Hugo Preview", preview);
        leaf.setContentFactoryId(HugoPreviewFactory.FACTORY_ID);
        leaf.setContentData(LeafContentData.of(HugoPreviewFactory.FACTORY_ID, "hugo:sample-ribbon", 1));
        return leaf;
    }

    private static final class HugoRibbonEditor extends BorderPane implements HugoRibbonActions {

        private final Label statusLabel = new Label("Server stopped; commands are sample-local");
        private final TextArea logArea = new TextArea();
        private boolean serverRunning;

        private HugoRibbonEditor() {
            getStyleClass().add("pf-hugo-ribbon-sample");
            setMinSize(0, 0);
            setPadding(new Insets(18));

            Label title = new Label("Hugo content editor");
            title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

            statusLabel.setId(STATUS_LABEL_ID);
            statusLabel.setStyle("-fx-font-weight: bold;");

            GridPane summary = new GridPane();
            summary.setHgap(18);
            summary.setVgap(8);
            addSummary(summary, 0, "Site root", "sample://hugo-site");
            addSummary(summary, 1, "Active file", MARKDOWN_CONTENT_ID);
            addSummary(summary, 2, "Draft mode", "enabled locally");
            addSummary(summary, 3, "Execution", "no Hugo CLI or browser launches");

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
        public boolean isServerRunning() {
            return serverRunning;
        }

        @Override
        public boolean canRunHugoCommands() {
            return true;
        }

        @Override
        public void toggleServer() {
            serverRunning = !serverRunning;
            record("Server", serverRunning ? "Sample server marked running" : "Sample server marked stopped");
        }

        @Override
        public void newContent(String relativePath) {
            record("New Content", "Prepared sample content at " + relativePath);
        }

        @Override
        public void build() {
            record("Build", "Rendered sample site output in memory");
        }

        @Override
        public void mod(String subCommand) {
            record("Hugo Mod", "Recorded hugo mod " + subCommand);
        }

        @Override
        public void env() {
            record("Env", "Displayed sample Hugo environment");
        }

        @Override
        public void frontMatterTemplate() {
            record("Template", "Inserted front matter helper");
        }

        @Override
        public void insertShortcode(String shortcodeName) {
            record("Shortcode", "Inserted " + shortcodeName + " shortcode helper");
        }

        private void record(String command, String message) {
            statusLabel.setText(command + ": " + message);
            logArea.appendText(command + " - " + message + "\n");
        }
    }
}
