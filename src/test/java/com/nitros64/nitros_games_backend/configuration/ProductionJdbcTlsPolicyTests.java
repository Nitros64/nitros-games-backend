package com.nitros64.nitros_games_backend.configuration;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

import org.junit.jupiter.api.Test;

class ProductionJdbcTlsPolicyTests {

    private static final String RDS_URL =
            "jdbc:mysql://database.example.eu-west-1.rds.amazonaws.com:3306/nitrosgames";

    @Test
    void acceptsRdsUrlWithIdentityVerificationAndSystemTrustStore() {
        assertThatNoException().isThrownBy(() -> ProductionJdbcTlsPolicy.validate(
                RDS_URL + "?sslMode=VERIFY_IDENTITY&connectTimeout=30000"));
    }

    @Test
    void rejectsMissingOrWeakerTlsModes() {
        assertRejected(RDS_URL);
        assertRejected(RDS_URL + "?sslMode=DISABLED");
        assertRejected(RDS_URL + "?sslMode=PREFERRED");
        assertRejected(RDS_URL + "?sslMode=REQUIRED");
        assertRejected(RDS_URL + "?sslMode=VERIFY_CA");
    }

    @Test
    void rejectsAmbiguousDuplicateTlsModes() {
        assertRejected(RDS_URL + "?sslMode=VERIFY_IDENTITY&sslMode=DISABLED");
    }

    @Test
    void rejectsTrustStoreBypassesAndOverrides() {
        assertRejected(RDS_URL + "?sslMode=VERIFY_IDENTITY&fallbackToSystemTrustStore=false");
        assertRejected(RDS_URL
                + "?sslMode=VERIFY_IDENTITY&trustCertificateKeyStoreUrl=file:/tmp/untrusted.jks");
    }

    @Test
    void rejectsIpAddressesAndNonRdsHostsSoHostnameVerificationUsesTheRdsEndpoint() {
        assertRejected("jdbc:mysql://10.43.10.10:3306/nitrosgames?sslMode=VERIFY_IDENTITY");
        assertRejected("jdbc:mysql://database.example.com:3306/nitrosgames?sslMode=VERIFY_IDENTITY");
    }

    private void assertRejected(String jdbcUrl) {
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> ProductionJdbcTlsPolicy.validate(jdbcUrl))
                .withMessageContaining("sslMode=VERIFY_IDENTITY");
    }
}
