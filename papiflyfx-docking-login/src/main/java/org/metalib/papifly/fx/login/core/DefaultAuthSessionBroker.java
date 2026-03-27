package org.metalib.papifly.fx.login.core;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.metalib.papifly.fx.login.api.AuthSessionBroker;
import org.metalib.papifly.fx.login.idapi.AuthorizationRequest;
import org.metalib.papifly.fx.login.idapi.CodeExchangeRequest;
import org.metalib.papifly.fx.login.idapi.DeviceCodeResponse;
import org.metalib.papifly.fx.login.idapi.IdentityProvider;
import org.metalib.papifly.fx.login.idapi.ProviderConfig;
import org.metalib.papifly.fx.login.idapi.ProviderRegistry;
import org.metalib.papifly.fx.login.idapi.TokenResponse;
import org.metalib.papifly.fx.login.idapi.UserPrincipal;
import org.metalib.papifly.fx.login.idapi.oauth.LoopbackCallbackServer;
import org.metalib.papifly.fx.login.idapi.oauth.OAuthStateStore;
import org.metalib.papifly.fx.login.session.AuthSession;
import org.metalib.papifly.fx.login.session.AuthState;
import org.metalib.papifly.fx.settings.api.SecretKeyNames;
import org.metalib.papifly.fx.settings.api.SecretStore;
import org.metalib.papifly.fx.settings.api.SettingsStorage;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

public class DefaultAuthSessionBroker implements AuthSessionBroker {

    private static final String GOOGLE_PROVIDER_ID = "google";

    private final Map<String, AuthSession> sessions = new LinkedHashMap<>();
    private final Map<String, SessionTokenState> sessionTokens = new LinkedHashMap<>();
    private final SimpleObjectProperty<AuthState> authState = new SimpleObjectProperty<>(AuthState.UNAUTHENTICATED);
    private final SimpleObjectProperty<AuthSession> activeSession = new SimpleObjectProperty<>();
    private final SimpleObjectProperty<DeviceCodeResponse> deviceCode = new SimpleObjectProperty<>();
    private final OAuthStateStore oauthStateStore = new OAuthStateStore();

    private final ProviderRegistry providerRegistry;
    private final ProviderSettingsResolver settingsResolver;
    private final SecretStore secretStore;
    private final BrowserLauncher browserLauncher;
    private final CallbackServerFactory callbackServerFactory;

    public DefaultAuthSessionBroker() {
        this.providerRegistry = null;
        this.settingsResolver = null;
        this.secretStore = null;
        this.browserLauncher = BrowserLauncher.desktop();
        this.callbackServerFactory = CallbackServerFactory.loopback();
    }

    public DefaultAuthSessionBroker(ProviderRegistry providerRegistry, SettingsStorage settingsStorage, SecretStore secretStore) {
        this(providerRegistry, settingsStorage, secretStore, BrowserLauncher.desktop(), CallbackServerFactory.loopback());
    }

    DefaultAuthSessionBroker(
        ProviderRegistry providerRegistry,
        SettingsStorage settingsStorage,
        SecretStore secretStore,
        BrowserLauncher browserLauncher
    ) {
        this(providerRegistry, settingsStorage, secretStore, browserLauncher, CallbackServerFactory.loopback());
    }

    DefaultAuthSessionBroker(
        ProviderRegistry providerRegistry,
        SettingsStorage settingsStorage,
        SecretStore secretStore,
        BrowserLauncher browserLauncher,
        CallbackServerFactory callbackServerFactory
    ) {
        this.providerRegistry = providerRegistry;
        this.settingsResolver = new ProviderSettingsResolver(settingsStorage, secretStore);
        this.secretStore = secretStore;
        this.browserLauncher = browserLauncher;
        this.callbackServerFactory = callbackServerFactory;
    }

    public boolean isConfiguredForOAuth() {
        return providerRegistry != null && settingsResolver != null && secretStore != null;
    }

    @Override
    public synchronized CompletableFuture<AuthSession> signIn(String providerId) {
        deviceCode.set(null);
        if (!isConfiguredForOAuth()) {
            return signInStub(providerId);
        }

        try {
            IdentityProvider provider = provider(providerId);
            ProviderSettingsResolver.ProviderRuntimeConfig runtimeConfig = resolveConfig(providerId, provider);
            if (runtimeConfig.preferDeviceFlow() && provider.descriptor().capabilities().supportsDeviceFlow()) {
                return signInWithDeviceFlow(providerId, provider, runtimeConfig);
            }
            if (!provider.descriptor().capabilities().supportsAuthCodePkce()
                && provider.descriptor().capabilities().supportsDeviceFlow()) {
                return signInWithDeviceFlow(providerId, provider, runtimeConfig);
            }
            return signInWithAuthorizationCode(providerId, provider, runtimeConfig);
        } catch (RuntimeException exception) {
            authState.set(AuthState.ERROR);
            return CompletableFuture.failedFuture(exception);
        }
    }

    @Override
    public synchronized CompletableFuture<AuthSession> signInWithDeviceFlow(String providerId) {
        deviceCode.set(null);
        if (!isConfiguredForOAuth()) {
            authState.set(AuthState.POLLING_DEVICE);
            return signInStub(providerId);
        }

        try {
            IdentityProvider provider = provider(providerId);
            ProviderSettingsResolver.ProviderRuntimeConfig runtimeConfig = resolveConfig(providerId, provider);
            return signInWithDeviceFlow(providerId, provider, runtimeConfig);
        } catch (RuntimeException exception) {
            authState.set(AuthState.ERROR);
            return CompletableFuture.failedFuture(exception);
        }
    }

    @Override
    public synchronized CompletableFuture<AuthSession> refresh(boolean force) {
        if (!isConfiguredForOAuth()) {
            return refreshStub();
        }

        AuthSession session = activeSession.get();
        if (session == null) {
            authState.set(AuthState.ERROR);
            return CompletableFuture.failedFuture(new IllegalStateException("No active session."));
        }

        SessionTokenState tokenState = sessionTokens.get(key(session.providerId(), session.subject()));
        String refreshToken = tokenState != null && tokenState.refreshToken() != null
            ? tokenState.refreshToken()
            : secretStore.getSecret(SecretKeyNames.oauthRefreshToken(session.providerId(), session.subject())).orElse(null);
        if ((refreshToken == null || refreshToken.isBlank()) && !force && !session.isExpired(Instant.now())) {
            return CompletableFuture.completedFuture(session);
        }
        if ((refreshToken == null || refreshToken.isBlank()) && !session.isExpired(Instant.now())) {
            return CompletableFuture.completedFuture(session);
        }
        if (refreshToken == null || refreshToken.isBlank()) {
            authState.set(AuthState.EXPIRED);
            return CompletableFuture.failedFuture(new IllegalStateException("No refresh token is available. Sign in again."));
        }

        try {
            IdentityProvider provider = provider(session.providerId());
            ProviderSettingsResolver.ProviderRuntimeConfig runtimeConfig = resolveConfig(session.providerId(), provider);
            authState.set(AuthState.REFRESHING);

            return provider.refreshToken(runtimeConfig.config(), refreshToken)
                .handle((tokenResponse, error) -> {
                    if (error != null) {
                        if (!session.isExpired(Instant.now())) {
                            synchronized (this) {
                                authState.set(AuthState.AUTHENTICATED);
                            }
                            return CompletableFuture.completedFuture(session);
                        }
                        synchronized (this) {
                            authState.set(AuthState.ERROR);
                        }
                        return CompletableFuture.<AuthSession>failedFuture(rootCause(error));
                    }
                    String accessToken = tokenResponse.accessToken();
                    if (accessToken == null || accessToken.isBlank()) {
                        return CompletableFuture.completedFuture(session);
                    }
                    return provider.fetchUserPrincipal(runtimeConfig.config(), accessToken)
                        .thenApply(principal -> persistAuthenticatedSession(
                            session.providerId(),
                            runtimeConfig,
                            tokenResponse,
                            principal,
                            session
                        ));
                })
                .thenCompose(future -> future);
        } catch (RuntimeException exception) {
            authState.set(AuthState.ERROR);
            return CompletableFuture.failedFuture(exception);
        }
    }

    @Override
    public synchronized CompletableFuture<Void> logout(boolean revokeRemote) {
        deviceCode.set(null);
        if (!isConfiguredForOAuth()) {
            activeSession.set(null);
            authState.set(AuthState.SIGNED_OUT);
            return CompletableFuture.completedFuture(null);
        }

        AuthSession session = activeSession.get();
        if (session == null) {
            authState.set(AuthState.SIGNED_OUT);
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<Void> revokeFuture = CompletableFuture.completedFuture(null);
        if (revokeRemote) {
            SessionTokenState tokenState = sessionTokens.get(key(session.providerId(), session.subject()));
            String token = tokenState != null && tokenState.accessToken() != null
                ? tokenState.accessToken()
                : secretStore.getSecret(SecretKeyNames.oauthRefreshToken(session.providerId(), session.subject())).orElse(null);
            if (token != null && !token.isBlank()) {
                try {
                    IdentityProvider provider = provider(session.providerId());
                    ProviderSettingsResolver.ProviderRuntimeConfig runtimeConfig = resolveConfig(session.providerId(), provider);
                    revokeFuture = provider.revokeToken(runtimeConfig.config(), token);
                } catch (RuntimeException exception) {
                    revokeFuture = CompletableFuture.failedFuture(exception);
                }
            }
        }

        return revokeFuture.handle((ignored, error) -> {
            synchronized (this) {
                activeSession.set(null);
                authState.set(AuthState.SIGNED_OUT);
                if (revokeRemote) {
                    secretStore.clearSecret(SecretKeyNames.oauthRefreshToken(session.providerId(), session.subject()));
                    removeSession(session.providerId(), session.subject());
                }
            }
            if (error != null) {
                throw new CompletionException(rootCause(error));
            }
            return null;
        });
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

    @Override
    public ReadOnlyObjectProperty<DeviceCodeResponse> deviceCodeProperty() {
        return deviceCode;
    }

    public synchronized void upsertSession(AuthSession session) {
        sessions.put(key(session.providerId(), session.subject()), session);
        activeSession.set(session);
        deviceCode.set(null);
        authState.set(session.isExpired(Instant.now()) ? AuthState.EXPIRED : AuthState.AUTHENTICATED);
    }

    public synchronized void removeSession(String providerId, String subject) {
        sessions.remove(key(providerId, subject));
        sessionTokens.remove(key(providerId, subject));
        AuthSession session = activeSession.get();
        if (session != null && providerId.equals(session.providerId()) && subject.equals(session.subject())) {
            activeSession.set(null);
            deviceCode.set(null);
            authState.set(AuthState.SIGNED_OUT);
        }
    }

    private CompletableFuture<AuthSession> signInWithAuthorizationCode(
        String providerId,
        IdentityProvider provider,
        ProviderSettingsResolver.ProviderRuntimeConfig runtimeConfig
    ) {
        return signInWithAuthorizationCode(providerId, provider, runtimeConfig, false);
    }

    private CompletableFuture<AuthSession> signInWithAuthorizationCode(
        String providerId,
        IdentityProvider provider,
        ProviderSettingsResolver.ProviderRuntimeConfig runtimeConfig,
        boolean googleConsentRetry
    ) {
        authState.set(AuthState.INITIATING_AUTH);
        final CallbackServerHandle callbackServer;
        try {
            callbackServer = callbackServerFactory.start();
        } catch (IOException exception) {
            authState.set(AuthState.ERROR);
            return CompletableFuture.failedFuture(new IllegalStateException("Unable to start the local OAuth callback server.", exception));
        }

        final AuthorizationRequest request;
        try {
            ProviderSettingsResolver.ProviderRuntimeConfig attemptConfig = authorizationAttemptConfig(
                providerId,
                runtimeConfig,
                googleConsentRetry
            );
            request = provider.buildAuthorizationRequest(attemptConfig.config(), callbackServer.redirectUri());
            oauthStateStore.store(request.state(), request.nonce(), request.codeVerifier(), request.redirectUri());
            browserLauncher.open(URI.create(request.authUrl()));
        } catch (Exception exception) {
            callbackServer.close();
            authState.set(AuthState.ERROR);
            return CompletableFuture.failedFuture(new IllegalStateException(
                "Unable to open the system browser. Retry after fixing desktop browser integration.",
                exception
            ));
        }

        authState.set(AuthState.AWAITING_CALLBACK);
        return callbackServer.callbackParams()
            .thenCompose(params -> exchangeAuthorizationCode(providerId, provider, runtimeConfig, params, googleConsentRetry))
            .whenComplete((session, error) -> {
                callbackServer.close();
                if (error != null) {
                    synchronized (this) {
                        authState.set(AuthState.ERROR);
                    }
                }
            });
    }

    private CompletableFuture<AuthSession> signInWithDeviceFlow(
        String providerId,
        IdentityProvider provider,
        ProviderSettingsResolver.ProviderRuntimeConfig runtimeConfig
    ) {
        authState.set(AuthState.INITIATING_AUTH);
        return provider.requestDeviceCode(runtimeConfig.config())
            .thenCompose(deviceCode -> {
                synchronized (this) {
                    this.deviceCode.set(deviceCode);
                }
                openDeviceVerification(deviceCode);
                authState.set(AuthState.POLLING_DEVICE);
                return pollForDeviceToken(provider, runtimeConfig.config(), deviceCode);
            })
            .thenCompose(tokenResponse -> completeAuthentication(
                providerId,
                provider,
                runtimeConfig,
                tokenResponse,
                null,
                false
            ))
            .whenComplete((session, error) -> {
                if (error != null) {
                    synchronized (this) {
                        deviceCode.set(null);
                        authState.set(AuthState.ERROR);
                    }
                }
            });
    }

    private CompletableFuture<AuthSession> exchangeAuthorizationCode(
        String providerId,
        IdentityProvider provider,
        ProviderSettingsResolver.ProviderRuntimeConfig runtimeConfig,
        Map<String, String> params,
        boolean googleConsentRetry
    ) {
        String error = params.get("error");
        if (error != null && !error.isBlank()) {
            String description = params.getOrDefault("error_description", error);
            return CompletableFuture.failedFuture(new IllegalStateException(description));
        }

        String state = params.get("state");
        if (state == null || state.isBlank()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Missing OAuth state in callback."));
        }

        OAuthStateStore.StateEntry stateEntry = oauthStateStore.consume(state)
            .orElseThrow(() -> new IllegalStateException("The OAuth callback state did not match the sign-in request."));

        String code = params.get("code");
        if (code == null || code.isBlank()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Missing authorization code in callback."));
        }

        authState.set(AuthState.EXCHANGING_CODE);
        CodeExchangeRequest request = new CodeExchangeRequest(
            code,
            stateEntry.codeVerifier(),
            stateEntry.redirectUri(),
            state
        );

        ProviderSettingsResolver.ProviderRuntimeConfig attemptConfig = authorizationAttemptConfig(
            providerId,
            runtimeConfig,
            googleConsentRetry
        );
        return provider.exchangeCode(attemptConfig.config(), request)
            .thenCompose(tokenResponse -> completeAuthentication(
                providerId,
                provider,
                attemptConfig,
                tokenResponse,
                null,
                googleConsentRetry
            ));
    }

    private CompletableFuture<AuthSession> completeAuthentication(
        String providerId,
        IdentityProvider provider,
        ProviderSettingsResolver.ProviderRuntimeConfig runtimeConfig,
        TokenResponse tokenResponse,
        AuthSession previousSession,
        boolean googleConsentRetry
    ) {
        if (tokenResponse.accessToken() == null || tokenResponse.accessToken().isBlank()) {
            return CompletableFuture.failedFuture(new IllegalStateException("The provider did not return an access token."));
        }
        return provider.fetchUserPrincipal(runtimeConfig.config(), tokenResponse.accessToken())
            .thenCompose(principal -> {
                if (shouldRetryGoogleWithConsent(providerId, tokenResponse, principal)) {
                    if (googleConsentRetry) {
                        return CompletableFuture.failedFuture(googleOfflineAccessError());
                    }
                    return signInWithAuthorizationCode(providerId, provider, runtimeConfig, true);
                }
                return CompletableFuture.completedFuture(
                    persistAuthenticatedSession(providerId, runtimeConfig, tokenResponse, principal, previousSession)
                );
            });
    }

    private CompletableFuture<TokenResponse> pollForDeviceToken(
        IdentityProvider provider,
        ProviderConfig config,
        DeviceCodeResponse deviceCode
    ) {
        long intervalSeconds = deviceCode.interval() > 0 ? deviceCode.interval() : 5;
        Instant deadline = Instant.now().plusSeconds(Math.max(deviceCode.expiresIn(), intervalSeconds));
        return pollForDeviceToken(provider, config, deviceCode.deviceCode(), intervalSeconds, deadline);
    }

    private CompletableFuture<TokenResponse> pollForDeviceToken(
        IdentityProvider provider,
        ProviderConfig config,
        String deviceCode,
        long intervalSeconds,
        Instant deadline
    ) {
        return provider.pollDeviceToken(config, deviceCode)
            .thenCompose(tokenResponse -> {
                if (tokenResponse.accessToken() != null && !tokenResponse.accessToken().isBlank()) {
                    return CompletableFuture.completedFuture(tokenResponse);
                }
                if (!Instant.now().isBefore(deadline)) {
                    return CompletableFuture.failedFuture(new IllegalStateException("Device authorization expired before it was approved."));
                }
                return CompletableFuture.runAsync(
                    () -> { },
                    CompletableFuture.delayedExecutor(intervalSeconds, TimeUnit.SECONDS)
                ).thenCompose(ignored -> pollForDeviceToken(provider, config, deviceCode, intervalSeconds, deadline));
            });
    }

    private void openDeviceVerification(DeviceCodeResponse deviceCode) {
        String uri = deviceCode.verificationUriComplete() != null && !deviceCode.verificationUriComplete().isBlank()
            ? deviceCode.verificationUriComplete()
            : deviceCode.verificationUri();
        if (uri == null || uri.isBlank() || deviceCode.userCode() == null || deviceCode.userCode().isBlank()) {
            throw new IllegalStateException("The provider did not return a valid device verification URL and user code.");
        }
        try {
            browserLauncher.open(URI.create(uri));
        } catch (Exception exception) {
            // Device flow can continue without automatic browser launch because the UI shows the URL and user code.
        }
    }

    private synchronized AuthSession persistAuthenticatedSession(
        String providerId,
        ProviderSettingsResolver.ProviderRuntimeConfig runtimeConfig,
        TokenResponse tokenResponse,
        UserPrincipal principal,
        AuthSession previousSession
    ) {
        validatePrincipal(providerId, runtimeConfig, principal);

        Instant now = Instant.now();
        Instant expiresAt = tokenResponse.expiresIn() > 0
            ? now.plusSeconds(tokenResponse.expiresIn())
            : (previousSession != null ? previousSession.expiresAt() : null);
        Set<String> scopes = tokenResponse.scope() != null && !tokenResponse.scope().isBlank()
            ? Set.copyOf(List.of(tokenResponse.scope().trim().split("\\s+")))
            : Set.copyOf(runtimeConfig.config().scopes());

        AuthSession session = new AuthSession(
            previousSession != null ? previousSession.sessionId() : UUID.randomUUID().toString(),
            providerId,
            principal.subject(),
            principal,
            AuthState.AUTHENTICATED,
            previousSession != null ? previousSession.createdAt() : now,
            expiresAt,
            scopes
        );

        String refreshToken = tokenResponse.refreshToken();
        if (refreshToken != null && !refreshToken.isBlank()) {
            secretStore.setSecret(SecretKeyNames.oauthRefreshToken(providerId, principal.subject()), refreshToken);
        } else {
            refreshToken = secretStore.getSecret(SecretKeyNames.oauthRefreshToken(providerId, principal.subject())).orElse(null);
        }
        sessionTokens.put(key(providerId, principal.subject()), new SessionTokenState(
            tokenResponse.accessToken(),
            refreshToken,
            tokenResponse.tokenType(),
            tokenResponse.idToken(),
            runtimeConfig.config()
        ));
        upsertSession(session);
        return session;
    }

    private void validatePrincipal(
        String providerId,
        ProviderSettingsResolver.ProviderRuntimeConfig runtimeConfig,
        UserPrincipal principal
    ) {
        if (!"google".equals(providerId) || runtimeConfig.workspaceDomain() == null) {
            return;
        }
        String email = principal.email();
        String expectedDomain = runtimeConfig.workspaceDomain().toLowerCase(Locale.ROOT);
        if (email == null || !email.toLowerCase(Locale.ROOT).endsWith("@" + expectedDomain)) {
            throw new IllegalStateException("The signed-in Google account must belong to " + runtimeConfig.workspaceDomain() + '.');
        }
    }

    private IdentityProvider provider(String providerId) {
        return providerRegistry.get(providerId)
            .orElseThrow(() -> new IllegalStateException("No login provider is registered for '" + providerId + "'."));
    }

    private ProviderSettingsResolver.ProviderRuntimeConfig resolveConfig(String providerId, IdentityProvider provider) {
        try {
            return settingsResolver.resolve(providerId, provider.descriptor());
        } catch (RuntimeException exception) {
            authState.set(AuthState.ERROR);
            throw exception;
        }
    }

    private CompletableFuture<AuthSession> signInStub(String providerId) {
        authState.set(AuthState.INITIATING_AUTH);
        AuthSession session = sessions.values().stream()
            .filter(candidate -> providerId.equals(candidate.providerId()))
            .findFirst()
            .orElseGet(() -> createStubSession(providerId, providerId + "-user"));
        upsertSession(session);
        return CompletableFuture.completedFuture(session);
    }

    private CompletableFuture<AuthSession> refreshStub() {
        AuthSession session = activeSession.get();
        if (session == null) {
            authState.set(AuthState.ERROR);
            return CompletableFuture.failedFuture(new IllegalStateException("No active session."));
        }
        authState.set(AuthState.REFRESHING);
        AuthSession refreshed = new AuthSession(
            session.sessionId(),
            session.providerId(),
            session.subject(),
            session.principal(),
            AuthState.AUTHENTICATED,
            session.createdAt(),
            Instant.now().plusSeconds(3600),
            session.scopes()
        );
        upsertSession(refreshed);
        return CompletableFuture.completedFuture(refreshed);
    }

    private AuthSession createStubSession(String providerId, String subject) {
        Instant now = Instant.now();
        return new AuthSession(
            UUID.randomUUID().toString(),
            providerId,
            subject,
            new UserPrincipal(subject, subject, subject + "@local", ""),
            AuthState.AUTHENTICATED,
            now,
            now.plusSeconds(3600),
            Set.of("openid")
        );
    }

    private Throwable rootCause(Throwable error) {
        Throwable current = error;
        while (current instanceof CompletionException && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private String key(String providerId, String subject) {
        return providerId + ':' + subject;
    }

    private ProviderSettingsResolver.ProviderRuntimeConfig authorizationAttemptConfig(
        String providerId,
        ProviderSettingsResolver.ProviderRuntimeConfig runtimeConfig,
        boolean googleConsentRetry
    ) {
        if (!googleConsentRetry || !GOOGLE_PROVIDER_ID.equals(providerId)) {
            return runtimeConfig;
        }
        Map<String, String> authorizationParameters = new LinkedHashMap<>(runtimeConfig.config().authorizationParameters());
        authorizationParameters.put("prompt", "consent");
        ProviderConfig config = runtimeConfig.config().withAuthorizationParameters(authorizationParameters);
        return new ProviderSettingsResolver.ProviderRuntimeConfig(
            config,
            runtimeConfig.workspaceDomain(),
            runtimeConfig.preferDeviceFlow()
        );
    }

    private boolean shouldRetryGoogleWithConsent(String providerId, TokenResponse tokenResponse, UserPrincipal principal) {
        if (!GOOGLE_PROVIDER_ID.equals(providerId)) {
            return false;
        }
        if (tokenResponse.refreshToken() != null && !tokenResponse.refreshToken().isBlank()) {
            return false;
        }
        return secretStore.getSecret(SecretKeyNames.oauthRefreshToken(providerId, principal.subject()))
            .map(String::isBlank)
            .orElse(true);
    }

    private IllegalStateException googleOfflineAccessError() {
        return new IllegalStateException(
            "Google did not grant offline access. The session cannot be refreshed without a refresh token."
        );
    }

    @FunctionalInterface
    interface BrowserLauncher {

        void open(URI uri) throws Exception;

        static BrowserLauncher desktop() {
            return uri -> {
                if (!Desktop.isDesktopSupported()) {
                    throw new IllegalStateException("Desktop browser integration is not available on this platform.");
                }
                Desktop desktop = Desktop.getDesktop();
                if (!desktop.isSupported(Desktop.Action.BROWSE)) {
                    throw new IllegalStateException("Desktop browser integration is not available on this platform.");
                }
                desktop.browse(uri);
            };
        }
    }

    @FunctionalInterface
    interface CallbackServerFactory {

        CallbackServerHandle start() throws IOException;

        static CallbackServerFactory loopback() {
            return () -> {
                LoopbackCallbackServer server = LoopbackCallbackServer.start();
                return new CallbackServerHandle() {
                    @Override
                    public String redirectUri() {
                        return server.getRedirectUri();
                    }

                    @Override
                    public CompletableFuture<Map<String, String>> callbackParams() {
                        return server.getCallbackParams();
                    }

                    @Override
                    public void close() {
                        server.close();
                    }
                };
            };
        }
    }

    interface CallbackServerHandle extends AutoCloseable {

        String redirectUri();

        CompletableFuture<Map<String, String>> callbackParams();

        @Override
        void close();
    }

    private record SessionTokenState(
        String accessToken,
        String refreshToken,
        String tokenType,
        String idToken,
        ProviderConfig config
    ) {
    }
}
