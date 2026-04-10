package org.metalib.papifly.fx.settings.categories;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.metalib.papifly.fx.settings.api.SettingDefinition;
import org.metalib.papifly.fx.settings.api.SettingScope;
import org.metalib.papifly.fx.settings.api.SecretStore;
import org.metalib.papifly.fx.settings.api.SettingsCategory;
import org.metalib.papifly.fx.settings.api.SettingsContext;
import org.metalib.papifly.fx.settings.secret.EncryptedFileSecretStore;
import org.metalib.papifly.fx.settings.ui.controls.SecretSettingControl;

import java.util.List;
import java.util.Set;

public class SecurityCategory implements SettingsCategory {

    private static final SettingDefinition<String> SECRET_DEFINITION = SettingDefinition
        .of("security.secret", "Secret Value", org.metalib.papifly.fx.settings.api.SettingType.SECRET, "")
        .withDescription("Store or update a secret value for the selected key.");

    private BorderPane pane;
    private ListView<String> keysView;
    private TextField keyField;
    private SecretSettingControl secretControl;
    private Label backendLabel;
    private Label warningLabel;
    private boolean dirty;

    @Override
    public String id() {
        return "security";
    }

    @Override
    public String displayName() {
        return "Security";
    }

    @Override
    public int order() {
        return 30;
    }

    @Override
    public List<SettingDefinition<?>> definitions() {
        return List.of();
    }

    @Override
    public Set<SettingScope> supportedScopes() {
        return Set.of(SettingScope.APPLICATION);
    }

    @Override
    public Node buildSettingsPane(SettingsContext context) {
        if (pane == null) {
            keysView = new ListView<>();
            keyField = new TextField();
            keyField.setPromptText("secret:key:name");
            secretControl = new SecretSettingControl(SECRET_DEFINITION);
            backendLabel = new Label();
            warningLabel = new Label();
            warningLabel.setStyle("-fx-text-fill: #b07000;");
            warningLabel.setWrapText(true);

            keyField.textProperty().addListener((obs, oldValue, newValue) -> dirty = true);
            secretControl.setOnChange(() -> dirty = true);
            keysView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> loadSelection(context, newValue));

            Button newButton = new Button("New");
            newButton.setOnAction(event -> {
                keysView.getSelectionModel().clearSelection();
                keyField.clear();
                secretControl.setValue("");
                dirty = false;
            });

            Button saveButton = new Button("Save Secret");
            saveButton.setOnAction(event -> {
                String key = keyField.getText() == null ? "" : keyField.getText().trim();
                if (key.isEmpty()) {
                    return;
                }
                context.secretStore().setSecret(key, secretControl.getValue());
                refreshKeys(context);
                keysView.getSelectionModel().select(key);
                dirty = false;
            });

            Button deleteButton = new Button("Delete Secret");
            deleteButton.setOnAction(event -> {
                String key = keyField.getText() == null ? "" : keyField.getText().trim();
                if (key.isEmpty()) {
                    return;
                }
                context.secretStore().clearSecret(key);
                refreshKeys(context);
                keyField.clear();
                secretControl.setValue("");
                dirty = false;
            });

            HBox buttons = new HBox(8, newButton, saveButton, deleteButton);
            VBox form = new VBox(12, backendLabel, warningLabel, keyField, secretControl, buttons);
            form.setPadding(new Insets(0, 0, 0, 12));
            VBox.setVgrow(secretControl, Priority.NEVER);

            pane = new BorderPane();
            pane.setLeft(keysView);
            pane.setCenter(form);
            keysView.setPrefWidth(220);
        }
        SecretStore secretStore = context.secretStore();
        backendLabel.setText("Secret backend: " + secretStore.backendName());
        boolean usingFallback = secretStore instanceof EncryptedFileSecretStore;
        warningLabel.setText(usingFallback
            ? "Secrets are stored in the encrypted-file backend. OS keychain integration is not active."
            : "");
        refreshKeys(context);
        dirty = false;
        return pane;
    }

    @Override
    public void apply(SettingsContext context) {
        String key = keyField.getText() == null ? "" : keyField.getText().trim();
        if (!key.isEmpty()) {
            context.secretStore().setSecret(key, secretControl.getValue());
        }
        refreshKeys(context);
        dirty = false;
    }

    @Override
    public void reset(SettingsContext context) {
        String selected = keysView == null ? null : keysView.getSelectionModel().getSelectedItem();
        refreshKeys(context);
        loadSelection(context, selected);
        dirty = false;
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }

    private void refreshKeys(SettingsContext context) {
        List<String> keys = context.secretStore().listKeys().stream().sorted().toList();
        keysView.getItems().setAll(keys);
    }

    private void loadSelection(SettingsContext context, String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        keyField.setText(key);
        secretControl.setValue(context.secretStore().getSecret(key).orElse(""));
        dirty = false;
    }
}
