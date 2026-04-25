package org.metalib.papifly.fx.docks.ribbon;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ListChangeListener;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Bounds;
import javafx.scene.control.Button;
import javafx.scene.control.PopupControl;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import org.metalib.papifly.fx.api.ribbon.RibbonCommand;
import org.metalib.papifly.fx.api.ribbon.RibbonGroupSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonTabSpec;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.docks.layout.data.RibbonSessionData;
import org.metalib.papifly.fx.ui.UiStyleSupport;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Top-level ribbon shell containing the Quick Access Toolbar, tab strip, and
 * active tab groups.
 */
public class Ribbon extends VBox {

    /**
     * Ribbon stylesheet resource path.
     */
    public static final String STYLESHEET = "/org/metalib/papifly/fx/docks/ribbon/ribbon.css";

    private final ObjectProperty<Theme> theme = new SimpleObjectProperty<>(Theme.dark());
    private final BooleanProperty minimized = new SimpleBooleanProperty(false);
    private final StringProperty selectedTabId = new SimpleStringProperty();
    private final ObjectProperty<RibbonPlacement> placement = new SimpleObjectProperty<>(this, "placement", RibbonPlacement.TOP);

    private final QuickAccessToolbar quickAccessToolbar = new QuickAccessToolbar();
    private final RibbonTabStrip tabStrip = new RibbonTabStrip();
    private final FlowPane groupRow = new FlowPane(Orientation.HORIZONTAL, 12.0, 10.0);
    private final ScrollPane groupScroller = new ScrollPane(groupRow);
    private final Button minimizeButton = new Button();
    private final BorderPane header = new BorderPane();
    private final HBox headerActions = new HBox();
    private final HBox sideChrome = new HBox();
    private final VBox sideEdgeStrip = new VBox(8.0);
    private final StackPane sideContentPane = new StackPane();
    private final StackPane sideFlyoutPane = new StackPane();
    private final Region sideEdgeSpacer = new Region();
    private final Comparator<RibbonGroup> collapseOrderComparator =
        Comparator.comparingInt((RibbonGroup group) -> group.getSpec().collapseOrder())
            .thenComparing((RibbonGroup group) -> group.getSpec().order(), Comparator.reverseOrder())
            .thenComparing(group -> group.getSpec().id());
    private final Comparator<RibbonGroup> restoreOrderComparator =
        Comparator.comparingInt((RibbonGroup group) -> group.getSpec().collapseOrder()).reversed()
            .thenComparingInt(group -> group.getSpec().order())
            .thenComparing(group -> group.getSpec().id());
    private final Map<String, RibbonGroup> groupCache = new LinkedHashMap<>();

    private final ListChangeListener<RibbonTabSpec> tabsListener = change -> refreshTabs();
    private final ListChangeListener<RibbonCommand> quickAccessListener = change -> refreshQuickAccessToolbar();

    private RibbonManager manager;
    private boolean adaptiveLayoutRequested;
    private boolean minimizedTabPanelShowing;
    private PopupControl sideFlyout;
    private RibbonLayoutTelemetry layoutTelemetry = RibbonLayoutTelemetry.noop();

    /**
     * Creates a ribbon backed by a default {@link RibbonManager}.
     */
    public Ribbon() {
        this(new RibbonManager());
    }

    /**
     * Creates a ribbon backed by the supplied manager.
     *
     * @param manager ribbon manager used for provider discovery and context
     */
    public Ribbon(RibbonManager manager) {
        getStyleClass().add("pf-ribbon");
        UiStyleSupport.ensureCommonStylesheetLoaded(this);
        UiStyleSupport.ensureStylesheetLoaded(this, STYLESHEET);

        groupRow.getStyleClass().add("pf-ribbon-groups");
        groupRow.setAlignment(Pos.CENTER_LEFT);

        groupScroller.getStyleClass().add("pf-ribbon-scroll");
        groupScroller.setContent(groupRow);
        groupScroller.setFitToHeight(true);
        groupScroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        groupScroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        groupScroller.setPannable(true);
        groupScroller.viewportBoundsProperty().addListener((obs, oldBounds, newBounds) -> requestAdaptiveLayout());

        tabStrip.selectedTabIdProperty().bindBidirectional(selectedTabId);
        tabStrip.setOnTabActivated(this::showMinimizedTabPanel);

        minimizeButton.getStyleClass().addAll("pf-ui-compact-action-button", "pf-ribbon-collapse-button");
        minimizeButton.setOnAction(event -> toggleMinimized());

        header.getStyleClass().add("pf-ribbon-header");
        headerActions.getStyleClass().add("pf-ribbon-header-actions");
        headerActions.setAlignment(Pos.CENTER_RIGHT);
        headerActions.getChildren().add(minimizeButton);
        header.setLeft(quickAccessToolbar);
        header.setCenter(tabStrip);
        header.setRight(headerActions);
        BorderPane.setAlignment(headerActions, Pos.CENTER_RIGHT);
        HBox.setHgrow(tabStrip, Priority.ALWAYS);

        sideChrome.getStyleClass().add("pf-ribbon-side-chrome");
        sideEdgeStrip.getStyleClass().add("pf-ribbon-side-edge-strip");
        sideEdgeStrip.setAlignment(Pos.TOP_CENTER);
        sideContentPane.getStyleClass().add("pf-ribbon-side-content-pane");
        sideFlyoutPane.getStyleClass().addAll("pf-ribbon-side-content-pane", "pf-ribbon-side-flyout");
        VBox.setVgrow(tabStrip, Priority.ALWAYS);
        VBox.setVgrow(sideEdgeSpacer, Priority.ALWAYS);

        getChildren().addAll(header, groupScroller);

        theme.addListener((obs, oldTheme, newTheme) -> applyTheme(newTheme));
        minimized.addListener((obs, oldValue, newValue) -> updateMinimizedState());
        placement.addListener((obs, oldPlacement, newPlacement) -> configurePlacement(newPlacement));
        selectedTabId.addListener((obs, oldValue, newValue) -> {
            if (rebuildGroups()) {
                requestAdaptiveLayout();
            }
        });
        widthProperty().addListener((obs, oldWidth, newWidth) -> requestAdaptiveLayout());
        heightProperty().addListener((obs, oldHeight, newHeight) -> requestAdaptiveLayout());

        setManager(manager);
        applyTheme(theme.get());
        configurePlacement(placement.get());
        updateMinimizedState();
    }

    /**
     * Returns the ribbon theme property.
     *
     * @return ribbon theme property
     */
    public ObjectProperty<Theme> themeProperty() {
        return theme;
    }

    /**
     * Returns the ribbon placement property.
     *
     * @return ribbon placement property
     */
    public ObjectProperty<RibbonPlacement> placementProperty() {
        return placement;
    }

    /**
     * Returns the side where this ribbon renders its chrome.
     *
     * @return current placement, defaulting to {@link RibbonPlacement#TOP}
     */
    public RibbonPlacement getPlacement() {
        return RibbonPlacement.normalize(placement.get());
    }

    /**
     * Updates the side where this ribbon renders its chrome.
     *
     * @param placement requested placement, or {@code null} for {@link RibbonPlacement#TOP}
     */
    public void setPlacement(RibbonPlacement placement) {
        this.placement.set(RibbonPlacement.normalize(placement));
    }

    /**
     * Returns whether the ribbon is minimized.
     *
     * @return {@code true} when only the header row is shown
     */
    public boolean isMinimized() {
        return minimized.get();
    }

    /**
     * Updates the ribbon minimized state.
     *
     * @param minimized minimized flag
     */
    public void setMinimized(boolean minimized) {
        if (this.minimized.get() != minimized) {
            minimizedTabPanelShowing = false;
        }
        this.minimized.set(minimized);
    }

    /**
     * Returns the minimized state property.
     *
     * @return minimized state property
     */
    public BooleanProperty minimizedProperty() {
        return minimized;
    }

    /**
     * Returns the currently selected tab identifier.
     *
     * @return selected tab identifier
     */
    public String getSelectedTabId() {
        return selectedTabId.get();
    }

    /**
     * Updates the selected tab identifier.
     *
     * @param selectedTabId selected tab identifier
     */
    public void setSelectedTabId(String selectedTabId) {
        this.selectedTabId.set(selectedTabId);
    }

    /**
     * Returns the selected tab identifier property.
     *
     * @return selected tab identifier property
     */
    public StringProperty selectedTabIdProperty() {
        return selectedTabId;
    }

    /**
     * Returns the current ribbon manager.
     *
     * @return current ribbon manager
     */
    public RibbonManager getManager() {
        return manager;
    }

    /**
     * Replaces the ribbon manager backing this shell.
     *
     * @param manager ribbon manager
     */
    public final void setManager(RibbonManager manager) {
        RibbonManager resolvedManager = Objects.requireNonNull(manager, "manager");
        if (this.manager == resolvedManager) {
            return;
        }
        if (this.manager != null) {
            this.manager.getTabs().removeListener(tabsListener);
            this.manager.getQuickAccessCommands().removeListener(quickAccessListener);
        }
        this.manager = resolvedManager;
        this.manager.getTabs().addListener(tabsListener);
        this.manager.getQuickAccessCommands().addListener(quickAccessListener);
        this.manager.setLayoutTelemetry(layoutTelemetry);
        quickAccessToolbar.setClassLoader(this.manager.getClassLoader());
        tabStrip.setLayoutTelemetry(layoutTelemetry);
        refreshQuickAccessToolbar();
        refreshTabs();
    }

    /**
     * Captures ribbon-only session state.
     *
     * <p>The Quick Access Toolbar snapshot is taken from the runtime identifier
     * list ({@link RibbonManager#getQuickAccessCommandIds()}), not the
     * derived command view, so identifiers tied to hidden contextual commands
     * still round-trip through persistence.</p>
     *
     * @return ribbon session payload
     */
    public RibbonSessionData captureSessionState() {
        List<String> quickAccessCommandIds = manager == null
            ? List.of()
            : manager.getQuickAccessCommandIds().stream()
                .filter(Objects::nonNull)
                .filter(id -> !id.isBlank())
                .distinct()
                .toList();
        return new RibbonSessionData(isMinimized(), getSelectedTabId(), quickAccessCommandIds, getPlacement());
    }

    /**
     * Restores ribbon-only session state. Missing tabs or commands are ignored
     * so persisted state remains tolerant of unavailable providers.
     *
     * <p>Identifiers that cannot currently be resolved remain pinned on the
     * Quick Access Toolbar list and reappear automatically once the owning
     * contextual tab becomes visible.</p>
     *
     * @param state ribbon session payload
     */
    public void restoreSessionState(RibbonSessionData state) {
        if (state == null || manager == null) {
            return;
        }
        manager.getQuickAccessCommandIds().setAll(state.quickAccessCommandIds());
        if (state.selectedTabId() != null && manager.hasTab(state.selectedTabId())) {
            setSelectedTabId(state.selectedTabId());
        } else if (!manager.getTabs().isEmpty()) {
            setSelectedTabId(manager.getTabs().getFirst().id());
        } else {
            setSelectedTabId(null);
        }
        setPlacement(state.placement());
        setMinimized(state.minimized());
    }

    private void refreshQuickAccessToolbar() {
        quickAccessToolbar.getCommands().setAll(manager.getQuickAccessCommands());
    }

    private void refreshTabs() {
        tabStrip.getTabs().setAll(manager.getTabs());
        ensureSelectedTab();
        if (rebuildGroups()) {
            requestAdaptiveLayout();
        }
    }

    private void ensureSelectedTab() {
        if (manager.getTabs().isEmpty()) {
            if (selectedTabId.get() != null) {
                selectedTabId.set(null);
            }
            return;
        }
        boolean selectedTabStillVisible = manager.getTabs().stream()
            .anyMatch(tab -> Objects.equals(tab.id(), selectedTabId.get()));
        if (!selectedTabStillVisible) {
            selectedTabId.set(manager.getTabs().getFirst().id());
        }
    }

    private boolean rebuildGroups() {
        if (manager == null || manager.getTabs().isEmpty()) {
            if (!groupRow.getChildren().isEmpty()) {
                groupRow.getChildren().clear();
                return true;
            }
            return false;
        }
        RibbonTabSpec selectedTab = manager.getTabs().stream()
            .filter(tab -> Objects.equals(tab.id(), selectedTabId.get()))
            .findFirst()
            .orElse(manager.getTabs().getFirst());
        boolean structureChanged = false;
        List<RibbonGroup> desiredGroups = new java.util.ArrayList<>(selectedTab.groups().size());
        for (RibbonGroupSpec group : selectedTab.groups()) {
            GroupResolution resolution = resolveGroup(selectedTab.id(), group);
            desiredGroups.add(resolution.group());
            structureChanged = structureChanged || resolution.changed();
        }
        if (!groupRow.getChildren().equals(desiredGroups)) {
            groupRow.getChildren().setAll(desiredGroups);
            return true;
        }
        return structureChanged;
    }

    private void updateMinimizedState() {
        boolean vertical = isVerticalPlacement();
        boolean bodyVisible = !isMinimized() || minimizedTabPanelShowing;
        if (vertical && isMinimized()) {
            bodyVisible = false;
        }
        groupScroller.setManaged(bodyVisible);
        groupScroller.setVisible(bodyVisible);
        sideContentPane.setManaged(!vertical || bodyVisible);
        sideContentPane.setVisible(!vertical || bodyVisible);
        String actionLabel = isMinimized() ? "Expand ribbon" : "Collapse ribbon";
        minimizeButton.setText("");
        minimizeButton.setGraphic(createMinimizeGraphic(isMinimized()));
        minimizeButton.setAccessibleText(actionLabel);
        minimizeButton.setTooltip(new Tooltip(actionLabel));
        if (!isMinimized()) {
            hideSideFlyout();
            requestAdaptiveLayout();
        }
    }

    private void toggleMinimized() {
        minimizedTabPanelShowing = false;
        hideSideFlyout();
        setMinimized(!isMinimized());
    }

    private void showMinimizedTabPanel(String tabId) {
        if (!isMinimized()) {
            return;
        }
        minimizedTabPanelShowing = true;
        if (rebuildGroups()) {
            requestAdaptiveLayout();
        }
        updateMinimizedState();
        if (isVerticalPlacement()) {
            showSideFlyout();
        } else {
            requestAdaptiveLayout();
        }
    }

    private StackPane createMinimizeGraphic(boolean minimized) {
        SVGPath icon = new SVGPath();
        if (isVerticalPlacement()) {
            icon.setContent(minimized ? "M6,3 L11,8 L6,13" : "M10,3 L5,8 L10,13");
        } else {
            icon.setContent(minimized ? "M3,6 L8,11 L13,6" : "M3,10 L8,5 L13,10");
        }
        icon.getStyleClass().add("pf-ribbon-collapse-icon");
        StackPane wrapper = new StackPane(icon);
        wrapper.getStyleClass().add("pf-ribbon-collapse-icon-wrap");
        return wrapper;
    }

    private void applyTheme(Theme theme) {
        setStyle(RibbonThemeSupport.themeVariables(theme));
        if (sideFlyoutPane != null) {
            sideFlyoutPane.setStyle(RibbonThemeSupport.themeVariables(theme));
        }
    }

    List<RibbonGroup> getRenderedGroups() {
        return groupRow.getChildren().stream()
            .filter(RibbonGroup.class::isInstance)
            .map(RibbonGroup.class::cast)
            .toList();
    }

    void setLayoutTelemetry(RibbonLayoutTelemetry telemetry) {
        layoutTelemetry = telemetry == null ? RibbonLayoutTelemetry.noop() : telemetry;
        if (manager != null) {
            manager.setLayoutTelemetry(layoutTelemetry);
        }
        tabStrip.setLayoutTelemetry(layoutTelemetry);
        groupCache.values().forEach(group -> group.setLayoutTelemetry(layoutTelemetry));
    }

    private void requestAdaptiveLayout() {
        if (adaptiveLayoutRequested || (isMinimized() && !minimizedTabPanelShowing)) {
            return;
        }
        adaptiveLayoutRequested = true;
        Platform.runLater(() -> {
            adaptiveLayoutRequested = false;
            applyAdaptiveLayout();
        });
    }

    private void applyAdaptiveLayout() {
        List<RibbonGroup> groups = getRenderedGroups();
        if (groups.isEmpty()) {
            return;
        }

        Orientation orientation = orientation();
        double availableExtent = orientation == Orientation.VERTICAL
            ? groupScroller.getViewportBounds().getHeight()
            : groupScroller.getViewportBounds().getWidth();
        if (availableExtent <= 0.0) {
            availableExtent = orientation == Orientation.VERTICAL ? groupScroller.getHeight() : groupScroller.getWidth();
        }
        if (availableExtent <= 0.0) {
            return;
        }

        double totalExtent = estimatedTotalExtent(groups, orientation);
        while (totalExtent > availableExtent) {
            RibbonGroup next = nextGroupToShrink(groups);
            if (next == null) {
                break;
            }
            next.setSizeMode(next.getSizeMode().smaller());
            totalExtent = estimatedTotalExtent(groups, orientation);
        }

        while (totalExtent < availableExtent) {
            RibbonGroup next = nextGroupToGrow(groups);
            if (next == null) {
                break;
            }
            RibbonGroupSizeMode expandedMode = next.getSizeMode().larger();
            double candidateExtent = totalExtent
                - next.estimateExtent(next.getSizeMode(), orientation)
                + next.estimateExtent(expandedMode, orientation);
            if (candidateExtent > availableExtent) {
                break;
            }
            next.setSizeMode(expandedMode);
            totalExtent = candidateExtent;
        }
    }

    private double estimatedTotalExtent(List<RibbonGroup> groups, Orientation orientation) {
        double extents = groups.stream()
            .mapToDouble(group -> group.estimateExtent(group.getSizeMode(), orientation))
            .sum();
        double gap = orientation == Orientation.VERTICAL ? groupRow.getVgap() : groupRow.getHgap();
        double spacing = Math.max(0, groups.size() - 1) * gap;
        return extents + spacing;
    }

    private GroupResolution resolveGroup(String tabId, RibbonGroupSpec groupSpec) {
        String key = tabId + "/" + groupSpec.id();
        RibbonGroup cached = groupCache.get(key);
        if (cached == null) {
            layoutTelemetry.nodeCacheMiss(RibbonLayoutTelemetry.CacheKind.GROUP, key);
            RibbonGroup created = new RibbonGroup(tabId, groupSpec, manager.getClassLoader(), theme, layoutTelemetry);
            created.setRibbonOrientation(orientation());
            groupCache.put(key, created);
            return new GroupResolution(created, true);
        }
        layoutTelemetry.nodeCacheHit(RibbonLayoutTelemetry.CacheKind.GROUP, key);
        cached.setLayoutTelemetry(layoutTelemetry);
        cached.setRibbonOrientation(orientation());
        return new GroupResolution(cached, cached.updateSpec(groupSpec));
    }

    private void configurePlacement(RibbonPlacement requestedPlacement) {
        RibbonPlacement resolvedPlacement = RibbonPlacement.normalize(requestedPlacement);
        if (placement.get() != resolvedPlacement) {
            placement.set(resolvedPlacement);
            return;
        }

        getStyleClass().removeAll(
            "pf-ribbon-placement-top",
            "pf-ribbon-placement-bottom",
            "pf-ribbon-placement-left",
            "pf-ribbon-placement-right",
            "pf-ribbon-orientation-horizontal",
            "pf-ribbon-orientation-vertical"
        );
        getStyleClass().add("pf-ribbon-placement-" + resolvedPlacement.name().toLowerCase(java.util.Locale.ROOT));
        getStyleClass().add(isVerticalPlacement() ? "pf-ribbon-orientation-vertical" : "pf-ribbon-orientation-horizontal");

        minimizedTabPanelShowing = false;
        hideSideFlyout();
        if (isVerticalPlacement()) {
            configureVerticalPlacement(resolvedPlacement);
        } else {
            configureHorizontalPlacement();
        }
        groupCache.values().forEach(group -> group.setRibbonOrientation(orientation()));
        rebuildGroups();
        updateMinimizedState();
        requestAdaptiveLayout();
    }

    private void configureHorizontalPlacement() {
        hideSideFlyout();
        headerActions.getChildren().setAll(minimizeButton);
        header.setLeft(quickAccessToolbar);
        header.setCenter(tabStrip);
        header.setRight(headerActions);
        sideContentPane.getChildren().clear();
        sideFlyoutPane.getChildren().clear();
        tabStrip.setRibbonOrientation(Orientation.HORIZONTAL);
        groupRow.setOrientation(Orientation.HORIZONTAL);
        groupRow.setHgap(12.0);
        groupRow.setVgap(10.0);
        groupRow.setAlignment(Pos.CENTER_LEFT);
        groupScroller.setFitToHeight(true);
        groupScroller.setFitToWidth(false);
        groupScroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        groupScroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        getChildren().setAll(header, groupScroller);
    }

    private void configureVerticalPlacement(RibbonPlacement resolvedPlacement) {
        header.setLeft(null);
        header.setCenter(null);
        header.setRight(null);
        tabStrip.setRibbonOrientation(Orientation.VERTICAL);
        groupRow.setOrientation(Orientation.VERTICAL);
        groupRow.setHgap(10.0);
        groupRow.setVgap(10.0);
        groupRow.setAlignment(Pos.TOP_LEFT);
        groupScroller.setFitToHeight(false);
        groupScroller.setFitToWidth(true);
        groupScroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        groupScroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sideFlyoutPane.getChildren().clear();
        if (!sideContentPane.getChildren().equals(List.of(groupScroller))) {
            sideContentPane.getChildren().setAll(groupScroller);
        }
        sideEdgeStrip.getChildren().setAll(quickAccessToolbar, tabStrip, sideEdgeSpacer, minimizeButton);
        if (resolvedPlacement == RibbonPlacement.LEFT) {
            sideChrome.getChildren().setAll(sideEdgeStrip, sideContentPane);
        } else {
            sideChrome.getChildren().setAll(sideContentPane, sideEdgeStrip);
        }
        getChildren().setAll(sideChrome);
    }

    private Orientation orientation() {
        return isVerticalPlacement() ? Orientation.VERTICAL : Orientation.HORIZONTAL;
    }

    private boolean isVerticalPlacement() {
        RibbonPlacement resolvedPlacement = RibbonPlacement.normalize(placement.get());
        return resolvedPlacement == RibbonPlacement.LEFT || resolvedPlacement == RibbonPlacement.RIGHT;
    }

    private void showSideFlyout() {
        if (!isVerticalPlacement() || !isMinimized()) {
            return;
        }
        PopupControl flyout = sideFlyout();
        updateSideFlyoutTheme();
        sideContentPane.getChildren().clear();
        if (!sideFlyoutPane.getChildren().equals(List.of(groupScroller))) {
            sideFlyoutPane.getChildren().setAll(groupScroller);
        }
        groupScroller.setManaged(true);
        groupScroller.setVisible(true);
        requestAdaptiveLayout();
        Bounds screenBounds = localToScreen(getBoundsInLocal());
        if (screenBounds == null) {
            return;
        }
        double flyoutWidth = 284.0;
        double x = getPlacement() == RibbonPlacement.RIGHT
            ? screenBounds.getMinX() - flyoutWidth
            : screenBounds.getMaxX();
        flyout.setPrefWidth(flyoutWidth);
        flyout.show(this, x, screenBounds.getMinY());
        sideFlyoutPane.requestFocus();
    }

    private PopupControl sideFlyout() {
        if (sideFlyout == null) {
            UiStyleSupport.ensureCommonStylesheetLoaded(sideFlyoutPane);
            UiStyleSupport.ensureStylesheetLoaded(sideFlyoutPane, STYLESHEET);
            updateSideFlyoutTheme();
            sideFlyout = new PopupControl();
            sideFlyout.setAutoFix(true);
            sideFlyout.setAutoHide(true);
            sideFlyout.setHideOnEscape(true);
            sideFlyout.getScene().setRoot(sideFlyoutPane);
            sideFlyout.setOnHidden(event -> {
                minimizedTabPanelShowing = false;
                if (isVerticalPlacement()) {
                    sideContentPane.getChildren().clear();
                }
                if (tabStrip.getScene() != null) {
                    tabStrip.requestFocus();
                }
            });
        }
        return sideFlyout;
    }

    private void hideSideFlyout() {
        if (sideFlyout != null) {
            sideFlyout.hide();
        }
        sideFlyoutPane.getChildren().clear();
        if (isVerticalPlacement() && !sideContentPane.getChildren().equals(List.of(groupScroller))) {
            sideContentPane.getChildren().setAll(groupScroller);
        }
    }

    private void updateSideFlyoutTheme() {
        sideFlyoutPane.setStyle(RibbonThemeSupport.themeVariables(theme.get()));
    }

    private RibbonGroup nextGroupToShrink(List<RibbonGroup> groups) {
        return groups.stream()
            .filter(group -> group.getSizeMode() != RibbonGroupSizeMode.COLLAPSED)
            .sorted(collapseOrderComparator)
            .findFirst()
            .orElse(null);
    }

    private RibbonGroup nextGroupToGrow(List<RibbonGroup> groups) {
        return groups.stream()
            .filter(group -> group.getSizeMode() != RibbonGroupSizeMode.LARGE)
            .sorted(restoreOrderComparator)
            .findFirst()
            .orElse(null);
    }

    private record GroupResolution(RibbonGroup group, boolean changed) {
    }
}
