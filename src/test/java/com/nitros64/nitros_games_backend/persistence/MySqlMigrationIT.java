package com.nitros64.nitros_games_backend.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import com.nitros64.nitros_games_backend.catalog.persistence.GenreRepository;

@Testcontainers
@SpringBootTest(properties = {
        "spring.flyway.baseline-on-migrate=false",
        "spring.jpa.hibernate.ddl-auto=validate",
        "app.storage.host-images.directory=target/test-storage/mysql-it",
        "app.security.admin-username=test-admin",
        "app.security.admin-password=test-admin-password",
        "app.security.allowed-origins=http://localhost:4200"
})
class MySqlMigrationIT {

    @Container
    @ServiceConnection
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4.11")
            .withDatabaseName("nitrosgames")
            .withUsername("nitros")
            .withPassword("test-password");

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private GenreRepository genreRepository;

    @Test
    void flywayCreatesSchemaThatMatchesTheJpaModel() {
        assertThat(flyway.info().current().getVersion().toString()).isEqualTo("2");

        Integer applicationTableCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name <> 'flyway_schema_history'
                """, Integer.class);

        assertThat(applicationTableCount).isEqualTo(15);
        assertThat(genreRepository.findAll()).isEmpty();

        jdbcTemplate.update("insert into dev_difficulty (name) values (?)", "Medium");
        Long difficultyId = jdbcTemplate.queryForObject(
                "select id from dev_difficulty where name = ?", Long.class, "Medium");
        jdbcTemplate.update("""
                insert into gamedata
                    (descripcion, dev_numbers, jam, name, dev_difficulty_id)
                values (?, ?, ?, ?, ?)
                """, "First game", 2, false, "First game", difficultyId);
        jdbcTemplate.update("""
                insert into gamedata
                    (descripcion, dev_numbers, jam, name, dev_difficulty_id)
                values (?, ?, ?, ?, ?)
                """, "Second game", 3, false, "Second game", difficultyId);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from gamedata where dev_difficulty_id = ?",
                Integer.class,
                difficultyId)).isEqualTo(2);
    }
}
