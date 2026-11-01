package com.nitros64.nitros_games_backend.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.DriverManager;
import java.sql.SQLException;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers
class MySqlPublicationMigrationIT {
    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4.11")
            .withDatabaseName("publication_upgrade")
            .withUsername("publication_test")
            .withPassword("test-password");

    @Test
    void upgradeFromV4MakesExistingGamesDrafts() throws Exception {
        Flyway.configure().dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration/mysql").target("4").load().migrate();
        try (var connection = DriverManager.getConnection(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
                var statement = connection.createStatement()) {
            statement.execute("""
                    INSERT INTO gamedata (name, descripcion, jam, dev_numbers)
                    VALUES ('Existing game', 'Existing description', false, 1)
                    """);
            var flyway = Flyway.configure()
                    .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                    .locations("classpath:db/migration/mysql").load();
            flyway.migrate();
            assertThat(flyway.info().current().getVersion().toString()).isEqualTo("5");
            statement.execute("""
                    INSERT INTO gamedata (name, descripcion, jam, dev_numbers)
                    VALUES ('New game', 'New description', false, 1)
                    """);
            try (var rows = statement.executeQuery("SELECT publication_status FROM gamedata ORDER BY id")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString(1)).isEqualTo("DRAFT");
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString(1)).isEqualTo("DRAFT");
            }
            assertThatThrownBy(() -> statement.execute("UPDATE gamedata SET publication_status = 'INVALID'"))
                    .isInstanceOf(SQLException.class);
            assertThatThrownBy(() -> statement.execute("UPDATE gamedata SET publication_status = NULL"))
                    .isInstanceOf(SQLException.class);
            try (var columns = connection.getMetaData().getColumns(
                    "publication_upgrade", null, "gamedata", "publication_status")) {
                assertThat(columns.next()).isTrue();
                assertThat(columns.getInt("DATA_TYPE")).isEqualTo(java.sql.Types.VARCHAR);
            }
        }
    }
}
