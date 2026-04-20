package org.metalib.papifly.fx.docks.ribbon;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.PopupControl;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.metalib.papifly.fx.api.ribbon.PapiflyCommand;
import org.metalib.papifly.fx.api.ribbon.RibbonGroupSpec;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.ui.UiStyleSupport;

import java.util.Objects;

/**
 * Visual container for a ribbon group label, optional launcher, and controls.
 */
public class RibbonGroup extends VBox {

    private static final double GROUP_HORIZONTAL_PADDING = 20.0;

    private final RibbonGroupSpec spec;
    private final FlowPane controlsPane = new FlowPane(Orientation.HORIZONTAL, 8.0, 8.0);
    private final StackPane contentPane = new StackPane();
    private final javafx.scene.control.Label label = new javafx.scene.control.Label();
    private final Button launcherButton = new Button("...");
    private final Button collapsedButton = new Button();
    private final ObjectProperty<RibbonGroupSizeMode> sizeMode = new SimpleObjectProperty<>(RibbonGroupSizeMode.LARGE);
    private final ClassLoader classLoader;
    private final ObservableValue<Theme> theme;
    private final BorderPane footer = new BorderPane();

    private PopupControl collapsedPopup;
    private VBox collapsedPopupRoot;

    /**
     * Creates a ribbon group for the supplied spec.
     *
     * @param spec group specification
     * @param classLoader class loader used for icon resolution
     */
    public RibbonGroup(RibbonGroupSpec spec, ClassLoader classLoader) {
        this(spec, classLoader, null);
    }

    /**
     * Creates a ribbon group for the supplied spec.
     *
     * @param spec group specification
     * @param classLoader class loader used for icon resolution
     * @param theme ribbon theme used by collapsed popups
     */
    public RibbonGroup(RibbonGroupSpec spec, ClassLoader classLoader, ObservableValue<Theme> theme) {
        this.spec = Objects.requireNonNull(spec, "spec");
        this.classLoader = classLoader;
        this.theme = theme;
        getStyleClass().add("pf-ribbon-group");
        setId("pf-ribbon-group-" + spec.id());

        controlsPane.getStyleClass().add("pf-ribbon-group-controls");
        contentPane.getStyleClass().add("pf-ribbon-group-content");
        label.getStyleClass().add("pf-ribbon-group-label");
        label.setText(spec.label());

        launcherButton.getStyleClass().add("pf-ribbon-group-launcher");
        configureLauncher(spec.dialogLauncher());

        footer.getStyleClass().add("pf-ribbon-group-footer");
        footer.setCenter(label);
        footer.setRight(launcherButton);

        collapsedButton.setId("pf-ribbon-group-collapsed-" + spec.id());
        collapsedButton.setOnAction(event -> toggleCollapsedPopup());
        RibbonControlFactory.configureCollapsedGroupButton(collapsedButton, spec, classLoader);

        getChildren().addAll(contentPane, footer);

        sizeMode.addListener((obs, oldMode, newMode) -> applySizeMode(newMode));
        if (theme != null) {
            theme.addListener((obs, oldTheme, newTheme) -> updateCollapsedPopupTheme());
        }
        applySizeMode(sizeMode.get());
    }

    /**
     * Returns the backing group specification.
     *
     * @return backing group specification
     */
    public RibbonGroupSpec getSpec() {
        return spec;
    }

    /**
     * Returns the active presentation size mode for this group.
     *
     * @return active size mode
     */
    public RibbonGroupSizeMode getSizeMode() {
        return sizeMode.get();
    }

    /**
     * Updates the active presentation size mode for this group.
     *
     * @param mode active size mode
     */
    public void setSizeMode(RibbonGroupSizeMode mode) {
        sizeMode.set(mode == null ? RibbonGroupSizeMode.LARGE : mode);
    }

    /**
     * Returns the observable presentation mode property.
     *
     * @return presentation mode property
     */
    public ObjectProperty<RibbonGroupSizeMode> sizeModeProperty() {
        return sizeMode;
    }

    /**
     * Estimates the group width for the requested size mode.
     *
     * @param mode size mode to estimate
     * @return estimated width in pixels
     */
    public double estimateWidth(RibbonGroupSizeMode mode) {
        RibbonGroupSizeMode resolvedMode = mode == null ? RibbonGroupSizeMode.LARGE : mode;
        if (resolvedMode == RibbonGroupSizeMode.COLLAPSED) {
            return RibbonControlFactory.preferredControlWidth(RibbonGroupSizeMode.COLLAPSED) + GROUP_HORIZONTAL_PADDING;
        }
        int columns = columnsFor(resolvedMode);
        return (columns * RibbonControlFactory.preferredControlWidth(resolvedMode))
            + (Math.max(0, columns - 1) * RibbonControlFactory.controlGap())
            + GROUP_HORIZONTAL_PADDING;
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

    private void applySizeMode(RibbonGroupSizeMode mode) {
        RibbonGroupSizeMode resolvedMode = mode == null ? RibbonGroupSizeMode.LARGE : mode;
        getStyleClass().removeAll(
            "pf-ribbon-group-large",
            "pf-ribbon-group-medium",
            "pf-ribbon-group-small",
            "pf-ribbon-group-collapsed"
        );
        getStyleClass().add(switch (resolvedMode) {
            case LARGE -> "pf-ribbon-group-large";
            case MEDIUM -> "pf-ribbon-group-medium";
            case SMALL -> "pf-ribbon-group-small";
            case COLLAPSED -> "pf-ribbon-group-collapsed";
        });

        if (resolvedMode == RibbonGroupSizeMode.COLLAPSED) {
            footer.setManaged(false);
            footer.setVisible(false);
            contentPane.getChildren().setAll(collapsedButton);
            return;
        }

        hideCollapsedPopup();
        footer.setManaged(true);
        footer.setVisible(true);
        controlsPane.getChildren().setAll(spec.controls().stream()
            .filter(Objects::nonNull)
            .map(control -> RibbonControlFactory.createGroupControl(control, classLoader, resolvedMode))
            .toList());
        controlsPane.setPrefWrapLength(wrapLengthFor(resolvedMode));
        contentPane.getChildren().setAll(controlsPane);
    }

    private int columnsFor(RibbonGroupSizeMode mode) {
        return switch (mode) {
            case LARGE -> Math.min(spec.controls().size(), 3);
            case MEDIUM -> Math.min(spec.controls().size(), 2);
            case SMALL -> Math.min(spec.controls().size(), 3);
            case COLLAPSED -> 1;
        };
    }

    private double wrapLengthFor(RibbonGroupSizeMode mode) {
        int columns = columnsFor(mode);
        return (columns * RibbonControlFactory.preferredControlWidth(mode))
            + (Math.max(0, columns - 1) * RibbonControlFactory.controlGap());
    }

    private void toggleCollapsedPopup() {
        PopupControl popup = collapsedPopup();
        if (popup.isShowing()) {
            popup.hide();
            return;
        }
        updateCollapsedPopupTheme();
        Bounds screenBounds = collapsedButton.localToScreen(collapsedButton.getBoundsInLocal());
        if (screenBounds == null) {
            return;
        }
        popup.show(collapsedButton, screenBounds.getMinX(), screenBounds.getMaxY() + 6.0);
    }

    private void hideCollapsedPopup() {
        if (collapsedPopup != null) {
            collapsedPopup.hide();
        }
    }

    private PopupControl collapsedPopup() {
        if (collapsedPopup == null) {
            collapsedPopupRoot = new VBox(newPopupGroup());
            collapsedPopupRoot.getStyleClass().add("pf-ribbon-collapsed-popup");
            UiStyleSupport.ensureCommonStylesheetLoaded(collapsedPopupRoot);
            UiStyleSupport.ensureStylesheetLoaded(collapsedPopupRoot, Ribbon.STYLESHEET);
            updateCollapsedPopupTheme();

            collapsedPopup = new PopupControl();
            collapsedPopup.setAutoFix(true);
            collapsedPopup.setAutoHide(true);
            collapsedPopup.setHideOnEscape(true);
            collapsedPopup.getScene().setRoot(collapsedPopupRoot);
        }
        return collapsedPopup;
    }

    private RibbonGroup newPopupGroup() {
        RibbonGroup popupGroup = new RibbonGroup(spec, classLoader, theme);
        popupGroup.setSizeMode(spec.controls().size() <= 2 ? RibbonGroupSizeMode.LARGE : RibbonGroupSizeMode.MEDIUM);
        popupGroup.getStyleClass().add("pf-ribbon-popup-group");
        return popupGroup;
    }

    private void updateCollapsedPopupTheme() {
        if (collapsedPopupRoot == null) {
            return;
        }
        Theme resolvedTheme = theme == null || theme.getValue() == null ? Theme.dark() : theme.getValue();
        collapsedPopupRoot.setStyle(RibbonThemeSupport.themeVariables(resolvedTheme));
    }
}
