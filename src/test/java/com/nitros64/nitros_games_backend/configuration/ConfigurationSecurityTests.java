package com.nitros64.nitros_games_backend.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

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
        Properties production = loadProperties("application-prod.properties");

        assertNull(common.getProperty("spring.datasource.password"));
        assertNull(common.getProperty("app.security.admin-password"));
        assertEquals("${DB_PASSWORD}", local.getProperty("spring.datasource.password"));
        assertEquals("${DB_PASSWORD}", production.getProperty("spring.datasource.password"));
        assertEquals(
                "${APP_SECURITY_ADMIN_PASSWORD}",
                local.getProperty("app.security.admin-password"));
        assertEquals(
                "${APP_SECURITY_ADMIN_PASSWORD}",
                production.getProperty("app.security.admin-password"));
    }

    @Test
    void productionRequiresExternalDatabaseCredentials() throws IOException {
        Properties properties = loadProperties("application-prod.properties");

        assertEquals("${DB_URL}", properties.getProperty("spring.datasource.url"));
        assertEquals("${DB_USERNAME}", properties.getProperty("spring.datasource.username"));
        assertEquals("${DB_PASSWORD}", properties.getProperty("spring.datasource.password"));
        assertEquals(
                "${APP_STORAGE_HOST_IMAGES_DIRECTORY}",
                properties.getProperty("app.storage.host-images.directory"));
        assertEquals(
                "${APP_SECURITY_ADMIN_USERNAME}",
                properties.getProperty("app.security.admin-username"));
        assertEquals(
                "${APP_SECURITY_ALLOWED_ORIGINS}",
                properties.getProperty("app.security.allowed-origins"));
    }

    @Test
    void productionUsesSafeJpaSettings() throws IOException {
        Properties properties = loadProperties("application-prod.properties");

        assertEquals("validate", properties.getProperty("spring.jpa.hibernate.ddl-auto"));
        assertEquals("false", properties.getProperty("spring.jpa.open-in-view"));
        assertEquals("false", properties.getProperty("spring.jpa.show-sql"));
        assertEquals(
                "false",
                properties.getProperty("spring.jpa.properties.hibernate.enable_lazy_load_no_trans"));
        assertEquals("graceful", properties.getProperty("server.shutdown"));
        assertEquals(
                "25s",
                properties.getProperty("spring.lifecycle.timeout-per-shutdown-phase"));
    }

    private Properties loadProperties(String resourceName) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = new ClassPathResource(resourceName).getInputStream()) {
            properties.load(input);
        }
        return properties;
    }

}
