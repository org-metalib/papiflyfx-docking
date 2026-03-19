package org.metalib.papifly.fx.settings.persist;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.metalib.papifly.fx.settings.api.SettingScope;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonSettingsStorageTest {

    @TempDir
    Path tempDir;

    @Test
    void savesAndReloadsApplicationAndWorkspaceScopes() {
        JsonSettingsStorage storage = new JsonSettingsStorage(tempDir.resolve("app"), tempDir.resolve("workspace"));

        storage.putString(SettingScope.APPLICATION, "appearance.theme", "dark");
        storage.putBoolean(SettingScope.WORKSPACE, "workspace.restore", true);
        storage.putMap(SettingScope.APPLICATION, "network.proxy", Map.of("host", "localhost", "port", 8080));
        storage.save();

        JsonSettingsStorage reloaded = new JsonSettingsStorage(tempDir.resolve("app"), tempDir.resolve("workspace"));

        assertEquals("dark", reloaded.getString(SettingScope.APPLICATION, "appearance.theme", "light"));
        assertTrue(reloaded.getBoolean(SettingScope.WORKSPACE, "workspace.restore", false));
        assertEquals("localhost", reloaded.getMap(SettingScope.APPLICATION, "network.proxy").get("host"));
    }

    @Test
    void keepsSessionScopeInMemoryOnly() {
        JsonSettingsStorage storage = new JsonSettingsStorage(tempDir.resolve("app"), tempDir.resolve("workspace"));

        storage.putBoolean(SettingScope.SESSION, "panel.open", true);
        storage.save();
        storage.reload();

        assertFalse(storage.getBoolean(SettingScope.SESSION, "panel.open", false));
    }

    @Test
    void migratesOlderFilesWhenMigratorIsRegistered() {
        JsonSettingsStorage writer = new JsonSettingsStorage(tempDir.resolve("app"), null);
        writer.putString(SettingScope.APPLICATION, "appearance.colorMode", "dark");
        writer.save();

        JsonSettingsStorage storage = new JsonSettingsStorage(tempDir.resolve("app"), null, 2);
        storage.registerMigrator(1, (data, version) -> {
            Object value = data.remove("appearance.colorMode");
            if (value != null) {
                data.put("appearance.theme", value);
            }
            return data;
        });
        storage.reload();

        assertEquals("dark", storage.getString(SettingScope.APPLICATION, "appearance.theme", "light"));
    }
}
