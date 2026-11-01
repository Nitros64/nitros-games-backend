package com.nitros64.nitros_games_backend.security;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Exercises the real filter chain and configured converter; only decoding is mocked. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BearerAuthorizationTests {

    @Autowired MockMvc mvc;
    @MockitoBean JwtDecoder decoder;

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "admin"})
    void keycloakRealmAdminIsMappedByTheConfiguredConverter(String role) throws Exception {
        when(decoder.decode("local-test-token")).thenReturn(token(Map.of("roles", List.of(role))));
        mvc.perform(get("/api/v1/admin/games").header("Authorization", "Bearer local-test-token"))
                .andExpect(status().isOk());
    }

    @Test
    void authenticatedRealmUserCannotReadAdministrativeResources() throws Exception {
        when(decoder.decode("local-test-token")).thenReturn(token(Map.of("roles", List.of("USER"))));
        mvc.perform(get("/api/v1/admin/games").header("Authorization", "Bearer local-test-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("access_denied"));
    }

    @Test
    void malformedRoleClaimCannotGrantAdmin() throws Exception {
        when(decoder.decode("local-test-token")).thenReturn(token(Map.of("roles", "ADMIN")));
        mvc.perform(get("/api/v1/admin/games").header("Authorization", "Bearer local-test-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectedBearerIsUnauthorizedEvenOnPublicRoutesAndDoesNotLeakDecoderDetails() throws Exception {
        when(decoder.decode("rejected-test-token")).thenThrow(new BadJwtException("private decoder detail"));
        for (String path : List.of("/api/v1/admin/games", "/api/v1/games")) {
            mvc.perform(get(path).header("Authorization", "Bearer rejected-test-token"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.code").value("authentication_required"))
                    .andExpect(content().string(org.hamcrest.Matchers.not(
                            org.hamcrest.Matchers.containsString("private decoder detail"))));
        }
    }

    private Jwt token(Map<String, Object> realmAccess) {
        return Jwt.withTokenValue("local-test-token").header("alg", "RS256")
                .subject("local-test-user").claim("realm_access", realmAccess).build();
    }
}
