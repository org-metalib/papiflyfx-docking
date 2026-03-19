package org.metalib.papifly.fx.login.api;

public record UserPrincipal(
    String displayName,
    String email,
    String avatarUrl
) {
}
