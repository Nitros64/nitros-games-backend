package com.nitros64.nitros_games_backend.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.nitros64.nitros_games_backend.security.SecurityConfiguration;
import com.nitros64.nitros_games_backend.security.SecurityProperties;
import com.nitros64.nitros_games_backend.security.JwtApplicationAuthorityConverter;
import com.nitros64.nitros_games_backend.security.JwtIdentityProperties;
import com.nitros64.nitros_games_backend.security.JwtTrustValidator;

class SecurityModuleStructureTests {

    private static final String SECURITY_PACKAGE =
            "com.nitros64.nitros_games_backend.security";

    @Test
    void securityComponentsStayInsideTheirModule() {
        assertThat(List.of(
                SecurityConfiguration.class,
                SecurityProperties.class,
                JwtIdentityProperties.class,
                JwtApplicationAuthorityConverter.class,
                JwtTrustValidator.class))
                .allSatisfy(type -> assertThat(type.getPackageName())
                        .startsWith(SECURITY_PACKAGE));
    }
}
