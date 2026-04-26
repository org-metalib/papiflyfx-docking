package org.metalib.papifly.fx.docks.ribbon;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.PopupControl;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.metalib.papifly.fx.api.ribbon.RibbonCommand;
import org.metalib.papifly.fx.api.ribbon.RibbonButtonSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonControlSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonGroupSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonMenuSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonSplitButtonSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonToggleSpec;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.ui.UiStyleSupport;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Visual container for a ribbon group label, optional launcher, and controls.
 */
public class RibbonGroup extends VBox {

    private static final double GROUP_HORIZONTAL_PADDING = 20.0;

    private RibbonGroupSpec spec;
    private final FlowPane controlsPane = new FlowPane(Orientation.HORIZONTAL, 8.0, 8.0);
    private final StackPane contentPane = new StackPane();
    private final javafx.scene.control.Label label = new javafx.scene.control.Label();
    private final Button launcherButton = new Button("...");
    private final Button collapsedButton = new Button();
    private final ObjectProperty<RibbonGroupSizeMode> sizeMode = new SimpleObjectProperty<>(RibbonGroupSizeMode.LARGE);
    private final ClassLoader classLoader;
    private final ObservableValue<Theme> theme;
    private final BorderPane footer = new BorderPane();
    private final String tabId;
    private final Map<ControlCacheKey, Node> controlNodeCache = new LinkedHashMap<>();
    private final Map<String, ControlSignature> controlSignatures = new LinkedHashMap<>();

    private RibbonLayoutTelemetry telemetry;
    private PopupControl collapsedPopup;
    private VBox collapsedPopupRoot;
    private Orientation orientation = Orientation.HORIZONTAL;

    /**
     * Creates a ribbon group for the supplied spec.
     *
     * @param spec group specification
     * @param classLoader class loader used for icon resolution
     */
    public RibbonGroup(RibbonGroupSpec spec, ClassLoader classLoader) {
        this(null, spec, classLoader, null, RibbonLayoutTelemetry.noop());
    }

    /**
     * Creates a ribbon group for the supplied spec.
     *
     * @param spec group specification
     * @param classLoader class loader used for icon resolution
     * @param theme ribbon theme used by collapsed popups
     */
    public RibbonGroup(RibbonGroupSpec spec, ClassLoader classLoader, ObservableValue<Theme> theme) {
        this(null, spec, classLoader, theme, RibbonLayoutTelemetry.noop());
    }

    RibbonGroup(
        String tabId,
        RibbonGroupSpec spec,
        ClassLoader classLoader,
        ObservableValue<Theme> theme,
        RibbonLayoutTelemetry telemetry
    ) {
        this.spec = Objects.requireNonNull(spec, "spec");
        this.classLoader = classLoader;
        this.theme = theme;
        this.tabId = tabId == null ? "" : tabId;
        this.telemetry = telemetry == null ? RibbonLayoutTelemetry.noop() : telemetry;
        getStyleClass().add("pf-ribbon-group");
        setId("pf-ribbon-group-" + spec.id());

        controlsPane.getStyleClass().add("pf-ribbon-group-controls");
        contentPane.getStyleClass().add("pf-ribbon-group-content");
        label.getStyleClass().add("pf-ribbon-group-label");
        launcherButton.getStyleClass().add("pf-ribbon-group-launcher");

        footer.getStyleClass().add("pf-ribbon-group-footer");
        footer.setCenter(label);
        footer.setRight(launcherButton);
        BorderPane.setAlignment(label, Pos.CENTER);
        BorderPane.setAlignment(launcherButton, Pos.CENTER_RIGHT);

        collapsedButton.setId("pf-ribbon-group-collapsed-" + spec.id());
        collapsedButton.setOnAction(event -> toggleCollapsedPopup());

        getChildren().addAll(contentPane, footer);

        sizeMode.addListener((obs, oldMode, newMode) -> applySizeMode(oldMode, newMode, RibbonLayoutTelemetry.RebuildReason.COLLAPSE, true));
        if (theme != null) {
            theme.addListener((obs, oldTheme, newTheme) -> updateCollapsedPopupTheme());
        }

        refreshPresentationMetadata();
        reindexControlSignatures(spec);
        this.telemetry.groupRebuild(spec.id(), RibbonLayoutTelemetry.RebuildReason.INITIAL);
        applySizeMode(null, sizeMode.get(), RibbonLayoutTelemetry.RebuildReason.INITIAL, false);
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
        RibbonGroupSizeMode resolvedMode = mode == null ? RibbonGroupSizeMode.LARGE : mode;
        if (resolvedMode != sizeMode.get()) {
            sizeMode.set(resolvedMode);
        }
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
        return estimateExtent(mode, Orientation.HORIZONTAL);
    }

    /**
     * Estimates the primary-axis extent for the requested size mode.
     *
     * @param mode size mode to estimate
     * @param orientation ribbon orientation
     * @return estimated extent in pixels
     */
    public double estimateExtent(RibbonGroupSizeMode mode, Orientation orientation) {
        RibbonGroupSizeMode resolvedMode = mode == null ? RibbonGroupSizeMode.LARGE : mode;
        Orientation resolvedOrientation = orientation == null ? Orientation.HORIZONTAL : orientation;
        if (resolvedOrientation == Orientation.VERTICAL) {
            return estimateHeight(resolvedMode);
        }
        if (resolvedMode == RibbonGroupSizeMode.COLLAPSED) {
            return RibbonControlFactory.preferredControlWidth(RibbonGroupSizeMode.COLLAPSED) + GROUP_HORIZONTAL_PADDING;
        }
        int columns = columnsFor(resolvedMode);
        return (columns * RibbonControlFactory.preferredControlWidth(resolvedMode))
            + (Math.max(0, columns - 1) * RibbonControlFactory.controlGap())
            + GROUP_HORIZONTAL_PADDING;
    }

    void setRibbonOrientation(Orientation orientation) {
        Orientation resolvedOrientation = orientation == null ? Orientation.HORIZONTAL : orientation;
        if (this.orientation == resolvedOrientation) {
            return;
        }
        this.orientation = resolvedOrientation;
        getStyleClass().removeAll("pf-ribbon-group-horizontal", "pf-ribbon-group-vertical");
        getStyleClass().add(resolvedOrientation == Orientation.VERTICAL
            ? "pf-ribbon-group-vertical"
            : "pf-ribbon-group-horizontal");
        controlsPane.setOrientation(Orientation.HORIZONTAL);
        if (resolvedOrientation == Orientation.VERTICAL) {
            getChildren().setAll(footer, contentPane);
        } else {
            getChildren().setAll(contentPane, footer);
        }
        if (getSizeMode() != RibbonGroupSizeMode.COLLAPSED) {
            renderControlsForMode(getSizeMode(), RibbonLayoutTelemetry.RebuildReason.STRUCTURAL);
        }
    }

    void setLayoutTelemetry(RibbonLayoutTelemetry telemetry) {
        this.telemetry = telemetry == null ? RibbonLayoutTelemetry.noop() : telemetry;
    }

    boolean updateSpec(RibbonGroupSpec newSpec) {
        Objects.requireNonNull(newSpec, "newSpec");
        boolean structuralChange = !structurallyEquivalent(spec, newSpec);
        spec = newSpec;
        refreshPresentationMetadata();
        if (!structuralChange) {
            observeCurrentModeReuse();
            return false;
        }

        telemetry.groupRebuild(newSpec.id(), RibbonLayoutTelemetry.RebuildReason.STRUCTURAL);
        invalidateChangedControls(newSpec);
        invalidateCollapsedPopup();
        if (getSizeMode() != RibbonGroupSizeMode.COLLAPSED) {
            renderControlsForMode(getSizeMode(), RibbonLayoutTelemetry.RebuildReason.STRUCTURAL);
        }
        return true;
    }

    private void configureLauncher(RibbonCommand launcher) {
        JavaFxCommandBindings.disposeSubscriptions(launcherButton);
        launcherButton.disableProperty().unbind();
        if (launcher == null) {
            launcherButton.setManaged(false);
            launcherButton.setVisible(false);
            launcherButton.setTooltip(null);
            launcherButton.setOnAction(null);
            launcherButton.setAccessibleText(null);
            return;
        }
        launcherButton.setManaged(true);
        launcherButton.setVisible(true);
        JavaFxCommandBindings.bindDisabledToNot(launcherButton, launcher.enabled());
        launcherButton.setOnAction(event -> launcher.execute());
        launcherButton.setAccessibleText(launcher.label());
        String tooltip = launcher.tooltip() == null || launcher.tooltip().isBlank()
            ? launcher.label()
            : launcher.tooltip();
        launcherButton.setTooltip(new Tooltip(tooltip));
    }

    private void applySizeMode(
        RibbonGroupSizeMode previousMode,
        RibbonGroupSizeMode mode,
        RibbonLayoutTelemetry.RebuildReason reason,
        boolean emitTransition
    ) {
        RibbonGroupSizeMode resolvedMode = mode == null ? RibbonGroupSizeMode.LARGE : mode;
        if (emitTransition && previousMode != null && previousMode != resolvedMode) {
            telemetry.collapseTransition(spec.id(), previousMode, resolvedMode);
        }

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
            if (!contentPane.getChildren().equals(List.of(collapsedButton))) {
                contentPane.getChildren().setAll(collapsedButton);
            }
            return;
        }

        hideCollapsedPopup();
        footer.setManaged(true);
        footer.setVisible(true);
        renderControlsForMode(resolvedMode, reason);
    }

    private int columnsFor(RibbonGroupSizeMode mode) {
        if (orientation == Orientation.VERTICAL && mode == RibbonGroupSizeMode.LARGE) {
            return 1;
        }
        return switch (mode) {
            case LARGE -> Math.min(spec.controls().size(), 3);
            case MEDIUM -> Math.min(spec.controls().size(), 2);
            case SMALL -> Math.min(spec.controls().size(), 3);
            case COLLAPSED -> 1;
        };
    }

    private double estimateHeight(RibbonGroupSizeMode mode) {
        if (mode == RibbonGroupSizeMode.COLLAPSED) {
            return 72.0;
        }
        int columns = Math.max(1, columnsFor(mode));
        int rows = (int) Math.ceil(spec.controls().size() / (double) columns);
        double controlHeight = switch (mode) {
            case LARGE -> 88.0;
            case MEDIUM -> 68.0;
            case SMALL -> 36.0;
            case COLLAPSED -> 56.0;
        };
        return (rows * controlHeight)
            + (Math.max(0, rows - 1) * RibbonControlFactory.controlGap())
            + 58.0;
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
            collapsedPopup.setOnHidden(event -> {
                if (collapsedButton.getScene() != null && collapsedButton.isVisible()) {
                    collapsedButton.requestFocus();
                }
            });
        }
        return collapsedPopup;
    }

    private RibbonGroup newPopupGroup() {
        RibbonGroup popupGroup = new RibbonGroup(tabId, spec, classLoader, theme, RibbonLayoutTelemetry.noop());
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

    private void refreshPresentationMetadata() {
        label.setText(spec.label());
        configureLauncher(spec.dialogLauncher());
        RibbonControlFactory.configureCollapsedGroupButton(collapsedButton, spec, classLoader);
    }

    private void renderControlsForMode(RibbonGroupSizeMode mode, RibbonLayoutTelemetry.RebuildReason reason) {
        List<Node> nodes = new ArrayList<>(spec.controls().size());
        for (RibbonControlSpec control : spec.controls()) {
            if (control == null) {
                continue;
            }
            nodes.add(resolveControlNode(control, mode, reason));
        }
        controlsPane.setPrefWrapLength(wrapLengthFor(mode));
        if (!controlsPane.getChildren().equals(nodes)) {
            controlsPane.getChildren().setAll(nodes);
        }
        if (!contentPane.getChildren().equals(List.of(controlsPane))) {
            contentPane.getChildren().setAll(controlsPane);
        }
    }

    private Node resolveControlNode(
        RibbonControlSpec control,
        RibbonGroupSizeMode mode,
        RibbonLayoutTelemetry.RebuildReason reason
    ) {
        ControlCacheKey key = new ControlCacheKey(control.id(), mode);
        ControlSignature signature = ControlSignature.from(control);
        Node cached = controlNodeCache.get(key);
        if (cached != null && signature.equals(controlSignatures.get(control.id()))) {
            telemetry.nodeCacheHit(RibbonLayoutTelemetry.CacheKind.CONTROL, controlCacheId(key));
            return cached;
        }
        telemetry.nodeCacheMiss(RibbonLayoutTelemetry.CacheKind.CONTROL, controlCacheId(key));
        Node rebuilt = RibbonControlFactory.createGroupControl(control, classLoader, mode, theme);
        controlNodeCache.put(key, rebuilt);
        controlSignatures.put(control.id(), signature);
        telemetry.controlRebuild(control.id(), reason);
        return rebuilt;
    }

    private void observeCurrentModeReuse() {
        RibbonGroupSizeMode currentMode = getSizeMode();
        if (currentMode == RibbonGroupSizeMode.COLLAPSED) {
            return;
        }
        for (RibbonControlSpec control : spec.controls()) {
            if (control == null) {
                continue;
            }
            ControlCacheKey key = new ControlCacheKey(control.id(), currentMode);
            if (controlNodeCache.containsKey(key)) {
                telemetry.nodeCacheHit(RibbonLayoutTelemetry.CacheKind.CONTROL, controlCacheId(key));
            }
        }
    }

    private void invalidateChangedControls(RibbonGroupSpec newSpec) {
        Map<String, ControlSignature> previous = new LinkedHashMap<>(controlSignatures);
        Map<String, ControlSignature> next = reindexControlSignatures(newSpec);
        previous.entrySet().stream()
            .filter(entry -> !Objects.equals(next.get(entry.getKey()), entry.getValue()))
            .map(Map.Entry::getKey)
            .forEach(this::removeControlFromCache);
        controlSignatures.clear();
        controlSignatures.putAll(next);
    }

    private Map<String, ControlSignature> reindexControlSignatures(RibbonGroupSpec groupSpec) {
        Map<String, ControlSignature> signatures = new LinkedHashMap<>();
        for (RibbonControlSpec control : groupSpec.controls()) {
            if (control == null) {
                continue;
            }
            signatures.put(control.id(), ControlSignature.from(control));
        }
        if (controlSignatures.isEmpty()) {
            controlSignatures.putAll(signatures);
        }
        return signatures;
    }

    private void removeControlFromCache(String controlId) {
        List<ControlCacheKey> keysToRemove = controlNodeCache.keySet().stream()
            .filter(key -> Objects.equals(key.controlId(), controlId))
            .toList();
        for (ControlCacheKey key : keysToRemove) {
            RibbonControlFactory.dispose(controlNodeCache.remove(key));
        }
    }

    private void invalidateCollapsedPopup() {
        hideCollapsedPopup();
        RibbonControlFactory.dispose(collapsedPopupRoot);
        collapsedPopup = null;
        collapsedPopupRoot = null;
    }

    private String controlCacheId(ControlCacheKey key) {
        return tabId + "/" + spec.id() + "/" + key.controlId() + "/" + key.mode();
    }

    private static boolean structurallyEquivalent(RibbonGroupSpec left, RibbonGroupSpec right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        if (!Objects.equals(left.id(), right.id())
            || !Objects.equals(left.label(), right.label())
            || left.order() != right.order()
            || left.collapseOrder() != right.collapseOrder()
            || !Objects.equals(commandSignature(left.dialogLauncher()), commandSignature(right.dialogLauncher()))
            || left.controls().size() != right.controls().size()) {
            return false;
        }
        for (int index = 0; index < left.controls().size(); index++) {
            if (!Objects.equals(ControlSignature.from(left.controls().get(index)), ControlSignature.from(right.controls().get(index)))) {
                return false;
            }
        }
        return true;
    }

    private static CommandSignature commandSignature(RibbonCommand command) {
        if (command == null) {
            return null;
        }
        return new CommandSignature(
            command.id(),
            command.label(),
            command.tooltip(),
            command.smallIcon() == null ? null : command.smallIcon().resourcePath(),
            command.largeIcon() == null ? null : command.largeIcon().resourcePath()
        );
    }

    private record ControlCacheKey(String controlId, RibbonGroupSizeMode mode) {
    }

    private record ControlSignature(
        String controlId,
        String type,
        CommandSignature primaryCommand,
        List<String> itemCommandIds
    ) {
        static ControlSignature from(RibbonControlSpec control) {
            if (control == null) {
                return null;
            }
            return RibbonControlStrategies.renderPlan(control)
                .map(plan -> new ControlSignature(
                    plan.id(),
                    plan.kind().name(),
                    commandSignature(plan.primaryCommand()),
                    plan.itemCommands().stream().map(RibbonCommand::id).toList()
                ))
                .orElse(null);
        }
    }

    private record CommandSignature(
        String id,
        String label,
        String tooltip,
        String smallIconPath,
        String largeIconPath
    ) {
    }
}
