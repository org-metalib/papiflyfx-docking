package org.metalib.papifly.fx.docks.ribbon;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ListChangeListener;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.metalib.papifly.fx.api.ribbon.PapiflyCommand;
import org.metalib.papifly.fx.api.ribbon.RibbonTabSpec;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.docks.layout.data.RibbonSessionData;
import org.metalib.papifly.fx.ui.UiStyleSupport;

import java.util.Comparator;
import java.util.List;
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

    private final QuickAccessToolbar quickAccessToolbar = new QuickAccessToolbar();
    private final RibbonTabStrip tabStrip = new RibbonTabStrip();
    private final HBox groupRow = new HBox(12.0);
    private final ScrollPane groupScroller = new ScrollPane(groupRow);
    private final Button minimizeButton = new Button("Collapse");
    private final BorderPane header = new BorderPane();
    private final HBox headerActions = new HBox();
    private final Comparator<RibbonGroup> collapseOrderComparator =
        Comparator.comparingInt((RibbonGroup group) -> group.getSpec().collapseOrder())
            .thenComparingInt(group -> group.getSpec().order())
            .thenComparing(group -> group.getSpec().id());

    private final ListChangeListener<RibbonTabSpec> tabsListener = change -> refreshTabs();
    private final ListChangeListener<PapiflyCommand> quickAccessListener = change -> refreshQuickAccessToolbar();

    private RibbonManager manager;
    private boolean adaptiveLayoutRequested;

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

        minimizeButton.getStyleClass().addAll("pf-ui-compact-action-button", "pf-ribbon-collapse-button");
        minimizeButton.setOnAction(event -> setMinimized(!isMinimized()));

        header.getStyleClass().add("pf-ribbon-header");
        headerActions.getStyleClass().add("pf-ribbon-header-actions");
        headerActions.setAlignment(Pos.CENTER_RIGHT);
        headerActions.getChildren().add(minimizeButton);
        header.setLeft(quickAccessToolbar);
        header.setCenter(tabStrip);
        header.setRight(headerActions);
        BorderPane.setAlignment(headerActions, Pos.CENTER_RIGHT);
        HBox.setHgrow(tabStrip, Priority.ALWAYS);

        getChildren().addAll(header, groupScroller);

        theme.addListener((obs, oldTheme, newTheme) -> applyTheme(newTheme));
        minimized.addListener((obs, oldValue, newValue) -> updateMinimizedState());
        selectedTabId.addListener((obs, oldValue, newValue) -> rebuildGroups());
        widthProperty().addListener((obs, oldWidth, newWidth) -> requestAdaptiveLayout());

        setManager(manager);
        applyTheme(theme.get());
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
        quickAccessToolbar.setClassLoader(this.manager.getClassLoader());
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
        return new RibbonSessionData(isMinimized(), getSelectedTabId(), quickAccessCommandIds);
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
        setMinimized(state.minimized());
    }

    private void refreshQuickAccessToolbar() {
        quickAccessToolbar.getCommands().setAll(manager.getQuickAccessCommands());
    }

    private void refreshTabs() {
        tabStrip.getTabs().setAll(manager.getTabs());
        ensureSelectedTab();
        rebuildGroups();
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

    private void rebuildGroups() {
        groupRow.getChildren().clear();
        if (isMinimized() || manager == null || manager.getTabs().isEmpty()) {
            return;
        }
        RibbonTabSpec selectedTab = manager.getTabs().stream()
            .filter(tab -> Objects.equals(tab.id(), selectedTabId.get()))
            .findFirst()
            .orElse(manager.getTabs().getFirst());
        groupRow.getChildren().setAll(selectedTab.groups().stream()
            .map(group -> new RibbonGroup(group, manager.getClassLoader(), theme))
            .toList());
        requestAdaptiveLayout();
    }

    private void updateMinimizedState() {
        groupScroller.setManaged(!isMinimized());
        groupScroller.setVisible(!isMinimized());
        minimizeButton.setText(isMinimized() ? "Expand" : "Collapse");
        if (!isMinimized()) {
            requestAdaptiveLayout();
        }
    }

    private void applyTheme(Theme theme) {
        setStyle(RibbonThemeSupport.themeVariables(theme));
    }

    List<RibbonGroup> getRenderedGroups() {
        return groupRow.getChildren().stream()
            .filter(RibbonGroup.class::isInstance)
            .map(RibbonGroup.class::cast)
            .toList();
    }

    private void requestAdaptiveLayout() {
        if (adaptiveLayoutRequested || isMinimized()) {
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

        double availableWidth = groupScroller.getViewportBounds().getWidth();
        if (availableWidth <= 0.0) {
            availableWidth = groupScroller.getWidth();
        }
        if (availableWidth <= 0.0) {
            return;
        }

        groups.forEach(group -> group.setSizeMode(RibbonGroupSizeMode.LARGE));
        double totalWidth = estimatedTotalWidth(groups);
        if (totalWidth <= availableWidth) {
            return;
        }

        for (RibbonGroup group : groups.stream().sorted(collapseOrderComparator).toList()) {
            while (totalWidth > availableWidth && group.getSizeMode() != RibbonGroupSizeMode.COLLAPSED) {
                group.setSizeMode(group.getSizeMode().smaller());
                totalWidth = estimatedTotalWidth(groups);
            }
            if (totalWidth <= availableWidth) {
                break;
            }
        }
    }

    private double estimatedTotalWidth(List<RibbonGroup> groups) {
        double widths = groups.stream()
            .mapToDouble(group -> group.estimateWidth(group.getSizeMode()))
            .sum();
        double spacing = Math.max(0, groups.size() - 1) * groupRow.getSpacing();
        return widths + spacing;
    }
}
