package org.metalib.papifly.fx.samples;

import javafx.beans.property.ObjectProperty;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.login.core.DefaultAuthSessionBroker;
import org.metalib.papifly.fx.login.idapi.ProviderRegistry;
import org.metalib.papifly.fx.login.idapi.providers.GenericOidcProvider;
import org.metalib.papifly.fx.login.idapi.providers.GitHubProvider;
import org.metalib.papifly.fx.login.idapi.providers.GoogleProvider;
import org.metalib.papifly.fx.login.runtime.LoginRuntime;
import org.metalib.papifly.fx.settings.docking.SettingsStateAdapter;
import org.metalib.papifly.fx.settings.runtime.DefaultSettingsServicesProvider;
import org.metalib.papifly.fx.settings.runtime.SettingsRuntime;

import java.util.Objects;

public final class SamplesRuntimeSupport {

    private static final ProviderRegistry LOGIN_PROVIDER_REGISTRY = createProviderRegistry();
    private static SettingsRuntime settingsRuntime;
    private static LoginRuntime loginRuntime;

    private SamplesRuntimeSupport() {
    }

    public static synchronized void initialize(ObjectProperty<Theme> themeProperty) {
        SettingsRuntime resolvedSettingsRuntime = settingsRuntime(themeProperty);
        SettingsStateAdapter.setSharedRuntime(resolvedSettingsRuntime);
        DefaultSettingsServicesProvider.setSharedRuntime(resolvedSettingsRuntime);
        loginRuntime = LoginRuntime.of(
            new DefaultAuthSessionBroker(
                LOGIN_PROVIDER_REGISTRY,
                resolvedSettingsRuntime.storage(),
                resolvedSettingsRuntime.secretStore()
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

    public static synchronized LoginRuntime loginRuntime(ObjectProperty<Theme> themeProperty) {
        if (loginRuntime == null) {
            initialize(themeProperty);
        }
        return loginRuntime;
    }

    public static synchronized void setLoginRuntime(LoginRuntime runtime) {
        loginRuntime = Objects.requireNonNull(runtime, "runtime");
    }

    public static synchronized void resetForTests() {
        settingsRuntime = null;
        loginRuntime = null;
    }

    public static ProviderRegistry loginProviderRegistry() {
        return LOGIN_PROVIDER_REGISTRY;
    }

    private static ProviderRegistry createProviderRegistry() {
        ProviderRegistry registry = new ProviderRegistry();
        registry.register(new GoogleProvider());
        registry.register(new GitHubProvider());
        registry.register(new GenericOidcProvider());
        return registry;
    }
}
