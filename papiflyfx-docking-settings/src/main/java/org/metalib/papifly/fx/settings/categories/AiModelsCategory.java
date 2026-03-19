package org.metalib.papifly.fx.settings.categories;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.metalib.papifly.fx.settings.api.SecretKeyNames;
import org.metalib.papifly.fx.settings.api.SettingDefinition;
import org.metalib.papifly.fx.settings.api.SettingScope;
import org.metalib.papifly.fx.settings.api.SettingType;
import org.metalib.papifly.fx.settings.api.SettingsCategory;
import org.metalib.papifly.fx.settings.api.SettingsContext;

import java.util.List;

public class AiModelsCategory implements SettingsCategory {

    private enum Provider {
        OPENAI,
        ANTHROPIC,
        GOOGLE
    }

    private static final SettingDefinition<Provider> DEFAULT_PROVIDER = SettingDefinition
        .of("ai.defaultProvider", "Default Provider", SettingType.ENUM, Provider.OPENAI)
        .withDescription("Provider used for default AI operations.");
    private static final SettingDefinition<String> DEFAULT_MODEL = SettingDefinition
        .of("ai.defaultModel", "Default Model", SettingType.STRING, "gpt-5.4")
        .withDescription("Default model identifier.");

    private ComboBox<Provider> providerBox;
    private TextField defaultModelField;
    private TextField openAiBaseUrlField;
    private TextField openAiModelField;
    private PasswordField openAiKeyField;
    private TextField anthropicModelField;
    private PasswordField anthropicKeyField;
    private TextField googleModelField;
    private PasswordField googleKeyField;
    private VBox pane;
    private boolean dirty;

    @Override
    public String id() {
        return "ai-models";
    }

    @Override
    public String displayName() {
        return "AI Models";
    }

    @Override
    public int order() {
        return 85;
    }

    @Override
    public List<SettingDefinition<?>> definitions() {
        return List.of(
            DEFAULT_PROVIDER,
            DEFAULT_MODEL,
            SettingDefinition.of("ai.openai.baseUrl", "OpenAI Base URL", SettingType.STRING, "https://api.openai.com/v1"),
            SettingDefinition.of("ai.openai.model", "OpenAI Model", SettingType.STRING, "gpt-5.4"),
            SettingDefinition.of("ai.anthropic.model", "Anthropic Model", SettingType.STRING, "claude-sonnet-4"),
            SettingDefinition.of("ai.google.model", "Google Model", SettingType.STRING, "gemini-2.5-pro")
        );
    }

    @Override
    public Node buildSettingsPane(SettingsContext context) {
        if (pane == null) {
            providerBox = new ComboBox<>();
            providerBox.getItems().addAll(Provider.values());
            defaultModelField = new TextField();
            openAiBaseUrlField = new TextField();
            openAiModelField = new TextField();
            openAiKeyField = new PasswordField();
            anthropicModelField = new TextField();
            anthropicKeyField = new PasswordField();
            googleModelField = new TextField();
            googleKeyField = new PasswordField();

            providerBox.valueProperty().addListener((obs, oldValue, newValue) -> dirty = true);
            defaultModelField.textProperty().addListener((obs, oldValue, newValue) -> dirty = true);
            openAiBaseUrlField.textProperty().addListener((obs, oldValue, newValue) -> dirty = true);
            openAiModelField.textProperty().addListener((obs, oldValue, newValue) -> dirty = true);
            openAiKeyField.textProperty().addListener((obs, oldValue, newValue) -> dirty = true);
            anthropicModelField.textProperty().addListener((obs, oldValue, newValue) -> dirty = true);
            anthropicKeyField.textProperty().addListener((obs, oldValue, newValue) -> dirty = true);
            googleModelField.textProperty().addListener((obs, oldValue, newValue) -> dirty = true);
            googleKeyField.textProperty().addListener((obs, oldValue, newValue) -> dirty = true);

            pane = new VBox(
                12,
                field("Default Provider", providerBox),
                field("Default Model", defaultModelField),
                field("OpenAI Base URL", openAiBaseUrlField),
                field("OpenAI Model", openAiModelField),
                field("OpenAI API Key", openAiKeyField),
                field("Anthropic Model", anthropicModelField),
                field("Anthropic API Key", anthropicKeyField),
                field("Google Model", googleModelField),
                field("Google API Key", googleKeyField)
            );
            pane.setPadding(new Insets(8));
        }
        reset(context);
        return pane;
    }

    @Override
    public void apply(SettingsContext context) {
        context.storage().putString(SettingScope.APPLICATION, DEFAULT_PROVIDER.key(), providerBox.getValue().name().toLowerCase());
        context.storage().putString(SettingScope.APPLICATION, DEFAULT_MODEL.key(), defaultModelField.getText());
        context.storage().putString(SettingScope.APPLICATION, "ai.openai.baseUrl", openAiBaseUrlField.getText());
        context.storage().putString(SettingScope.APPLICATION, "ai.openai.model", openAiModelField.getText());
        context.secretStore().setSecret(SecretKeyNames.settingsKey("openai", "api-key"), openAiKeyField.getText());
        context.storage().putString(SettingScope.APPLICATION, "ai.anthropic.model", anthropicModelField.getText());
        context.secretStore().setSecret(SecretKeyNames.settingsKey("anthropic", "api-key"), anthropicKeyField.getText());
        context.storage().putString(SettingScope.APPLICATION, "ai.google.model", googleModelField.getText());
        context.secretStore().setSecret(SecretKeyNames.settingsKey("google", "api-key"), googleKeyField.getText());
        context.storage().save();
        dirty = false;
    }

    @Override
    public void reset(SettingsContext context) {
        String provider = context.storage().getString(SettingScope.APPLICATION, DEFAULT_PROVIDER.key(), DEFAULT_PROVIDER.defaultValue().name().toLowerCase());
        providerBox.setValue("anthropic".equalsIgnoreCase(provider) ? Provider.ANTHROPIC : "google".equalsIgnoreCase(provider) ? Provider.GOOGLE : Provider.OPENAI);
        defaultModelField.setText(context.storage().getString(SettingScope.APPLICATION, DEFAULT_MODEL.key(), DEFAULT_MODEL.defaultValue()));
        openAiBaseUrlField.setText(context.storage().getString(SettingScope.APPLICATION, "ai.openai.baseUrl", "https://api.openai.com/v1"));
        openAiModelField.setText(context.storage().getString(SettingScope.APPLICATION, "ai.openai.model", "gpt-5.4"));
        openAiKeyField.setText(context.secretStore().getSecret(SecretKeyNames.settingsKey("openai", "api-key")).orElse(""));
        anthropicModelField.setText(context.storage().getString(SettingScope.APPLICATION, "ai.anthropic.model", "claude-sonnet-4"));
        anthropicKeyField.setText(context.secretStore().getSecret(SecretKeyNames.settingsKey("anthropic", "api-key")).orElse(""));
        googleModelField.setText(context.storage().getString(SettingScope.APPLICATION, "ai.google.model", "gemini-2.5-pro"));
        googleKeyField.setText(context.secretStore().getSecret(SecretKeyNames.settingsKey("google", "api-key")).orElse(""));
        dirty = false;
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }

    private VBox field(String labelText, Node field) {
        Label label = new Label(labelText);
        label.setStyle("-fx-font-weight: bold;");
        return new VBox(4, label, field);
    }
}
