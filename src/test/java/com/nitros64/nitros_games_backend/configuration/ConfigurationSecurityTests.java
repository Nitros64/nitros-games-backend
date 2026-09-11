package com.nitros64.nitros_games_backend.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.env.Profiles;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ConfigurationSecurityTests {

    @Test
    void runtimeConfigurationDoesNotContainLiteralDatabasePasswords() throws IOException {
        Properties common = loadProperties("application.properties");
        Properties local = loadProperties("application-local.properties");
        Properties runtime = loadProperties("application-runtime.properties");
        Properties production = loadProperties("application-prod.properties");

        assertNull(common.getProperty("spring.datasource.password"));
        assertEquals("${DB_PASSWORD}", local.getProperty("spring.datasource.password"));
        assertEquals("${DB_PASSWORD}", runtime.getProperty("spring.datasource.password"));
        assertNull(production.getProperty("spring.datasource.password"));
        assertNull(local.getProperty("app.security.admin-password"));
        assertNull(runtime.getProperty("app.security.admin-password"));
        assertNull(production.getProperty("app.security.admin-password"));
    }

    @Test
    void deployedRuntimeRequiresExternalDatabaseCredentials() throws IOException {
        Properties properties = loadProperties("application-runtime.properties");

        assertEquals("${DB_URL}", properties.getProperty("spring.datasource.url"));
        assertEquals("${DB_USERNAME}", properties.getProperty("spring.datasource.username"));
        assertEquals("${DB_PASSWORD}", properties.getProperty("spring.datasource.password"));
        assertEquals(
                "${APP_STORAGE_HOST_IMAGES_BACKEND:filesystem}",
                properties.getProperty("app.storage.host-images.backend"));
        assertEquals(
                "${APP_STORAGE_HOST_IMAGES_DIRECTORY:uploadImageFileHost}",
                properties.getProperty("app.storage.host-images.directory"));
        assertEquals(
                "${APP_STORAGE_HOST_IMAGES_S3_BUCKET:}",
                properties.getProperty("app.storage.host-images.s3.bucket"));
        assertEquals(
                "${AWS_REGION:}",
                properties.getProperty("app.storage.host-images.s3.region"));
        assertEquals(
                "${APP_STORAGE_HOST_IMAGES_S3_PREFIX:host-images/}",
                properties.getProperty("app.storage.host-images.s3.prefix"));
        assertEquals(
                "${APP_SECURITY_ALLOWED_ORIGINS}",
                properties.getProperty("app.security.allowed-origins"));
        assertEquals(
                "${OAUTH2_ISSUER_URI}",
                properties.getProperty("app.security.jwt.issuer-uri"));
        assertEquals(
                "${OAUTH2_JWK_SET_URI}",
                properties.getProperty("app.security.jwt.jwk-set-uri"));
        assertEquals(
                "${OAUTH2_RESOURCE_ID}",
                properties.getProperty("app.security.jwt.resource-id"));
        assertEquals(
                "${OAUTH2_ACCESS_SCOPE}",
                properties.getProperty("app.security.jwt.access-scope"));
        assertEquals(
                "${OAUTH2_ADMIN_SCOPE}",
                properties.getProperty("app.security.jwt.admin-scope"));
        assertEquals(
                "${OAUTH2_ALLOWED_CLIENT_IDS}",
                properties.getProperty("app.security.jwt.allowed-client-ids"));
    }

    @Test
    void deployedProfilesShareRuntimeConfigurationAndSafeJpaSettings() throws IOException {
        Properties common = loadProperties("application.properties");
        Properties runtime = loadProperties("application-runtime.properties");

        assertEquals("runtime", common.getProperty("spring.profiles.group.prod"));
        assertEquals("runtime", common.getProperty("spring.profiles.group.staging"));
        assertEquals("validate", common.getProperty("spring.jpa.hibernate.ddl-auto"));
        assertEquals("false", common.getProperty("spring.jpa.open-in-view"));
        assertEquals("false", common.getProperty("spring.jpa.show-sql"));
        assertEquals(
                "false",
                common.getProperty("spring.jpa.properties.hibernate.enable_lazy_load_no_trans"));
        assertEquals("graceful", runtime.getProperty("server.shutdown"));
        assertEquals(
                "25s",
                runtime.getProperty("spring.lifecycle.timeout-per-shutdown-phase"));
    }

    @Test
    void prodAndStagingActivateTheSharedRuntimeProfile() {
        assertRuntimeProfileActivated("prod");
        assertRuntimeProfileActivated("staging");
    }

    @Test
    void productionUsesStructuredLogsAndRequestLatencyHistograms() throws IOException {
        Properties common = loadProperties("application.properties");
        Properties runtime = loadProperties("application-runtime.properties");

        assertEquals(
                "health,prometheus",
                common.getProperty("management.endpoints.web.exposure.include"));
        assertEquals(
                "logstash",
                runtime.getProperty("logging.structured.format.console"));
        assertEquals(
                "true",
                runtime.getProperty(
                        "management.metrics.distribution.percentiles-histogram.http.server.requests"));
    }

    private Properties loadProperties(String resourceName) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = new ClassPathResource(resourceName).getInputStream()) {
            properties.load(input);
        }
        return properties;
    }

    private void assertRuntimeProfileActivated(String deployedProfile) {
        new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withPropertyValues("spring.profiles.active=" + deployedProfile)
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    assertEquals(
                            true,
                            context.getEnvironment().acceptsProfiles(Profiles.of("runtime")));
                    assertEquals("graceful", context.getEnvironment().getProperty("server.shutdown"));
                });
    }

}
