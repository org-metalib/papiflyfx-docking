package org.metalib.papifly.fx.docks.ribbon;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import org.metalib.papifly.fx.api.ribbon.PapiflyCommand;
import org.metalib.papifly.fx.api.ribbon.RibbonGroupSpec;

import java.util.Objects;

/**
 * Visual container for a ribbon group label, optional launcher, and controls.
 */
public class RibbonGroup extends VBox {

    private final RibbonGroupSpec spec;
    private final FlowPane controlsPane = new FlowPane(Orientation.HORIZONTAL, 8.0, 8.0);
    private final Label label = new Label();
    private final Button launcherButton = new Button("...");

    /**
     * Creates a ribbon group for the supplied spec.
     *
     * @param spec group specification
     * @param classLoader class loader used for icon resolution
     */
    public RibbonGroup(RibbonGroupSpec spec, ClassLoader classLoader) {
        this.spec = Objects.requireNonNull(spec, "spec");
        getStyleClass().add("pf-ribbon-group");

        controlsPane.getStyleClass().add("pf-ribbon-group-controls");
        label.getStyleClass().add("pf-ribbon-group-label");
        label.setText(spec.label());

        launcherButton.getStyleClass().add("pf-ribbon-group-launcher");
        configureLauncher(spec.dialogLauncher());

        BorderPane footer = new BorderPane();
        footer.getStyleClass().add("pf-ribbon-group-footer");
        footer.setCenter(label);
        footer.setRight(launcherButton);

        controlsPane.getChildren().setAll(spec.controls().stream()
            .filter(Objects::nonNull)
            .map(control -> RibbonControlFactory.createGroupControl(control, classLoader))
            .toList());

        getChildren().addAll(controlsPane, footer);
    }

    /**
     * Returns the backing group specification.
     *
     * @return backing group specification
     */
    public RibbonGroupSpec getSpec() {
        return spec;
    }

    private void configureLauncher(PapiflyCommand launcher) {
        if (launcher == null) {
            launcherButton.setManaged(false);
            launcherButton.setVisible(false);
            return;
        }
        launcherButton.setManaged(true);
        launcherButton.setVisible(true);
        launcherButton.disableProperty().bind(launcher.enabledProperty().not());
        launcherButton.setOnAction(event -> launcher.execute());
        String tooltip = launcher.tooltip() == null || launcher.tooltip().isBlank()
            ? launcher.label()
            : launcher.tooltip();
        launcherButton.setTooltip(new Tooltip(tooltip));
    }
}
