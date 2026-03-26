package org.metalib.papifly.fx.login.idapi;

import java.util.Map;

public record UserPrincipal(
    String subject,
    String displayName,
    String email,
    String avatarUrl,
    Map<String, Object> rawClaims
) {

    public UserPrincipal {
        rawClaims = rawClaims == null ? Map.of() : Map.copyOf(rawClaims);
    }

    public UserPrincipal(String subject, String displayName, String email, String avatarUrl) {
        this(subject, displayName, email, avatarUrl, Map.of());
    }
}
