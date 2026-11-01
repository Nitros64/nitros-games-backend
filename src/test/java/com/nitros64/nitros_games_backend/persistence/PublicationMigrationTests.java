package com.nitros64.nitros_games_backend.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

class PublicationMigrationTests {

    @Test
    void v5MigratesExistingRowsAndEnforcesDraftDefaultAndConstraints() throws Exception {
        // A unique, process-local database; no external datasource configuration is used.
        try (var connection = DriverManager.getConnection(
                "jdbc:h2:mem:publication_" + UUID.randomUUID() + ";MODE=MySQL", "sa", "");
                var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE gamedata (id BIGINT PRIMARY KEY, name VARCHAR(255))");
            statement.execute("INSERT INTO gamedata VALUES (1, 'Existing game')");
            ScriptUtils.executeSqlScript(connection,
                    new ClassPathResource("db/migration/mysql/V5__add_game_publication_status.sql"));
            statement.execute("INSERT INTO gamedata (id, name) VALUES (2, 'New game')");
            try (var rows = statement.executeQuery("SELECT publication_status FROM gamedata ORDER BY id")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString(1)).isEqualTo("DRAFT");
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString(1)).isEqualTo("DRAFT");
            }
            assertThatThrownBy(() -> statement.execute(
                    "UPDATE gamedata SET publication_status = 'INVALID' WHERE id = 1"))
                    .isInstanceOf(SQLException.class);
            assertThatThrownBy(() -> statement.execute(
                    "UPDATE gamedata SET publication_status = NULL WHERE id = 1"))
                    .isInstanceOf(SQLException.class);
            statement.execute("UPDATE gamedata SET publication_status = 'PUBLISHED' WHERE id = 1");
            try (var columns = connection.getMetaData().getColumns(null, null, "GAMEDATA", "PUBLICATION_STATUS")) {
                assertThat(columns.next()).isTrue();
                assertThat(columns.getInt("DATA_TYPE")).isEqualTo(java.sql.Types.VARCHAR);
            }
        }
    }
}
