package org.metalib.papifly.fx.code.settings;

import org.metalib.papifly.fx.code.api.CodeEditor;
import org.metalib.papifly.fx.settings.api.SettingScope;
import org.metalib.papifly.fx.settings.api.SettingsStorage;

public final class EditorSettingsSupport {

    private EditorSettingsSupport() {
    }

    public static void applyDefaults(CodeEditor editor) {
        SettingsStorage storage = locateStorage();
        if (storage == null) {
            return;
        }
        editor.setWordWrap(storage.getBoolean(SettingScope.APPLICATION, "editor.wordWrap", false));
        editor.setAutoDetectLanguage(storage.getBoolean(SettingScope.APPLICATION, "editor.autoDetectLanguage", false));
    }

    private static SettingsStorage locateStorage() {
        try {
            Class<?> runtimeClass = Class.forName("org.metalib.papifly.fx.settings.runtime.SettingsRuntime");
            Object value = runtimeClass.getMethod("defaultStorage").invoke(null);
            if (value instanceof SettingsStorage storage) {
                return storage;
            }
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
        return null;
    }
}
