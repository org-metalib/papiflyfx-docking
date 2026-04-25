package org.metalib.papifly.fx.docks.ribbon;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Tooltip;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import org.metalib.papifly.fx.api.ribbon.RibbonTabSpec;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Horizontal selector for ribbon tabs.
 */
public class RibbonTabStrip extends FlowPane {

    private final ObservableList<RibbonTabSpec> tabs = FXCollections.observableArrayList();
    private final ToggleGroup toggleGroup = new ToggleGroup();
    private final StringProperty selectedTabId = new SimpleStringProperty();
    private final Map<String, TabRenderState> renderedTabs = new LinkedHashMap<>();
    private Orientation orientation = Orientation.HORIZONTAL;

    private RibbonLayoutTelemetry telemetry = RibbonLayoutTelemetry.noop();
    private Consumer<String> tabActivated = tabId -> {
    };

    /**
     * Creates an empty ribbon tab strip.
     */
    public RibbonTabStrip() {
        getStyleClass().add("pf-ribbon-tab-strip");
        setAlignment(Pos.CENTER_LEFT);
        setHgap(4.0);
        setVgap(4.0);
        tabs.addListener((ListChangeListener<RibbonTabSpec>) change -> rebuild());
        selectedTabId.addListener((obs, oldValue, newValue) -> syncSelection());
    }

    void setRibbonOrientation(Orientation orientation) {
        Orientation resolvedOrientation = orientation == null ? Orientation.HORIZONTAL : orientation;
        if (this.orientation == resolvedOrientation) {
            return;
        }
        this.orientation = resolvedOrientation;
        setOrientation(resolvedOrientation);
        setAlignment(resolvedOrientation == Orientation.VERTICAL ? Pos.TOP_CENTER : Pos.CENTER_LEFT);
        getStyleClass().removeAll("pf-ribbon-tab-strip-horizontal", "pf-ribbon-tab-strip-vertical");
        getStyleClass().add(resolvedOrientation == Orientation.VERTICAL
            ? "pf-ribbon-tab-strip-vertical"
            : "pf-ribbon-tab-strip-horizontal");
        rebuild();
    }

    /**
     * Returns the mutable tab list shown in the strip.
     *
     * @return mutable tab list
     */
    public ObservableList<RibbonTabSpec> getTabs() {
        return tabs;
    }

    /**
     * Returns the selected tab identifier.
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
     * Returns the observable selected tab identifier property.
     *
     * @return selected tab identifier property
     */
    public StringProperty selectedTabIdProperty() {
        return selectedTabId;
    }

    void setLayoutTelemetry(RibbonLayoutTelemetry telemetry) {
        this.telemetry = telemetry == null ? RibbonLayoutTelemetry.noop() : telemetry;
    }

    void setOnTabActivated(Consumer<String> tabActivated) {
        this.tabActivated = tabActivated == null ? tabId -> {
        } : tabActivated;
    }

    private void rebuild() {
        ensureSelection();
        List<ToggleButton> desiredButtons = new ArrayList<>(tabs.size());
        for (RibbonTabSpec tab : tabs) {
            TabRenderState existing = renderedTabs.get(tab.id());
            if (existing != null) {
                telemetry.nodeCacheHit(RibbonLayoutTelemetry.CacheKind.TAB, tab.id());
                if (!existing.matches(tab, orientation)) {
                    telemetry.tabRebuild(tab.id(), RibbonLayoutTelemetry.RebuildReason.STRUCTURAL);
                    configureButton(existing.button(), tab);
                    renderedTabs.put(tab.id(), new TabRenderState(tab.label(), tab.contextual(), orientation, existing.button()));
                }
                desiredButtons.add(existing.button());
                continue;
            }

            telemetry.nodeCacheMiss(RibbonLayoutTelemetry.CacheKind.TAB, tab.id());
            telemetry.tabRebuild(tab.id(), RibbonLayoutTelemetry.RebuildReason.INITIAL);
            ToggleButton button = new ToggleButton();
            configureButton(button, tab);
            renderedTabs.put(tab.id(), new TabRenderState(tab.label(), tab.contextual(), orientation, button));
            desiredButtons.add(button);
        }
        if (!getChildren().equals(desiredButtons)) {
            getChildren().setAll(desiredButtons);
        }
        syncSelection();
    }

    private void ensureSelection() {
        if (tabs.isEmpty()) {
            if (selectedTabId.get() != null) {
                selectedTabId.set(null);
            }
            return;
        }
        boolean present = tabs.stream().anyMatch(tab -> Objects.equals(tab.id(), selectedTabId.get()));
        if (!present) {
            selectedTabId.set(tabs.getFirst().id());
        }
    }

    private void syncSelection() {
        toggleGroup.getToggles().forEach(toggle ->
            toggle.setSelected(Objects.equals(toggle.getProperties().get(RibbonTabSpec.class.getName()), selectedTabId.get())));
    }

    private void configureButton(ToggleButton button, RibbonTabSpec tab) {
        button.setText(tab.label());
        button.getProperties().put(RibbonTabSpec.class.getName(), tab.id());
        button.getStyleClass().setAll("toggle-button", "pf-ribbon-tab");
        if (orientation == Orientation.VERTICAL) {
            button.getStyleClass().add("pf-ribbon-side-tab");
        }
        if (tab.contextual()) {
            button.getStyleClass().add("pf-ribbon-tab-contextual");
        }
        button.setAccessibleText(tab.label());
        button.setTooltip(new Tooltip(tab.label()));
        if (button.getToggleGroup() != toggleGroup) {
            button.setToggleGroup(toggleGroup);
        }
        button.setOnAction(event -> {
            setSelectedTabId(tab.id());
            tabActivated.accept(tab.id());
        });
    }

    private record TabRenderState(String label, boolean contextual, Orientation orientation, ToggleButton button) {
        boolean matches(RibbonTabSpec tab, Orientation currentOrientation) {
            return Objects.equals(label, tab.label()) && contextual == tab.contextual() && orientation == currentOrientation;
        }
    }
}
