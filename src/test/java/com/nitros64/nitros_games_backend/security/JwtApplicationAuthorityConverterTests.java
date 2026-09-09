package com.nitros64.nitros_games_backend.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class JwtApplicationAuthorityConverterTests {

    private static final String ADMIN_SCOPE = "https://api.nitrosgames64.com/admin";

    private final JwtApplicationAuthorityConverter converter =
            new JwtApplicationAuthorityConverter(ADMIN_SCOPE);

    @Test
    void keycloakRealmRolesRemainSupported() {
        Jwt jwt = jwt(Map.of("realm_access", Map.of(
                "roles", List.of("ADMIN", "viewer", "ADMIN"))));

        assertThat(converter.convert(jwt))
                .extracting("authority")
                .containsExactly("ROLE_ADMIN", "ROLE_VIEWER");
    }

    @Test
    void cognitoGroupsBecomeApplicationRoles() {
        Jwt jwt = jwt(Map.of("cognito:groups", List.of("ADMIN", "reader")));

        assertThat(converter.convert(jwt))
                .extracting("authority")
                .containsExactly("ROLE_ADMIN", "ROLE_READER");
    }

    @Test
    void exactAdministrativeMachineScopeGrantsAdminRole() {
        Jwt jwt = jwt(Map.of(
                "scope", "https://api.nitrosgames64.com/access " + ADMIN_SCOPE));

        assertThat(converter.convert(jwt))
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void similarOrMalformedClaimsGrantNoApplicationRoles() {
        assertThat(converter.convert(jwt(Map.of(
                "realm_access", Map.of("roles", "ADMIN"),
                "cognito:groups", "ADMIN",
                "scope", ADMIN_SCOPE + "-lookalike"))))
                .isEmpty();
        assertThat(converter.convert(jwt(Map.of(
                "realm_access", Map.of("roles", Arrays.asList(1, null, "  ")),
                "cognito:groups", Arrays.asList(null, 2)))))
                .isEmpty();
    }

    private Jwt jwt(Map<String, Object> claims) {
        return Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject("test-subject")
                .issuedAt(Instant.parse("2026-01-01T00:00:00Z"))
                .expiresAt(Instant.parse("2099-01-01T00:00:00Z"))
                .claims(values -> values.putAll(claims))
                .build();
    }
}
