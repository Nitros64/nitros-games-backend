package com.nitros64.nitros_games_backend.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

public final class JwtTestSupport {

    private JwtTestSupport() {
    }

    public static RequestPostProcessor adminJwt() {
        return jwt()
                .jwt(token -> token
                        .subject("test-admin-id")
                        .claim("preferred_username", "test-admin"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    public static RequestPostProcessor userJwt() {
        return jwt()
                .jwt(token -> token
                        .subject("test-user-id")
                        .claim("preferred_username", "catalog-reader"))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }
}
