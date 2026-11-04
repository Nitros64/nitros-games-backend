package com.nitros64.nitros_games_backend.media.api;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers
@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.flyway.baseline-on-migrate=false",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
        "app.storage.game-media.directory=target/test-storage/game-media-mysql",
        "app.storage.game-media.max-bytes=4096",
        "app.storage.game-media.max-pixels=1024"
})
class GameMediaMySqlIT extends GameMediaApiContractTests {
    @Container
    @ServiceConnection
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4.11")
            .withDatabaseName("media_tests").withUsername("media_test").withPassword("test-password");
}
