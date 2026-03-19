package org.metalib.papifly.fx.settings.runtime;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.settings.api.SettingScope;
import org.metalib.papifly.fx.settings.api.SettingsContext;
import org.metalib.papifly.fx.settings.api.SettingsStorage;
import org.metalib.papifly.fx.settings.api.SecretStore;
import org.metalib.papifly.fx.settings.persist.JsonSettingsStorage;
import org.metalib.papifly.fx.settings.secret.SecretStoreFactory;

import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class SettingsRuntime {

    private static final AtomicReference<SettingsRuntime> DEFAULT_RUNTIME = new AtomicReference<>();

    private final Path applicationDir;
    private final Path workspaceRoot;
    private final SettingsStorage storage;
    private final SecretStore secretStore;
    private final ObjectProperty<Theme> themeProperty;

    public SettingsRuntime(
        Path applicationDir,
        Path workspaceRoot,
        SettingsStorage storage,
        SecretStore secretStore,
        ObjectProperty<Theme> themeProperty
    ) {
        this.applicationDir = Objects.requireNonNull(applicationDir, "applicationDir");
        this.workspaceRoot = workspaceRoot;
        this.storage = Objects.requireNonNull(storage, "storage");
        this.secretStore = Objects.requireNonNull(secretStore, "secretStore");
        this.themeProperty = Objects.requireNonNull(themeProperty, "themeProperty");
    }

    public static SettingsRuntime createDefault(ObjectProperty<Theme> themeProperty) {
        Path applicationDir = resolveApplicationDir();
        Path workspaceRoot = resolveWorkspaceRoot();
        JsonSettingsStorage storage = new JsonSettingsStorage(
            applicationDir,
            workspaceRoot == null ? null : workspaceRoot.resolve(".papiflyfx")
        );
        SettingsRuntime runtime = new SettingsRuntime(
            applicationDir,
            workspaceRoot,
            storage,
            SecretStoreFactory.createDefault(applicationDir),
            themeProperty
        );
        DEFAULT_RUNTIME.set(runtime);
        return runtime;
    }

    public static SettingsRuntime getDefault() {
        SettingsRuntime existing = DEFAULT_RUNTIME.get();
        if (existing != null) {
            return existing;
        }
        return createDefault(new SimpleObjectProperty<>(Theme.dark()));
    }

    public static void setDefault(SettingsRuntime runtime) {
        DEFAULT_RUNTIME.set(runtime);
    }

    public static SecretStore defaultSecretStore() {
        return getDefault().secretStore();
    }

    public static SettingsStorage defaultStorage() {
        return getDefault().storage();
    }

    public SettingsContext context(SettingScope activeScope) {
        return new SettingsContext(storage, secretStore, themeProperty, activeScope);
    }

    public Path applicationDir() {
        return applicationDir;
    }

    public Path workspaceRoot() {
        return workspaceRoot;
    }

    public SettingsStorage storage() {
        return storage;
    }

    public SecretStore secretStore() {
        return secretStore;
    }

    public ObjectProperty<Theme> themeProperty() {
        return themeProperty;
    }

    private static Path resolveWorkspaceRoot() {
        String configured = System.getProperty("papiflyfx.workspace.root", "").trim();
        if (!configured.isEmpty()) {
            return Path.of(configured).toAbsolutePath().normalize();
        }
        return Path.of("").toAbsolutePath().normalize();
    }

    private static Path resolveApplicationDir() {
        String configured = System.getProperty("papiflyfx.app.dir", "").trim();
        if (!configured.isEmpty()) {
            return Path.of(configured).toAbsolutePath().normalize();
        }
        return Path.of(System.getProperty("user.home"), ".papiflyfx");
    }
}
