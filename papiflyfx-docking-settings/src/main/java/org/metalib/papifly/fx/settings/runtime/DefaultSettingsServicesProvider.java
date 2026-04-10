package org.metalib.papifly.fx.settings.runtime;

import javafx.beans.property.SimpleObjectProperty;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.settings.api.SecretStore;
import org.metalib.papifly.fx.settings.api.SettingsServicesProvider;
import org.metalib.papifly.fx.settings.api.SettingsStorage;

public final class DefaultSettingsServicesProvider implements SettingsServicesProvider {

    private final SettingsRuntime runtime;

    public DefaultSettingsServicesProvider() {
        this(SettingsRuntime.createDefault(new SimpleObjectProperty<>(Theme.dark())));
    }

    DefaultSettingsServicesProvider(SettingsRuntime runtime) {
        this.runtime = runtime;
    }

    @Override
    public SettingsStorage storage() {
        return runtime.storage();
    }

    @Override
    public SecretStore secretStore() {
        return runtime.secretStore();
    }
}
