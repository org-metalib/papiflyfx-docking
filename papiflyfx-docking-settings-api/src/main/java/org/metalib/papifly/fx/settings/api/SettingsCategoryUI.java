package org.metalib.papifly.fx.settings.api;

import javafx.scene.Node;

/**
 * Owns the interactive view and lifecycle for a settings category.
 */
public interface SettingsCategoryUI {

    Node buildSettingsPane(SettingsContext context);

    void apply(SettingsContext context);

    void reset(SettingsContext context);

    default boolean isDirty() {
        return false;
    }
}
