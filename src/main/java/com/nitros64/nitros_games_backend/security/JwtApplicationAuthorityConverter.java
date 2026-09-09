package com.nitros64.nitros_games_backend.security;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

public final class JwtApplicationAuthorityConverter
        implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String ADMIN_ROLE = "ADMIN";

    private final String adminScope;

    public JwtApplicationAuthorityConverter(String adminScope) {
        this.adminScope = adminScope;
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<String> roles = new LinkedHashSet<>();
        addKeycloakRealmRoles(jwt, roles);
        addFlatRoles(jwt.getClaim("cognito:groups"), roles);
        if (scopes(jwt.getClaim("scope")).contains(adminScope)) {
            roles.add(ADMIN_ROLE);
        }

        return roles.stream()
                .map(String::strip)
                .filter(role -> !role.isEmpty())
                .map(role -> "ROLE_" + role.toUpperCase(Locale.ROOT))
                .distinct()
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();
    }

    private void addKeycloakRealmRoles(Jwt jwt, Set<String> roles) {
        Object realmAccessClaim = jwt.getClaim("realm_access");
        if (realmAccessClaim instanceof Map<?, ?> realmAccess) {
            addFlatRoles(realmAccess.get("roles"), roles);
        }
    }

    private void addFlatRoles(Object claim, Set<String> roles) {
        if (claim instanceof Collection<?> values) {
            values.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .forEach(roles::add);
        }
    }

    private Set<String> scopes(Object claim) {
        if (claim instanceof String value) {
            Set<String> result = new LinkedHashSet<>();
            for (String scope : value.strip().split("\\s+")) {
                if (!scope.isBlank()) {
                    result.add(scope);
                }
            }
            return result;
        }
        if (claim instanceof Collection<?> values) {
            Set<String> result = new LinkedHashSet<>();
            values.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .forEach(result::add);
            return result;
        }
        return Set.of();
    }
}
