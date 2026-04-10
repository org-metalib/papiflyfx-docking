
### Review Note 1

papiflyfx-docking-login/src/main/java/org/metalib/papifly/fx/login/core/TokenManager.java
```java
    String revokeToken(AuthSession session) {
        DefaultAuthSessionBroker.SessionTokenState tokenState = sessionTokens.get(key(session.providerId(), session.subject()));
        if (tokenState != null && tokenState.accessToken() != null) {
            return tokenState.accessToken();
        }
        return secretStore.getSecret(SecretKeyNames.oauthRefreshToken(session.providerId(), session.subject())).orElse(null);
    }
```

TokenManager.revokeToken() falls back to reading the refresh token from SecretStore when the in-memory access token is missing. That means remote revocation may send a refresh token to providers like GitHub that expect an access token, leaving the access token unrevoked. Consider persisting the access token separately (if acceptable) or changing the fallback behavior to skip remote revocation when no access token is available (or use a provider-specific token selector).

### Review Note 2

papiflyfx-docking-login/src/main/java/org/metalib/papifly/fx/login/settings/AuthenticationCategory.java
```java
    public AuthenticationCategory() {
        this(LoginRuntime.createDefault());
    }

    AuthenticationCategory(LoginRuntime runtime) {
        this(runtime::broker);
    }

    AuthenticationCategory(Supplier<AuthSessionBroker> brokerSupplier) {
        this.brokerSupplier = brokerSupplier;
    }
```

AuthenticationCategory’s default constructor creates a new LoginRuntime (and therefore its own broker) instead of reusing the application’s login runtime. This can desynchronize the settings UI from the rest of the app’s auth state. Consider requiring injection (via constructor/ServiceLoader wiring) or sourcing the broker from a shared runtime owned by the host application.

### Review Note 3

papiflyfx-docking-login/src/main/java/org/metalib/papifly/fx/login/docking/LoginStateAdapter.java

```java
    public LoginStateAdapter() {
        this(new LoginFactory());
    }
```

Reuse the app login runtime when restoring login content

The default LoginStateAdapter now constructs its own LoginFactory, which in turn builds a fresh LoginRuntime/broker instance. In layouts restored via ContentStateRegistry.fromServiceLoader(), restored login panes are therefore bound to a different broker than panes created from the app’s main LoginFactory, so sign-in state and session changes stop propagating between them. This regression is introduced by the no-arg adapter path and breaks multi-pane/session-restore consistency.

