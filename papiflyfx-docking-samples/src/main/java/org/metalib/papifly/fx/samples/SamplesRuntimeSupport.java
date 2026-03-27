package org.metalib.papifly.fx.samples;

import javafx.beans.property.ObjectProperty;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.login.core.DefaultAuthSessionBroker;
import org.metalib.papifly.fx.login.idapi.ProviderRegistry;
import org.metalib.papifly.fx.login.idapi.providers.GenericOidcProvider;
import org.metalib.papifly.fx.login.idapi.providers.GitHubProvider;
import org.metalib.papifly.fx.login.idapi.providers.GoogleProvider;
import org.metalib.papifly.fx.login.runtime.LoginRuntime;
import org.metalib.papifly.fx.settings.runtime.SettingsRuntime;

public final class SamplesRuntimeSupport {

    private static final ProviderRegistry LOGIN_PROVIDER_REGISTRY = createProviderRegistry();
    private static SettingsRuntime settingsRuntime;

    private SamplesRuntimeSupport() {
    }

    public static synchronized void initialize(ObjectProperty<Theme> themeProperty) {
        settingsRuntime = SettingsRuntime.createDefault(themeProperty);
        LoginRuntime.configure(
            new DefaultAuthSessionBroker(
                LOGIN_PROVIDER_REGISTRY,
                settingsRuntime.storage(),
                settingsRuntime.secretStore()
            ),
            LOGIN_PROVIDER_REGISTRY
        );
    }

    public static synchronized SettingsRuntime settingsRuntime(ObjectProperty<Theme> themeProperty) {
        if (settingsRuntime == null) {
            settingsRuntime = SettingsRuntime.createDefault(themeProperty);
        }
        return settingsRuntime;
    }

    public static ProviderRegistry loginProviderRegistry() {
        return LoginRuntime.providerRegistry();
    }

    private static ProviderRegistry createProviderRegistry() {
        ProviderRegistry registry = new ProviderRegistry();
        registry.register(new GoogleProvider());
        registry.register(new GitHubProvider());
        registry.register(new GenericOidcProvider());
        return registry;
    }
}
