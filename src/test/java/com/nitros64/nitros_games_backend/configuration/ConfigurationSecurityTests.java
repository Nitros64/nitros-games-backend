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
        assertEquals("${DB_PASSWORD}", local.getProperty("spring.datasource.password"));
        assertEquals("${DB_PASSWORD}", production.getProperty("spring.datasource.password"));
    }

    @Test
    void productionRequiresExternalDatabaseCredentials() throws IOException {
        Properties properties = loadProperties("application-prod.properties");

        assertEquals("${DB_URL}", properties.getProperty("spring.datasource.url"));
        assertEquals("${DB_USERNAME}", properties.getProperty("spring.datasource.username"));
        assertEquals("${DB_PASSWORD}", properties.getProperty("spring.datasource.password"));
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
    }

    private Properties loadProperties(String resourceName) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = new ClassPathResource(resourceName).getInputStream()) {
            properties.load(input);
        }
        return properties;
    }

}
