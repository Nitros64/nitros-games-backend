package com.nitros64.nitros_games_backend.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class KeycloakRealmRoleConverterTests {

    private final KeycloakRealmRoleConverter converter = new KeycloakRealmRoleConverter();

    @Test
    void realmRolesBecomeDistinctSpringRoleAuthorities() {
        var jwt = jwt(Map.of("roles", List.of("ADMIN", "viewer", "ADMIN")));

        assertThat(converter.convert(jwt))
                .extracting("authority")
                .containsExactly("ROLE_ADMIN", "ROLE_VIEWER");
    }

    @Test
    void absentOrMalformedRealmRolesGrantNoAuthorities() {
        assertThat(converter.convert(jwt(null))).isEmpty();
        assertThat(converter.convert(jwt(Map.of("roles", "ADMIN")))).isEmpty();
        assertThat(converter.convert(jwt(Map.of(
                "roles", Arrays.asList(1, null, "  "))))).isEmpty();
    }

    private Jwt jwt(Map<String, Object> realmAccess) {
        var builder = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject("test-subject")
                .issuedAt(Instant.parse("2026-01-01T00:00:00Z"))
                .expiresAt(Instant.parse("2099-01-01T00:00:00Z"));
        if (realmAccess != null) {
            builder.claim("realm_access", realmAccess);
        }
        return builder.build();
    }
}
