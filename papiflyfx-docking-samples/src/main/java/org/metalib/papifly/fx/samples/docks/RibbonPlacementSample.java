package org.metalib.papifly.fx.samples.docks;

import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.metalib.papifly.fx.api.ribbon.RibbonCommand;
import org.metalib.papifly.fx.docking.api.LeafContentData;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.docks.DockManager;
import org.metalib.papifly.fx.docks.core.DockLeaf;
import org.metalib.papifly.fx.docks.core.DockTabGroup;
import org.metalib.papifly.fx.docks.ribbon.Ribbon;
import org.metalib.papifly.fx.docks.ribbon.RibbonDockHost;
import org.metalib.papifly.fx.docks.ribbon.RibbonManager;
import org.metalib.papifly.fx.docks.ribbon.RibbonPlacement;
import org.metalib.papifly.fx.samples.SampleScene;
import org.metalib.papifly.fx.samples.docks.ribbon.SampleRibbonProvider;

import java.util.List;

/**
 * Compares the top ribbon and compact side-toolbar placement using the same
 * sample commands.
 */
public class RibbonPlacementSample implements SampleScene {

    public static final String TOP_HOST_ID = "sample-ribbon-placement-top-host";
    public static final String LEFT_HOST_ID = "sample-ribbon-placement-left-host";

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
        RibbonDockHost topHost = createHost(ownerStage, themeProperty, RibbonPlacement.TOP, TOP_HOST_ID);
        RibbonDockHost leftHost = createHost(ownerStage, themeProperty, RibbonPlacement.LEFT, LEFT_HOST_ID);

        HBox comparison = new HBox(12.0, topHost, leftHost);
        comparison.setMinSize(0, 0);
        HBox.setHgrow(topHost, Priority.ALWAYS);
        HBox.setHgrow(leftHost, Priority.ALWAYS);
        return comparison;
    }

    private static RibbonDockHost createHost(
        Stage ownerStage,
        ObjectProperty<Theme> themeProperty,
        RibbonPlacement placement,
        String id
    ) {
        DockManager dockManager = new DockManager();
        dockManager.themeProperty().bind(themeProperty);
        dockManager.setOwnerStage(ownerStage);

        DockTabGroup editors = dockManager.createTabGroup();
        editors.addLeaf(sampleLeaf(dockManager, "Landing.java", "sample.code", id + "-landing", "Code editor surface"));
        editors.addLeaf(sampleLeaf(dockManager, "Post.md", "sample.markdown", id + "-post", "Markdown authoring surface"));
        dockManager.setRoot(editors);

        RibbonManager ribbonManager = new RibbonManager(List.of(new SampleRibbonProvider()));
        ribbonManager.addQuickAccessCommand(RibbonCommand.of("sample.save", "Save", () -> {
        }));
        ribbonManager.addQuickAccessCommand(RibbonCommand.of("sample.undo", "Undo", () -> {
        }));

        RibbonDockHost host = new RibbonDockHost(dockManager, ribbonManager, new Ribbon());
        host.setId(id);
        host.setPlacement(placement);
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
        label.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        StackPane pane = new StackPane(label);
        pane.setMinSize(0, 0);
        return pane;
    }
}
