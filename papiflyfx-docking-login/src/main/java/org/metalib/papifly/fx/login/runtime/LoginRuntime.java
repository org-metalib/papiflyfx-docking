package org.metalib.papifly.fx.login.runtime;

import org.metalib.papifly.fx.login.api.AuthSessionBroker;
import org.metalib.papifly.fx.login.core.DefaultAuthSessionBroker;
import org.metalib.papifly.fx.login.idapi.ProviderRegistry;
import org.metalib.papifly.fx.settings.api.SettingsServicesProvider;

import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;

public final class LoginRuntime {

    private static final AtomicReference<AuthSessionBroker> DEFAULT_BROKER = new AtomicReference<>();
    private static final AtomicReference<ProviderRegistry> DEFAULT_PROVIDER_REGISTRY = new AtomicReference<>();

    private LoginRuntime() {
    }

    public static AuthSessionBroker broker() {
        AuthSessionBroker broker = DEFAULT_BROKER.get();
        if (broker != null) {
            return broker;
        }

        AuthSessionBroker created = createDefaultBroker(providerRegistry());
        if (DEFAULT_BROKER.compareAndSet(null, created)) {
            return created;
        }
        return DEFAULT_BROKER.get();
    }

    public static ProviderRegistry providerRegistry() {
        ProviderRegistry registry = DEFAULT_PROVIDER_REGISTRY.get();
        if (registry != null) {
            return registry;
        }

        ProviderRegistry created = new ProviderRegistry();
        created.discoverProviders();
        if (DEFAULT_PROVIDER_REGISTRY.compareAndSet(null, created)) {
            return created;
        }
        return DEFAULT_PROVIDER_REGISTRY.get();
    }

    public static void configure(AuthSessionBroker broker, ProviderRegistry registry) {
        DEFAULT_PROVIDER_REGISTRY.set(Objects.requireNonNull(registry, "registry"));
        DEFAULT_BROKER.set(Objects.requireNonNull(broker, "broker"));
    }

    public static void setBroker(AuthSessionBroker broker) {
        DEFAULT_BROKER.set(Objects.requireNonNull(broker, "broker"));
    }

    static void resetForTests() {
        DEFAULT_BROKER.set(null);
        DEFAULT_PROVIDER_REGISTRY.set(null);
    }

    private static AuthSessionBroker createDefaultBroker(ProviderRegistry registry) {
        Optional<SettingsServicesProvider> settingsProvider = ServiceLoader.load(SettingsServicesProvider.class)
            .findFirst();
        if (settingsProvider.isPresent()) {
            SettingsServicesProvider provider = settingsProvider.get();
            return new DefaultAuthSessionBroker(registry, provider.storage(), provider.secretStore());
        }
        return new DefaultAuthSessionBroker();
    }
}
