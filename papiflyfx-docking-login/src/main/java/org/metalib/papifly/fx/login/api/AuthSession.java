package org.metalib.papifly.fx.login.api;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public record AuthSession(
    String providerId,
    String subject,
    UserPrincipal principal,
    Set<String> scopes,
    String accessToken,
    String refreshTokenRef,
    Instant expiresAt,
    Instant issuedAt
) {

    public AuthSession {
        scopes = scopes == null ? Set.of() : Collections.unmodifiableSet(new LinkedHashSet<>(scopes));
    }

    public boolean isExpired(Instant now) {
        return expiresAt != null && !expiresAt.isAfter(now);
    }
}
