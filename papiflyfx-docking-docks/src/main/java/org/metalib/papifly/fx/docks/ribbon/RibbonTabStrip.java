package org.metalib.papifly.fx.docks.ribbon;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import org.metalib.papifly.fx.api.ribbon.RibbonTabSpec;

import java.util.Objects;

/**
 * Horizontal selector for ribbon tabs.
 */
public class RibbonTabStrip extends HBox {

    private final ObservableList<RibbonTabSpec> tabs = FXCollections.observableArrayList();
    private final ToggleGroup toggleGroup = new ToggleGroup();
    private final StringProperty selectedTabId = new SimpleStringProperty();

    /**
     * Creates an empty ribbon tab strip.
     */
    public RibbonTabStrip() {
        getStyleClass().add("pf-ribbon-tab-strip");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(4.0);
        tabs.addListener((ListChangeListener<RibbonTabSpec>) change -> rebuild());
        selectedTabId.addListener((obs, oldValue, newValue) -> syncSelection());
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

    private void rebuild() {
        getChildren().clear();
        toggleGroup.getToggles().clear();
        ensureSelection();
        for (RibbonTabSpec tab : tabs) {
            ToggleButton button = new ToggleButton(tab.label());
            button.getProperties().put(RibbonTabSpec.class.getName(), tab.id());
            button.getStyleClass().add("pf-ribbon-tab");
            if (tab.contextual()) {
                button.getStyleClass().add("pf-ribbon-tab-contextual");
            }
            button.setToggleGroup(toggleGroup);
            button.setOnAction(event -> setSelectedTabId(tab.id()));
            getChildren().add(button);
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
}
