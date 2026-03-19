package org.metalib.papifly.fx.settings.api;

import javafx.scene.Node;

import java.util.List;

public interface SettingsCategory {

    String id();

    String displayName();

    default Node icon() {
        return null;
    }

    default int order() {
        return 100;
    }

    default List<SettingDefinition<?>> definitions() {
        return List.of();
    }

    default List<SettingsAction> actions() {
        return List.of();
    }

    Node buildSettingsPane(SettingsContext context);

    void apply(SettingsContext context);

    void reset(SettingsContext context);

    default boolean isDirty() {
        return false;
    }
}
