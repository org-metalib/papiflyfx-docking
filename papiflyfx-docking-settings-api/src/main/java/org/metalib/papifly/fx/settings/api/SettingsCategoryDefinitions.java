package org.metalib.papifly.fx.settings.api;

import java.util.List;

/**
 * Exposes searchable definitions and toolbar actions for a settings category.
 */
public interface SettingsCategoryDefinitions {

    default List<SettingDefinition<?>> definitions() {
        return List.of();
    }

    default List<SettingsAction> actions() {
        return List.of();
    }
}
