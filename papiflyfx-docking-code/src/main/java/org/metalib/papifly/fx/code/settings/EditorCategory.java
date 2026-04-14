package org.metalib.papifly.fx.code.settings;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.metalib.papifly.fx.settings.api.SettingDefinition;
import org.metalib.papifly.fx.settings.api.SettingScope;
import org.metalib.papifly.fx.settings.api.SettingType;
import org.metalib.papifly.fx.settings.api.SettingsCategory;
import org.metalib.papifly.fx.settings.api.SettingsContext;
import org.metalib.papifly.fx.ui.UiMetrics;

import java.util.List;

public class EditorCategory implements SettingsCategory {

    private static final SettingDefinition<Boolean> WORD_WRAP_DEFINITION = SettingDefinition
        .of("editor.wordWrap", "Word Wrap", SettingType.BOOLEAN, false)
        .withDescription("Enable soft wrapping for new editors.");
    private static final SettingDefinition<Integer> TAB_SIZE_DEFINITION = SettingDefinition
        .of("editor.tabSize", "Tab Size", SettingType.INTEGER, 4)
        .withDescription("Stored editor tab size preference.");
    private static final SettingDefinition<Integer> FONT_SIZE_DEFINITION = SettingDefinition
        .of("editor.fontSize", "Font Size", SettingType.INTEGER, 12)
        .withDescription("Stored editor font size preference.");
    private static final SettingDefinition<Boolean> AUTO_DETECT_DEFINITION = SettingDefinition
        .of("editor.autoDetectLanguage", "Auto Detect Language", SettingType.BOOLEAN, false)
        .withDescription("Detect language automatically from the file path.");

    private CheckBox wordWrapBox;
    private TextField tabSizeField;
    private TextField fontSizeField;
    private CheckBox autoDetectBox;
    private VBox pane;
    private boolean dirty;

    @Override
    public String id() {
        return "editor";
    }

    @Override
    public String displayName() {
        return "Editor";
    }

    @Override
    public int order() {
        return 50;
    }

    @Override
    public List<SettingDefinition<?>> definitions() {
        return List.of(WORD_WRAP_DEFINITION, TAB_SIZE_DEFINITION, FONT_SIZE_DEFINITION, AUTO_DETECT_DEFINITION);
    }

    @Override
    public Node buildSettingsPane(SettingsContext context) {
        if (pane == null) {
            wordWrapBox = settingsCheckBox(new CheckBox("Enable word wrap"));
            tabSizeField = compactField(new TextField());
            fontSizeField = compactField(new TextField());
            autoDetectBox = settingsCheckBox(new CheckBox("Auto detect language"));

            wordWrapBox.selectedProperty().addListener((obs, oldValue, newValue) -> dirty = true);
            tabSizeField.textProperty().addListener((obs, oldValue, newValue) -> dirty = true);
            fontSizeField.textProperty().addListener((obs, oldValue, newValue) -> dirty = true);
            autoDetectBox.selectedProperty().addListener((obs, oldValue, newValue) -> dirty = true);

            pane = new VBox(
                UiMetrics.SPACE_3,
                wordWrapBox,
                field("Tab Size", tabSizeField),
                field("Font Size", fontSizeField),
                autoDetectBox
            );
            pane.setPadding(new Insets(UiMetrics.SPACE_2));
        }
        reset(context);
        return pane;
    }

    @Override
    public void apply(SettingsContext context) {
        context.storage().putBoolean(SettingScope.APPLICATION, WORD_WRAP_DEFINITION.key(), wordWrapBox.isSelected());
        context.storage().putInt(SettingScope.APPLICATION, TAB_SIZE_DEFINITION.key(), parse(tabSizeField.getText(), 4));
        context.storage().putInt(SettingScope.APPLICATION, FONT_SIZE_DEFINITION.key(), parse(fontSizeField.getText(), 12));
        context.storage().putBoolean(SettingScope.APPLICATION, AUTO_DETECT_DEFINITION.key(), autoDetectBox.isSelected());
        context.storage().save();
        dirty = false;
    }

    @Override
    public void reset(SettingsContext context) {
        wordWrapBox.setSelected(context.storage().getBoolean(SettingScope.APPLICATION, WORD_WRAP_DEFINITION.key(), WORD_WRAP_DEFINITION.defaultValue()));
        tabSizeField.setText(String.valueOf(context.storage().getInt(SettingScope.APPLICATION, TAB_SIZE_DEFINITION.key(), TAB_SIZE_DEFINITION.defaultValue())));
        fontSizeField.setText(String.valueOf(context.storage().getInt(SettingScope.APPLICATION, FONT_SIZE_DEFINITION.key(), FONT_SIZE_DEFINITION.defaultValue())));
        autoDetectBox.setSelected(context.storage().getBoolean(SettingScope.APPLICATION, AUTO_DETECT_DEFINITION.key(), AUTO_DETECT_DEFINITION.defaultValue()));
        dirty = false;
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }

    private VBox field(String labelText, TextField field) {
        Label label = new Label(labelText);
        label.getStyleClass().add("pf-settings-control-title");
        return new VBox(UiMetrics.SPACE_1, label, field);
    }

    private static <T extends TextField> T compactField(T field) {
        field.getStyleClass().add("pf-ui-compact-field");
        return field;
    }

    private static <T extends CheckBox> T settingsCheckBox(T checkBox) {
        checkBox.getStyleClass().add("pf-settings-check-box");
        return checkBox;
    }

    private int parse(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }
}
