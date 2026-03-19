package org.metalib.papifly.fx.login.runtime;

import org.metalib.papifly.fx.login.api.AuthSessionBroker;
import org.metalib.papifly.fx.login.core.DefaultAuthSessionBroker;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class LoginRuntime {

    private static final AtomicReference<AuthSessionBroker> DEFAULT_BROKER =
        new AtomicReference<>(new DefaultAuthSessionBroker());

    private LoginRuntime() {
    }

    public static AuthSessionBroker broker() {
        return DEFAULT_BROKER.get();
    }

    public static void setBroker(AuthSessionBroker broker) {
        DEFAULT_BROKER.set(Objects.requireNonNull(broker, "broker"));
    }
}
