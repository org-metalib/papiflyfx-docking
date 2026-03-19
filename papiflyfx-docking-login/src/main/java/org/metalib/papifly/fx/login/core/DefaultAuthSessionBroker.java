package org.metalib.papifly.fx.login.core;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.metalib.papifly.fx.login.api.AuthSession;
import org.metalib.papifly.fx.login.api.AuthSessionBroker;
import org.metalib.papifly.fx.login.api.AuthState;
import org.metalib.papifly.fx.login.api.UserPrincipal;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class DefaultAuthSessionBroker implements AuthSessionBroker {

    private final Map<String, AuthSession> sessions = new LinkedHashMap<>();
    private final SimpleObjectProperty<AuthState> authState = new SimpleObjectProperty<>(AuthState.UNAUTHENTICATED);
    private final SimpleObjectProperty<AuthSession> activeSession = new SimpleObjectProperty<>();

    @Override
    public synchronized CompletableFuture<AuthSession> signIn(String providerId) {
        authState.set(AuthState.INITIATING_AUTH);
        AuthSession session = sessions.values().stream()
            .filter(candidate -> providerId.equals(candidate.providerId()))
            .findFirst()
            .orElseGet(() -> createSession(providerId, providerId + "-user"));
        upsertSession(session);
        return CompletableFuture.completedFuture(session);
    }

    @Override
    public synchronized CompletableFuture<AuthSession> signInWithDeviceFlow(String providerId) {
        authState.set(AuthState.POLLING_DEVICE);
        return signIn(providerId);
    }

    @Override
    public synchronized CompletableFuture<AuthSession> refresh(boolean force) {
        AuthSession session = activeSession.get();
        if (session == null) {
            authState.set(AuthState.ERROR);
            return CompletableFuture.failedFuture(new IllegalStateException("No active session."));
        }
        authState.set(AuthState.REFRESHING);
        AuthSession refreshed = new AuthSession(
            session.providerId(),
            session.subject(),
            session.principal(),
            session.scopes(),
            session.accessToken(),
            session.refreshTokenRef(),
            Instant.now().plusSeconds(3600),
            session.issuedAt() == null ? Instant.now() : session.issuedAt()
        );
        upsertSession(refreshed);
        return CompletableFuture.completedFuture(refreshed);
    }

    @Override
    public synchronized CompletableFuture<Void> logout(boolean revokeRemote) {
        activeSession.set(null);
        authState.set(AuthState.SIGNED_OUT);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public synchronized Optional<AuthSession> activeSession() {
        return Optional.ofNullable(activeSession.get());
    }

    @Override
    public synchronized List<AuthSession> allSessions() {
        return List.copyOf(sessions.values());
    }

    @Override
    public synchronized void setActiveSession(String providerId, String subject) {
        AuthSession session = sessions.get(key(providerId, subject));
        activeSession.set(session);
        authState.set(session == null ? AuthState.SIGNED_OUT : AuthState.AUTHENTICATED);
    }

    @Override
    public ReadOnlyObjectProperty<AuthState> authStateProperty() {
        return authState;
    }

    @Override
    public ReadOnlyObjectProperty<AuthSession> sessionProperty() {
        return activeSession;
    }

    public synchronized void upsertSession(AuthSession session) {
        sessions.put(key(session.providerId(), session.subject()), session);
        activeSession.set(session);
        authState.set(session.isExpired(Instant.now()) ? AuthState.EXPIRED : AuthState.AUTHENTICATED);
    }

    public synchronized void removeSession(String providerId, String subject) {
        sessions.remove(key(providerId, subject));
        AuthSession session = activeSession.get();
        if (session != null && providerId.equals(session.providerId()) && subject.equals(session.subject())) {
            activeSession.set(null);
            authState.set(AuthState.SIGNED_OUT);
        }
    }

    private AuthSession createSession(String providerId, String subject) {
        Instant issuedAt = Instant.now();
        return new AuthSession(
            providerId,
            subject,
            new UserPrincipal(subject, subject + "@local", ""),
            Set.of("openid"),
            "",
            "",
            issuedAt.plusSeconds(3600),
            issuedAt
        );
    }

    private String key(String providerId, String subject) {
        return providerId + ':' + subject;
    }
}
