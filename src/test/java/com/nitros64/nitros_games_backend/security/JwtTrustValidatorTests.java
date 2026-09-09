package com.nitros64.nitros_games_backend.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class JwtTrustValidatorTests {

    private static final String ISSUER =
            "https://cognito-idp.eu-west-1.amazonaws.com/eu-west-1_test";
    private static final String RESOURCE = "https://api.nitrosgames64.com";
    private static final String ACCESS_SCOPE = RESOURCE + "/access";
    private static final String ADMIN_SCOPE = RESOURCE + "/admin";
    private static final String SPA_CLIENT = "angular-client-id";
    private static final String MACHINE_CLIENT = "automation-client-id";

    private final JwtTrustValidator validator = new JwtTrustValidator(properties());

    @Test
    void acceptsCognitoUserAccessTokenForAllowedClientAndApiScope() {
        Jwt jwt = jwt(Map.of(
                "client_id", SPA_CLIENT,
                "token_use", "access",
                "scope", "openid " + ACCESS_SCOPE));

        assertThat(validator.validate(jwt).hasErrors()).isFalse();
    }

    @Test
    void acceptsMachineAccessTokenForAllowedClientAndAdminScopeWithoutAudience() {
        Jwt jwt = jwt(Map.of(
                "client_id", MACHINE_CLIENT,
                "token_use", "access",
                "scope", ADMIN_SCOPE));

        assertThat(validator.validate(jwt).hasErrors()).isFalse();
    }

    @Test
    void acceptsCurrentKeycloakAccessTokenWithAuthorizedPartyAndAudience() {
        Jwt jwt = jwt(Map.of(
                "azp", "nitros-games-cli",
                "aud", List.of(RESOURCE)));

        assertThat(validator.validate(jwt).hasErrors()).isFalse();
    }

    @Test
    void rejectsWrongIssuer() {
        Jwt jwt = jwt("https://attacker.example", Map.of(
                "client_id", SPA_CLIENT,
                "token_use", "access",
                "scope", ACCESS_SCOPE));

        assertThat(validator.validate(jwt).hasErrors()).isTrue();
    }

    @Test
    void rejectsWrongClientIdentity() {
        Jwt jwt = jwt(Map.of(
                "client_id", "unexpected-client",
                "token_use", "access",
                "scope", ACCESS_SCOPE));

        assertThat(validator.validate(jwt).hasErrors()).isTrue();
    }

    @Test
    void rejectsTokenThatDoesNotTargetApiResource() {
        Jwt jwt = jwt(Map.of(
                "client_id", SPA_CLIENT,
                "token_use", "access",
                "scope", "openid profile",
                "aud", List.of("https://another-api.example")));

        assertThat(validator.validate(jwt).hasErrors()).isTrue();
    }

    @Test
    void rejectsIdTokenEvenWhenItsAudienceLooksLikeAnAllowedClient() {
        Jwt jwt = jwt(Map.of(
                "token_use", "id",
                "aud", List.of(SPA_CLIENT),
                "azp", SPA_CLIENT,
                "scope", ACCESS_SCOPE));

        assertThat(validator.validate(jwt).hasErrors()).isTrue();
    }

    @Test
    void rejectsMalformedClientAndScopeClaims() {
        Jwt jwt = jwt(Map.of(
                "client_id", List.of(SPA_CLIENT),
                "token_use", "access",
                "scope", Map.of("value", ACCESS_SCOPE),
                "aud", Map.of("value", RESOURCE)));

        assertThat(validator.validate(jwt).hasErrors()).isTrue();
    }

    private JwtIdentityProperties properties() {
        JwtIdentityProperties properties = new JwtIdentityProperties();
        properties.setIssuerUri(ISSUER);
        properties.setJwkSetUri(ISSUER + "/.well-known/jwks.json");
        properties.setResourceId(RESOURCE);
        properties.setAccessScope(ACCESS_SCOPE);
        properties.setAdminScope(ADMIN_SCOPE);
        properties.setAllowedClientIds(List.of(SPA_CLIENT, MACHINE_CLIENT, "nitros-games-cli"));
        return properties;
    }

    private Jwt jwt(Map<String, Object> claims) {
        return jwt(ISSUER, claims);
    }

    private Jwt jwt(String issuer, Map<String, Object> claims) {
        Instant now = Instant.now();
        return Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .issuer(issuer)
                .subject("test-subject")
                .issuedAt(now.minusSeconds(30))
                .notBefore(now.minusSeconds(30))
                .expiresAt(now.plusSeconds(300))
                .claims(values -> values.putAll(claims))
                .build();
    }
}
