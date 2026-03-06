package org.metalib.papifly.fx.github.auth;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class PatCredentialStore implements CredentialStore {

    private final AtomicReference<String> token = new AtomicReference<>("");

    @Override
    public Optional<String> getToken() {
        String current = token.get();
        if (current == null || current.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(current);
    }

    @Override
    public void setToken(String token) {
        this.token.set(token == null ? "" : token.trim());
    }

    @Override
    public void clearToken() {
        token.set("");
    }
}
