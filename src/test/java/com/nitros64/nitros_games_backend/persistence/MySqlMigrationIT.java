package com.nitros64.nitros_games_backend.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.Hibernate;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import com.nitros64.nitros_games_backend.catalog.domain.DevelopmentDifficulty;
import com.nitros64.nitros_games_backend.catalog.domain.GameGenre;
import com.nitros64.nitros_games_backend.catalog.domain.Platform;
import com.nitros64.nitros_games_backend.catalog.domain.Processor;
import com.nitros64.nitros_games_backend.catalog.persistence.DevelopmentDifficultyRepository;
import com.nitros64.nitros_games_backend.catalog.persistence.GameGenreRepository;
import com.nitros64.nitros_games_backend.catalog.persistence.PlatformRepository;
import com.nitros64.nitros_games_backend.catalog.persistence.ProcessorRepository;
import com.nitros64.nitros_games_backend.game.persistence.GameDataRepository;
import com.nitros64.nitros_games_backend.game.persistence.DownloadLinkRepository;
import com.nitros64.nitros_games_backend.game.persistence.GameVersionRepository;
import com.nitros64.nitros_games_backend.storage.domain.ServerHostImage;
import com.nitros64.nitros_games_backend.storage.persistence.ServerHostImageRepository;
import com.nitros64.nitros_games_backend.tooling.persistence.ProgrammingToolRepository;

import jakarta.persistence.EntityManager;

@Testcontainers
@SpringBootTest(properties = {
        "spring.flyway.baseline-on-migrate=false",
        "spring.jpa.hibernate.ddl-auto=validate",
        "app.storage.host-images.directory=target/test-storage/mysql-it",
        "app.security.admin-username=test-admin",
        "app.security.admin-password=test-admin-password",
        "app.security.allowed-origins=http://localhost:4200"
})
@Transactional
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
    private GameGenreRepository genreRepository;

    @Autowired
    private DevelopmentDifficultyRepository difficultyRepository;

    @Autowired
    private PlatformRepository platformRepository;

    @Autowired
    private ProcessorRepository processorRepository;

    @Autowired
    private GameDataRepository gameDataRepository;

    @Autowired
    private GameVersionRepository gameVersionRepository;

    @Autowired
    private DownloadLinkRepository downloadLinkRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ServerHostImageRepository serverHostImageRepository;

    @Autowired
    private ProgrammingToolRepository programmingToolRepository;

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

    @Test
    void programmingToolSearchRunsAgainstMySqlWithCompositeCompatibilityKeys() {
        jdbcTemplate.update("insert into programtool_type (name) values (?)", "Build");
        jdbcTemplate.update("insert into program_lang (name) values (?)", "Java");
        jdbcTemplate.update("insert into platform (name) values (?)", "Linux");
        jdbcTemplate.update("insert into processor (name) values (?)", "x86-64");

        Long typeId = id("programtool_type", "Build");
        Long languageId = id("program_lang", "Java");
        Long platformId = id("platform", "Linux");
        Long processorId = id("processor", "x86-64");

        jdbcTemplate.update("""
                insert into program_tool
                    (imagefile_path, name, web_page, fk_gametooltype)
                values (?, ?, ?, ?)
                """, "gradle.png", "Gradle", "https://gradle.example", typeId);
        Long toolId = id("program_tool", "Gradle");
        jdbcTemplate.update(
                "insert into tool_lang (program_lang_id, program_tool_id) values (?, ?)",
                languageId,
                toolId);
        jdbcTemplate.update(
                "insert into tool_platform (fk_idplatform, fk_idtool) values (?, ?)",
                platformId,
                toolId);
        jdbcTemplate.update(
                "insert into tool_processor (fk_idprocessor, fk_idtool) values (?, ?)",
                processorId,
                toolId);

        var result = programmingToolRepository.search(
                "grad",
                typeId,
                languageId,
                platformId,
                processorId,
                PageRequest.of(0, 10, Sort.by("name")));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).singleElement().satisfies(tool -> {
            assertThat(tool.getName()).isEqualTo("Gradle");
            assertThat(tool.getToolType().getName()).isEqualTo("Build");
        });
    }

    @Test
    void catalogNameSearchesRunAgainstMySqlCaseInsensitively() {
        genreRepository.saveAndFlush(new GameGenre("Strategy"));
        difficultyRepository.saveAndFlush(new DevelopmentDifficulty("Advanced"));
        platformRepository.saveAndFlush(new Platform("Windows"));
        processorRepository.saveAndFlush(new Processor("ARM64"));

        var page = PageRequest.of(0, 10, Sort.by("name"));

        assertThat(genreRepository.findByNameContainingIgnoreCase("RATEG", page).getContent())
                .singleElement()
                .extracting(GameGenre::getName)
                .isEqualTo("Strategy");
        assertThat(difficultyRepository.findByNameContainingIgnoreCase("DVANC", page).getContent())
                .singleElement()
                .extracting(DevelopmentDifficulty::getName)
                .isEqualTo("Advanced");
        assertThat(platformRepository.findByNameContainingIgnoreCase("INDOW", page).getContent())
                .singleElement()
                .extracting(Platform::getName)
                .isEqualTo("Windows");
        assertThat(processorRepository.findByNameContainingIgnoreCase("rm6", page).getContent())
                .singleElement()
                .extracting(Processor::getName)
                .isEqualTo("ARM64");
    }

    @Test
    void gameSearchRunsAgainstMySqlWithCombinedFiltersAndDetailedLoading() {
        jdbcTemplate.update("insert into dev_difficulty (name) values (?)", "Advanced");
        jdbcTemplate.update("insert into game_genres (name) values (?)", "Strategy");
        Long difficultyId = id("dev_difficulty", "Advanced");
        Long genreId = id("game_genres", "Strategy");
        jdbcTemplate.update("""
                insert into gamedata
                    (descripcion, dev_numbers, jam, name, dev_difficulty_id)
                values (?, ?, ?, ?, ?)
                """, "Built at a jam", 4, true, "Jam Project", difficultyId);
        Long gameId = id("gamedata", "Jam Project");
        jdbcTemplate.update(
                "insert into mygames_genres (mygame_id, genre_id) values (?, ?)",
                gameId,
                genreId);

        var ids = gameDataRepository.searchIds(
                "PROJECT",
                difficultyId,
                genreId,
                true,
                PageRequest.of(0, 10, Sort.by("name")));
        var games = gameDataRepository.findDetailedByIdIn(ids.getContent());

        assertThat(ids.getTotalElements()).isEqualTo(1);
        assertThat(games).singleElement().satisfies(game -> {
            assertThat(game.getName()).isEqualTo("Jam Project");
            assertThat(game.getDevelopmentDifficulty().getName()).isEqualTo("Advanced");
            assertThat(game.getGenres()).singleElement()
                    .extracting(GameGenre::getName)
                    .isEqualTo("Strategy");
        });
    }

    @Test
    void gameHierarchyQueriesRunAgainstMySqlWithoutUnnecessaryLoading() {
        jdbcTemplate.update("insert into dev_difficulty (name) values (?)", "Medium");
        jdbcTemplate.update("insert into programtool_type (name) values (?)", "Engine");
        jdbcTemplate.update("insert into program_lang (name) values (?)", "Java");
        jdbcTemplate.update("insert into platform (name) values (?)", "Windows");
        jdbcTemplate.update("insert into processor (name) values (?)", "x86-64");
        jdbcTemplate.update(
                "insert into server_hostimage (imagepath, name) values (?, ?)",
                "mediafire.png", "MediaFire");

        Long difficultyId = id("dev_difficulty", "Medium");
        Long typeId = id("programtool_type", "Engine");
        Long languageId = id("program_lang", "Java");
        Long platformId = id("platform", "Windows");
        Long processorId = id("processor", "x86-64");
        Long hostImageId = id("server_hostimage", "MediaFire");

        jdbcTemplate.update("""
                insert into gamedata
                    (descripcion, dev_numbers, jam, name, dev_difficulty_id)
                values (?, ?, ?, ?, ?)
                """, "First game", 2, false, "First Game", difficultyId);
        jdbcTemplate.update("""
                insert into gamedata
                    (descripcion, dev_numbers, jam, name, dev_difficulty_id)
                values (?, ?, ?, ?, ?)
                """, "Second game", 2, false, "Second Game", difficultyId);
        Long firstGameId = id("gamedata", "First Game");
        Long secondGameId = id("gamedata", "Second Game");

        jdbcTemplate.update("""
                insert into program_tool
                    (imagefile_path, name, web_page, fk_gametooltype)
                values (?, ?, ?, ?)
                """, "libgdx.png", "LibGDX", "https://libgdx.com", typeId);
        Long toolId = id("program_tool", "LibGDX");
        jdbcTemplate.update(
                "insert into tool_lang (program_lang_id, program_tool_id) values (?, ?)",
                languageId, toolId);
        jdbcTemplate.update(
                "insert into tool_platform (fk_idplatform, fk_idtool) values (?, ?)",
                platformId, toolId);
        jdbcTemplate.update(
                "insert into tool_processor (fk_idprocessor, fk_idtool) values (?, ?)",
                processorId, toolId);
        jdbcTemplate.update("""
                insert into game_version
                    (name, fk_gamedata, fk_idlang, fk_idtool, fk_idplatform, fk_idprocessor)
                values (?, ?, ?, ?, ?, ?)
                """, "Version One", firstGameId, languageId, toolId, platformId, processorId);
        Long versionId = id("game_version", "Version One");
        jdbcTemplate.update("""
                insert into download_link (link, fk_gameversion, fk_host_image)
                values (?, ?, ?)
                """, "https://files.example/game.zip", versionId, hostImageId);

        entityManager.clear();
        var detailed = gameVersionRepository.findDetailedByIdAndGameId(
                versionId, firstGameId).orElseThrow();
        assertThat(detailed.getLanguageTool().getProgrammingLanguage().getName())
                .isEqualTo("Java");
        assertThat(detailed.getLanguageTool().getProgrammingTool().getName())
                .isEqualTo("LibGDX");

        entityManager.clear();
        var owned = gameVersionRepository.findOwnedByIdAndGameId(
                versionId, firstGameId).orElseThrow();
        assertThat(Hibernate.isInitialized(owned.getLanguageTool())).isFalse();
        assertThat(Hibernate.isInitialized(owned.getToolPlatform())).isFalse();
        assertThat(Hibernate.isInitialized(owned.getToolProcessor())).isFalse();

        entityManager.clear();
        assertThat(downloadLinkRepository.findAllDetailedByHierarchy(versionId, firstGameId))
                .singleElement()
                .satisfies(link -> assertThat(link.getServerImage().getName())
                        .isEqualTo("MediaFire"));
        assertThat(downloadLinkRepository.findAllDetailedByHierarchy(versionId, secondGameId))
                .isEmpty();
    }

    @Test
    void hostImageNameSearchRunsAgainstMySqlCaseInsensitively() {
        serverHostImageRepository.saveAndFlush(
                new ServerHostImage("Dropbox", "dropbox.png"));

        var result = serverHostImageRepository.findByNameContainingIgnoreCase(
                "ROP",
                PageRequest.of(0, 10, Sort.by("name")));

        assertThat(result.getContent()).singleElement().satisfies(image -> {
            assertThat(image.getName()).isEqualTo("Dropbox");
            assertThat(image.getImagepath()).isEqualTo("dropbox.png");
        });
    }

    private Long id(String table, String name) {
        return jdbcTemplate.queryForObject(
                "select id from " + table + " where name = ?",
                Long.class,
                name);
    }
}
