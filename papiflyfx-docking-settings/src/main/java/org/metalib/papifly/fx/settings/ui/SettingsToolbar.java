package org.metalib.papifly.fx.settings.ui;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import org.metalib.papifly.fx.settings.api.SettingScope;
import org.metalib.papifly.fx.settings.api.SettingsAction;
import org.metalib.papifly.fx.settings.api.SettingsContext;
import org.metalib.papifly.fx.settings.api.ValidationResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class SettingsToolbar extends BorderPane {

    private final Button applyButton;
    private final Button resetButton;
    private final Label dirtyLabel;
    private final Label statusLabel;
    private final ComboBox<SettingScope> scopeSelector;
    private final HBox actionBox;
    private final ObjectProperty<SettingScope> activeScope = new SimpleObjectProperty<>(SettingScope.APPLICATION);
    private boolean updatingScopes;

    public SettingsToolbar() {
        this.applyButton = new Button("Apply");
        this.resetButton = new Button("Reset");
        this.dirtyLabel = new Label();
        this.statusLabel = new Label();
        this.scopeSelector = new ComboBox<>();
        this.actionBox = new HBox(8);

        scopeSelector.getItems().addAll(SettingScope.APPLICATION, SettingScope.WORKSPACE, SettingScope.SESSION);
        scopeSelector.valueProperty().bindBidirectional(activeScope);
        scopeSelector.setValue(SettingScope.APPLICATION);

        HBox left = new HBox(8, applyButton, resetButton, dirtyLabel, statusLabel);
        HBox right = new HBox(8, new Label("Scope"), scopeSelector, actionBox);
        left.setAlignment(Pos.CENTER_LEFT);
        right.setAlignment(Pos.CENTER_RIGHT);

        setPadding(new Insets(8));
        setLeft(left);
        setRight(right);
        dirtyLabel.setStyle("-fx-text-fill: #c08000;");
        statusLabel.setStyle("-fx-text-fill: #6a8f4e;");
    }

    public void onApply(Runnable action) {
        applyButton.setOnAction(event -> action.run());
    }

    public void onReset(Runnable action) {
        resetButton.setOnAction(event -> action.run());
    }

    public ObjectProperty<SettingScope> activeScopeProperty() {
        return activeScope;
    }

    public SettingScope getActiveScope() {
        return activeScope.get();
    }

    /**
     * Updates the scope selector to only show scopes supported by the active category.
     * If the currently selected scope is not in the new set, resets to the first available scope.
     */
    public void setSupportedScopes(Set<SettingScope> scopes) {
        if (updatingScopes) {
            return;
        }
        updatingScopes = true;
        try {
            List<SettingScope> filtered = new ArrayList<>();
            for (SettingScope scope : List.of(SettingScope.APPLICATION, SettingScope.WORKSPACE, SettingScope.SESSION)) {
                if (scopes.contains(scope)) {
                    filtered.add(scope);
                }
            }
            if (filtered.isEmpty()) {
                filtered.add(SettingScope.APPLICATION);
            }
            SettingScope current = activeScope.get();
            scopeSelector.getItems().setAll(filtered);
            if (!filtered.contains(current)) {
                scopeSelector.setValue(filtered.getFirst());
            }
            scopeSelector.setDisable(filtered.size() <= 1);
        } finally {
            updatingScopes = false;
        }
    }

    public void setDirty(boolean dirty) {
        dirtyLabel.setText(dirty ? "Modified" : "");
    }

    public void setActions(List<SettingsAction> actions, Supplier<SettingsContext> contextSupplier) {
        actionBox.getChildren().clear();
        for (SettingsAction action : actions) {
            Button button = new Button(action.label());
            button.setOnAction(event -> runAction(action, contextSupplier.get()));
            actionBox.getChildren().add(button);
        }
    }

    public void setStatus(String message) {
        statusLabel.setText(message == null ? "" : message);
    }

    private void runAction(SettingsAction action, SettingsContext context) {
        setStatus(action.description());
        CompletableFuture<ValidationResult> future = action.handler().apply(context);
        future.whenComplete((result, error) -> Platform.runLater(() -> {
            if (error != null) {
                setStatus(error.getMessage());
                return;
            }
            setStatus(result == null ? "" : result.message());
        }));
    }
}
