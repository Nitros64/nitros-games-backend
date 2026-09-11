package com.nitros64.nitros_games_backend.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

class ProductionJdbcTlsPolicyProfileTests {

    private static final String VALID_RDS_URL =
            "jdbc:mysql://database.example.eu-west-1.rds.amazonaws.com:3306/nitrosgames"
                    + "?sslMode=VERIFY_IDENTITY";
    private static final String DOCKER_MYSQL_URL =
            "jdbc:mysql://mysql:3306/nitrosgames?sslMode=DISABLED&allowPublicKeyRetrieval=true";

    @Test
    void prodActivatesPolicyAndAcceptsVerifiedRdsUrl() {
        try (AnnotationConfigApplicationContext context = context("prod", VALID_RDS_URL)) {
            context.refresh();

            assertThat(context.getBeansOfType(ProductionJdbcTlsPolicy.class)).hasSize(1);
        }
    }

    @Test
    void stagingDoesNotActivateProductionPolicyForDockerMysql() {
        try (AnnotationConfigApplicationContext context = context("staging", DOCKER_MYSQL_URL)) {
            context.refresh();

            assertThat(context.getBeansOfType(ProductionJdbcTlsPolicy.class)).isEmpty();
        }
    }

    @Test
    void prodRejectsDockerMysqlHostname() {
        assertProdStartupRejected(DOCKER_MYSQL_URL);
    }

    @Test
    void prodRejectsDisabledTlsForRds() {
        assertProdStartupRejected(
                "jdbc:mysql://database.example.eu-west-1.rds.amazonaws.com:3306/nitrosgames"
                        + "?sslMode=DISABLED");
    }

    private void assertProdStartupRejected(String jdbcUrl) {
        try (AnnotationConfigApplicationContext context = context("prod", jdbcUrl)) {
            assertThatThrownBy(context::refresh)
                    .isInstanceOf(BeanCreationException.class)
                    .hasRootCauseInstanceOf(IllegalStateException.class)
                    .hasRootCauseMessage(
                            "Production DB_URL must target an Amazon RDS hostname and use exactly one "
                                    + "sslMode=VERIFY_IDENTITY with the image's system truststore");
        }
    }

    private AnnotationConfigApplicationContext context(String profile, String jdbcUrl) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().setActiveProfiles(profile);
        context.getEnvironment().getPropertySources().addFirst(
                new MapPropertySource("test", Map.of("spring.datasource.url", jdbcUrl)));
        context.register(ProductionJdbcTlsPolicy.class);
        return context;
    }
}
