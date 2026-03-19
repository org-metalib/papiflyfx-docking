package org.metalib.papifly.fx.settings.categories;

import javafx.scene.Node;
import javafx.scene.layout.VBox;
import org.metalib.papifly.fx.settings.api.SettingDefinition;
import org.metalib.papifly.fx.settings.api.SettingScope;
import org.metalib.papifly.fx.settings.api.SettingType;
import org.metalib.papifly.fx.settings.api.SettingsCategory;
import org.metalib.papifly.fx.settings.api.SettingsContext;
import org.metalib.papifly.fx.settings.ui.controls.BooleanSettingControl;
import org.metalib.papifly.fx.settings.ui.controls.EnumSettingControl;

import java.util.List;

public class WorkspaceCategory implements SettingsCategory {

    private enum LayoutPreset {
        IDE,
        DOCUMENT,
        REVIEW
    }

    private static final SettingDefinition<Boolean> RESTORE_DEFINITION = SettingDefinition
        .of("workspace.restoreOnStartup", "Restore On Startup", SettingType.BOOLEAN, true)
        .withScope(SettingScope.WORKSPACE)
        .withDescription("Restores the last saved workspace layout on startup.");
    private static final SettingDefinition<Boolean> ANIMATION_DEFINITION = SettingDefinition
        .of("workspace.animations", "Enable Animations", SettingType.BOOLEAN, true)
        .withScope(SettingScope.WORKSPACE)
        .withDescription("Enables docking transitions and panel animations.");
    private static final SettingDefinition<LayoutPreset> PRESET_DEFINITION = SettingDefinition
        .of("workspace.layoutPreset", "Layout Preset", SettingType.ENUM, LayoutPreset.IDE)
        .withScope(SettingScope.WORKSPACE)
        .withDescription("Selects the preferred workspace layout preset.");

    private BooleanSettingControl restoreControl;
    private BooleanSettingControl animationControl;
    private EnumSettingControl<LayoutPreset> presetControl;
    private VBox pane;
    private boolean dirty;

    @Override
    public String id() {
        return "workspace";
    }

    @Override
    public String displayName() {
        return "Workspace";
    }

    @Override
    public int order() {
        return 20;
    }

    @Override
    public List<SettingDefinition<?>> definitions() {
        return List.of(RESTORE_DEFINITION, ANIMATION_DEFINITION, PRESET_DEFINITION);
    }

    @Override
    public Node buildSettingsPane(SettingsContext context) {
        if (pane == null) {
            pane = new VBox(12);
            restoreControl = new BooleanSettingControl(RESTORE_DEFINITION);
            animationControl = new BooleanSettingControl(ANIMATION_DEFINITION);
            presetControl = new EnumSettingControl<>(PRESET_DEFINITION);
            restoreControl.setOnChange(this::markDirty);
            animationControl.setOnChange(this::markDirty);
            presetControl.setOnChange(this::markDirty);
            pane.getChildren().addAll(restoreControl, animationControl, presetControl);
        }
        reset(context);
        return pane;
    }

    @Override
    public void apply(SettingsContext context) {
        context.storage().putBoolean(SettingScope.WORKSPACE, RESTORE_DEFINITION.key(), restoreControl.getValue());
        context.storage().putBoolean(SettingScope.WORKSPACE, ANIMATION_DEFINITION.key(), animationControl.getValue());
        context.storage().putString(SettingScope.WORKSPACE, PRESET_DEFINITION.key(), presetControl.getValue().name().toLowerCase());
        context.storage().save();
        dirty = false;
    }

    @Override
    public void reset(SettingsContext context) {
        restoreControl.setValue(context.storage().getBoolean(SettingScope.WORKSPACE, RESTORE_DEFINITION.key(), RESTORE_DEFINITION.defaultValue()));
        animationControl.setValue(context.storage().getBoolean(SettingScope.WORKSPACE, ANIMATION_DEFINITION.key(), ANIMATION_DEFINITION.defaultValue()));
        String preset = context.storage().getString(SettingScope.WORKSPACE, PRESET_DEFINITION.key(), PRESET_DEFINITION.defaultValue().name().toLowerCase());
        presetControl.setValue("document".equalsIgnoreCase(preset) ? LayoutPreset.DOCUMENT : "review".equalsIgnoreCase(preset) ? LayoutPreset.REVIEW : LayoutPreset.IDE);
        dirty = false;
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }

    private void markDirty() {
        dirty = true;
    }
}
