package org.metalib.papifly.fx.samples.docks;

import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.metalib.papifly.fx.api.ribbon.PapiflyCommand;
import org.metalib.papifly.fx.docks.DockManager;
import org.metalib.papifly.fx.docks.core.DockLeaf;
import org.metalib.papifly.fx.docks.core.DockTabGroup;
import org.metalib.papifly.fx.docks.ribbon.Ribbon;
import org.metalib.papifly.fx.docks.ribbon.RibbonDockHost;
import org.metalib.papifly.fx.docks.ribbon.RibbonManager;
import org.metalib.papifly.fx.docking.api.LeafContentData;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.samples.SampleScene;

/**
 * Demonstrates the Phase 2 ribbon shell mounted above a DockManager.
 */
public class RibbonShellSample implements SampleScene {

    @Override
    public String category() {
        return "Docks";
    }

    @Override
    public String title() {
        return "Ribbon Shell";
    }

    @Override
    public Node build(Stage ownerStage, ObjectProperty<Theme> themeProperty) {
        DockManager dockManager = new DockManager();
        dockManager.themeProperty().bind(themeProperty);
        dockManager.setOwnerStage(ownerStage);

        DockTabGroup editors = dockManager.createTabGroup();
        editors.addLeaf(sampleLeaf(dockManager, "Landing.java", "sample.code", "editor-landing",
            "Code editor surface"));
        editors.addLeaf(sampleLeaf(dockManager, "Post.md", "sample.markdown", "post-home",
            "Markdown authoring surface"));

        DockTabGroup sidePanel = dockManager.createTabGroup();
        sidePanel.addLeaf(sampleLeaf(dockManager, "Preview", "sample.preview", "preview-home",
            "Live preview panel"));
        sidePanel.addLeaf(sampleLeaf(dockManager, "Outline", "sample.outline", "outline-home",
            "Document outline"));

        dockManager.setRoot(dockManager.createHorizontalSplit(editors, sidePanel, 0.68));

        RibbonManager ribbonManager = new RibbonManager();
        ribbonManager.addQuickAccessCommand(PapiflyCommand.of("sample.save", "Save", () -> {}));
        ribbonManager.addQuickAccessCommand(PapiflyCommand.of("sample.undo", "Undo", () -> {}));
        ribbonManager.addQuickAccessCommand(PapiflyCommand.of("sample.redo", "Redo", () -> {}));

        RibbonDockHost host = new RibbonDockHost(dockManager, ribbonManager, new Ribbon());
        host.setMinSize(0, 0);
        return host;
    }

    private static DockLeaf sampleLeaf(
        DockManager dockManager,
        String title,
        String typeKey,
        String contentId,
        String message
    ) {
        DockLeaf leaf = dockManager.createLeaf(title, centeredLabel(message));
        leaf.setContentFactoryId(typeKey);
        leaf.setContentData(LeafContentData.of(typeKey, contentId, 1));
        return leaf;
    }

    private static StackPane centeredLabel(String message) {
        Label label = new Label(message);
        label.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");
        StackPane pane = new StackPane(label);
        pane.setMinSize(0, 0);
        return pane;
    }
}
