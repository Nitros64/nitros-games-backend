package com.nitros64.nitros_games_backend.security;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;

public final class JwtTrustValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error INVALID_TOKEN = new OAuth2Error(
            "invalid_token",
            "The access token is not trusted for this resource",
            null);

    private final OAuth2TokenValidator<Jwt> delegate;

    public JwtTrustValidator(JwtIdentityProperties properties) {
        this.delegate = new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(properties.getIssuerUri()),
                jwt -> validateApplicationClaims(jwt, properties));
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        return delegate.validate(jwt);
    }

    private OAuth2TokenValidatorResult validateApplicationClaims(
            Jwt jwt,
            JwtIdentityProperties properties) {
        if (!isAccessToken(jwt)
                || !comesFromAllowedClient(jwt, properties.getAllowedClientIds())
                || !targetsResource(jwt, properties)) {
            return OAuth2TokenValidatorResult.failure(INVALID_TOKEN);
        }
        return OAuth2TokenValidatorResult.success();
    }

    private boolean isAccessToken(Jwt jwt) {
        Object tokenUse = jwt.getClaim("token_use");
        return tokenUse == null || "access".equals(tokenUse);
    }

    private boolean comesFromAllowedClient(Jwt jwt, List<String> allowedClientIds) {
        Set<String> clientIds = new LinkedHashSet<>();
        addStringClaim(jwt.getClaim("client_id"), clientIds);
        addStringClaim(jwt.getClaim("azp"), clientIds);
        return !clientIds.isEmpty() && allowedClientIds.containsAll(clientIds);
    }

    private void addStringClaim(Object claim, Set<String> values) {
        if (claim instanceof String value && !value.isBlank()) {
            values.add(value);
        }
    }

    private boolean targetsResource(Jwt jwt, JwtIdentityProperties properties) {
        Object audience = jwt.getClaim("aud");
        if (properties.getResourceId().equals(audience)
                || audience instanceof Collection<?> values
                        && values.stream().anyMatch(properties.getResourceId()::equals)) {
            return true;
        }
        Set<String> tokenScopes = scopes(jwt.getClaim("scope"));
        return tokenScopes.contains(properties.getAccessScope())
                || tokenScopes.contains(properties.getAdminScope());
    }

    private Set<String> scopes(Object claim) {
        if (claim instanceof String value) {
            String stripped = value.strip();
            if (stripped.isEmpty()) {
                return Set.of();
            }
            Set<String> result = new LinkedHashSet<>();
            for (String scope : stripped.split("\\s+")) {
                result.add(scope);
            }
            return result;
        }
        if (claim instanceof Collection<?> values) {
            Set<String> result = new LinkedHashSet<>();
            values.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .filter(value -> !value.isBlank())
                    .forEach(result::add);
            return result;
        }
        return Set.of();
    }
}
