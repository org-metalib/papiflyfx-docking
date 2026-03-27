package org.metalib.papifly.fx.settings.runtime;

import org.metalib.papifly.fx.settings.api.SecretStore;
import org.metalib.papifly.fx.settings.api.SettingsServicesProvider;
import org.metalib.papifly.fx.settings.api.SettingsStorage;

public final class DefaultSettingsServicesProvider implements SettingsServicesProvider {

    @Override
    public SettingsStorage storage() {
        return SettingsRuntime.getDefault().storage();
    }

    @Override
    public SecretStore secretStore() {
        return SettingsRuntime.getDefault().secretStore();
    }
}
