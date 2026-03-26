package org.metalib.papifly.fx.login.idapi;

import java.util.List;

public record ProviderConfig(
    String clientId,
    String clientSecret,
    List<String> scopes,
    String discoveryUrl,
    String authorizationEndpoint,
    String tokenEndpoint,
    String userInfoEndpoint,
    String enterpriseUrl
) {

    public ProviderConfig {
        scopes = scopes == null ? List.of() : List.copyOf(scopes);
    }

    public static ProviderConfig ofClientId(String clientId) {
        return new ProviderConfig(clientId, null, List.of(), null, null, null, null, null);
    }

    public static ProviderConfig ofClientIdAndScopes(String clientId, List<String> scopes) {
        return new ProviderConfig(clientId, null, scopes, null, null, null, null, null);
    }
}
