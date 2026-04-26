package org.metalib.papifly.fx.samples.docks;

import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.metalib.papifly.fx.api.ribbon.RibbonCommand;
import org.metalib.papifly.fx.docking.api.LeafContentData;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.docks.DockManager;
import org.metalib.papifly.fx.docks.core.DockLeaf;
import org.metalib.papifly.fx.docks.core.DockTabGroup;
import org.metalib.papifly.fx.docks.ribbon.Ribbon;
import org.metalib.papifly.fx.docks.ribbon.RibbonManager;
import org.metalib.papifly.fx.docks.ribbon.RibbonPlacement;
import org.metalib.papifly.fx.samples.SampleScene;
import org.metalib.papifly.fx.samples.docks.ribbon.SampleRibbonProvider;

import java.util.List;

/**
 * Demonstrates two ribbon placements against one live dock manager.
 */
public class RibbonPlacementSample implements SampleScene {

    public static final String HOST_ID = "sample-ribbon-placement-host";
    public static final String TOP_RIBBON_ID = "sample-ribbon-placement-top-ribbon";
    public static final String LEFT_RIBBON_ID = "sample-ribbon-placement-left-ribbon";

    @Override
    public String category() {
        return "Docks";
    }

    @Override
    public String title() {
        return "Ribbon Placement";
    }

    @Override
    public Node build(Stage ownerStage, ObjectProperty<Theme> themeProperty) {
        return createHost(ownerStage, themeProperty);
    }

    private static BorderPane createHost(
        Stage ownerStage,
        ObjectProperty<Theme> themeProperty
    ) {
        DockManager dockManager = new DockManager();
        dockManager.themeProperty().bind(themeProperty);
        dockManager.setOwnerStage(ownerStage);

        DockTabGroup editors = dockManager.createTabGroup();
        editors.addLeaf(sampleLeaf(dockManager, "Landing.java", "sample.code", "sample-ribbon-placement-landing", "Code editor surface"));
        editors.addLeaf(sampleLeaf(dockManager, "Post.md", "sample.markdown", "sample-ribbon-placement-post", "Markdown authoring surface"));
        dockManager.setRoot(editors);

        RibbonManager ribbonManager = new RibbonManager(List.of(new SampleRibbonProvider()));
        ribbonManager.addQuickAccessCommand(RibbonCommand.of("sample.save", "Save", () -> {
        }));
        ribbonManager.addQuickAccessCommand(RibbonCommand.of("sample.undo", "Undo", () -> {
        }));

        ribbonManager.contextProperty().bind(dockManager.ribbonContextProperty());

        Ribbon topRibbon = createRibbon(ribbonManager, dockManager, TOP_RIBBON_ID, RibbonPlacement.TOP);
        Ribbon leftRibbon = createRibbon(ribbonManager, dockManager, LEFT_RIBBON_ID, RibbonPlacement.LEFT);

        BorderPane host = new BorderPane();
        host.setId(HOST_ID);
        host.getStyleClass().add("pf-ribbon-dock-host");
        host.setTop(topRibbon);
        host.setLeft(leftRibbon);
        host.setCenter(dockManager.getRootPane());
        host.setMinSize(0, 0);
        return host;
    }

    private static Ribbon createRibbon(
        RibbonManager ribbonManager,
        DockManager dockManager,
        String id,
        RibbonPlacement placement
    ) {
        Ribbon ribbon = new Ribbon(ribbonManager);
        ribbon.setId(id);
        ribbon.setPlacement(placement);
        ribbon.themeProperty().bind(dockManager.themeProperty());
        ribbon.setMinSize(0, 0);
        return ribbon;
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
        label.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        StackPane pane = new StackPane(label);
        pane.setMinSize(0, 0);
        return pane;
    }
}
