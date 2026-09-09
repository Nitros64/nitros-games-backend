package com.nitros64.nitros_games_backend.configuration;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class ProductionJdbcTlsPolicy implements InitializingBean {

    private static final String JDBC_PREFIX = "jdbc:mysql://";
    private static final String REQUIRED_SSL_MODE = "VERIFY_IDENTITY";
    private static final List<String> FORBIDDEN_TRUST_STORE_OVERRIDES = List.of(
            "trustcertificatekeystoreurl",
            "trustcertificatekeystorepassword",
            "trustcertificatekeystoretype");

    private final String jdbcUrl;

    public ProductionJdbcTlsPolicy(@Value("${spring.datasource.url}") String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    @Override
    public void afterPropertiesSet() {
        validate(jdbcUrl);
    }

    static void validate(String jdbcUrl) {
        if (jdbcUrl == null || !jdbcUrl.startsWith(JDBC_PREFIX)) {
            throw invalidConfiguration();
        }

        int querySeparator = jdbcUrl.indexOf('?');
        if (querySeparator < 0) {
            throw invalidConfiguration();
        }

        URI target;
        try {
            target = URI.create("mysql://" + jdbcUrl.substring(JDBC_PREFIX.length(), querySeparator));
        } catch (IllegalArgumentException exception) {
            throw invalidConfiguration();
        }
        String host = target.getHost();
        if (host == null
                || !host.toLowerCase(Locale.ROOT).endsWith(".rds.amazonaws.com")) {
            throw invalidConfiguration();
        }

        List<QueryParameter> parameters = Arrays.stream(jdbcUrl.substring(querySeparator + 1).split("&"))
                .filter(parameter -> !parameter.isBlank())
                .map(ProductionJdbcTlsPolicy::parseParameter)
                .toList();

        List<QueryParameter> sslModes = parameters.stream()
                .filter(parameter -> parameter.name().equalsIgnoreCase("sslMode"))
                .toList();
        if (sslModes.size() != 1
                || !sslModes.getFirst().value().equalsIgnoreCase(REQUIRED_SSL_MODE)) {
            throw invalidConfiguration();
        }

        boolean disablesSystemTrust = parameters.stream()
                .anyMatch(parameter -> parameter.name().equalsIgnoreCase("fallbackToSystemTrustStore")
                        && parameter.value().equalsIgnoreCase("false"));
        boolean overridesTrustStore = parameters.stream()
                .map(parameter -> parameter.name().toLowerCase(Locale.ROOT))
                .anyMatch(FORBIDDEN_TRUST_STORE_OVERRIDES::contains);
        if (disablesSystemTrust || overridesTrustStore) {
            throw invalidConfiguration();
        }
    }

    private static QueryParameter parseParameter(String parameter) {
        int separator = parameter.indexOf('=');
        if (separator <= 0 || separator == parameter.length() - 1) {
            throw invalidConfiguration();
        }
        return new QueryParameter(
                decode(parameter.substring(0, separator)),
                decode(parameter.substring(separator + 1)));
    }

    private static String decode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            throw invalidConfiguration();
        }
    }

    private static IllegalStateException invalidConfiguration() {
        return new IllegalStateException(
                "Production DB_URL must target an Amazon RDS hostname and use exactly one "
                        + "sslMode=VERIFY_IDENTITY with the image's system truststore");
    }

    private record QueryParameter(String name, String value) {
    }
}
