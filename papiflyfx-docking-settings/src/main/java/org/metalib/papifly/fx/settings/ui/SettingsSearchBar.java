package org.metalib.papifly.fx.settings.ui;

import javafx.geometry.Insets;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

public class SettingsSearchBar extends HBox {

    private final TextField searchField;

    public SettingsSearchBar() {
        this.searchField = new TextField();
        this.searchField.setPromptText("Search settings...");
        setPadding(new Insets(8));
        getChildren().add(searchField);
    }

    public TextField getSearchField() {
        return searchField;
    }
}
