package com.nitros64.nitros_games_backend.game.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.nitros64.nitros_games_backend.catalog.domain.DevelopmentDifficulty;
import com.nitros64.nitros_games_backend.catalog.domain.GameGenre;
import com.nitros64.nitros_games_backend.catalog.domain.Platform;
import com.nitros64.nitros_games_backend.catalog.domain.Processor;
import com.nitros64.nitros_games_backend.catalog.persistence.DevDifficultyRepository;
import com.nitros64.nitros_games_backend.catalog.persistence.GenreRepository;
import com.nitros64.nitros_games_backend.catalog.persistence.PlatformRepository;
import com.nitros64.nitros_games_backend.catalog.persistence.ProcessorRepository;
import com.nitros64.nitros_games_backend.game.persistence.DownloadLinkRepository;
import com.nitros64.nitros_games_backend.game.persistence.GameDataRepository;
import com.nitros64.nitros_games_backend.game.persistence.GameVersionRepository;
import com.nitros64.nitros_games_backend.storage.domain.ServerHostImage;
import com.nitros64.nitros_games_backend.storage.persistence.ServerHostImageRepository;
import com.nitros64.nitros_games_backend.tooling.domain.LanguageTool;
import com.nitros64.nitros_games_backend.tooling.domain.ProgrammingLanguage;
import com.nitros64.nitros_games_backend.tooling.domain.ProgrammingTool;
import com.nitros64.nitros_games_backend.tooling.domain.ProgramToolType;
import com.nitros64.nitros_games_backend.tooling.domain.ToolPlatform;
import com.nitros64.nitros_games_backend.tooling.domain.ToolProcessor;
import com.nitros64.nitros_games_backend.tooling.persistence.IToolPlatformDao;
import com.nitros64.nitros_games_backend.tooling.persistence.IToolProcessorDao;
import com.nitros64.nitros_games_backend.tooling.persistence.ProgramLangRepository;
import com.nitros64.nitros_games_backend.tooling.persistence.ProgramToolLangRepo;
import com.nitros64.nitros_games_backend.tooling.persistence.ProgramToolRepository;
import com.nitros64.nitros_games_backend.tooling.persistence.ProgramToolTypeRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GameApiContractTests {

    private static final String ADMIN_USERNAME = "test-admin";
    private static final String ADMIN_PASSWORD = "test-admin-password";

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired DevDifficultyRepository difficultyRepository;
    @Autowired GenreRepository genreRepository;
    @Autowired PlatformRepository platformRepository;
    @Autowired ProcessorRepository processorRepository;
    @Autowired ProgramLangRepository languageRepository;
    @Autowired ProgramToolTypeRepository toolTypeRepository;
    @Autowired ProgramToolRepository toolRepository;
    @Autowired ProgramToolLangRepo languageToolRepository;
    @Autowired IToolPlatformDao toolPlatformRepository;
    @Autowired IToolProcessorDao toolProcessorRepository;
    @Autowired ServerHostImageRepository hostImageRepository;
    @Autowired GameDataRepository gameRepository;
    @Autowired GameVersionRepository versionRepository;
    @Autowired DownloadLinkRepository downloadLinkRepository;

    private DevelopmentDifficulty difficulty;
    private GameGenre genre;
    private Platform platform;
    private Processor processor;
    private ProgrammingLanguage language;
    private ProgrammingTool tool;
    private ServerHostImage hostImage;

    @BeforeEach
    void prepareCompatibleCatalog() throws Exception {
        clearDatabase();
        difficulty = difficultyRepository.saveAndFlush(new DevelopmentDifficulty("Medium"));
        genre = genreRepository.saveAndFlush(new GameGenre("Adventure"));
        platform = platformRepository.saveAndFlush(new Platform("Windows"));
        processor = processorRepository.saveAndFlush(new Processor("x86-64"));
        language = languageRepository.saveAndFlush(new ProgrammingLanguage("Java"));
        var toolType = toolTypeRepository.saveAndFlush(new ProgramToolType("Engine"));
        tool = toolRepository.saveAndFlush(new ProgrammingTool(
                "LibGDX", URI.create("https://libgdx.com").toURL(), "libgdx.png", toolType));
        languageToolRepository.save(new LanguageTool(language, tool));
        toolPlatformRepository.saveAndFlush(new ToolPlatform(tool, platform));
        toolProcessorRepository.saveAndFlush(new ToolProcessor(tool, processor));
        hostImage = hostImageRepository.saveAndFlush(new ServerHostImage("MediaFire", "mediafire.png"));
    }

    @AfterEach
    void removeCompatibleCatalog() {
        clearDatabase();
    }

    @Test
    void completeLifecycleUsesNestedResourcesAndCascadesDeletion() throws Exception {
        createGame("Nitro Game").andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Great game"))
                .andExpect(jsonPath("$.developmentDifficultyId").value(difficulty.getId()))
                .andExpect(jsonPath("$.genreIds.length()").value(1));
        var game = gameRepository.findAll().getFirst();

        mvc.perform(post("/api/v1/games/{gameId}/versions", game.getId())
                        .with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content(versionJson("Version One", platform.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gameId").value(game.getId()))
                .andExpect(jsonPath("$.programmingToolId").value(tool.getId()));
        var version = versionRepository.findAll().getFirst();

        mvc.perform(post("/api/v1/games/{gameId}/versions/{versionId}/download-links",
                        game.getId(), version.getId())
                        .with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content(downloadLinkJson("https://files.example/game.zip")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gameVersionId").value(version.getId()))
                .andExpect(jsonPath("$.serverHostImageId").value(hostImage.getId()));

        mvc.perform(get("/api/v1/games/{gameId}", game.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nitro Game"));
        mvc.perform(get("/api/v1/games/{gameId}/versions/{versionId}/download-links",
                        game.getId(), version.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].link").value("https://files.example/game.zip"));
        var link = downloadLinkRepository.findAll().getFirst();
        mvc.perform(put("/api/v1/games/{gameId}/versions/{versionId}",
                        game.getId(), version.getId())
                        .with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content(versionJson("Version Two", platform.getId())))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.name").value("Version Two"));
        mvc.perform(put("/api/v1/games/{gameId}/versions/{versionId}/download-links/{linkId}",
                        game.getId(), version.getId(), link.getId())
                        .with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content(downloadLinkJson("https://files.example/game-v2.zip")))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.link").value("https://files.example/game-v2.zip"));

        mvc.perform(put("/api/v1/games/{gameId}", game.getId())
                        .with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content(gameJson("Nitro Updated")))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.name").value("Nitro Updated"));

        mvc.perform(delete("/api/v1/games/{gameId}", game.getId()).with(admin()))
                .andExpect(status().isNoContent());
        assertThat(gameRepository.count()).isZero();
        assertThat(versionRepository.count()).isZero();
        assertThat(downloadLinkRepository.count()).isZero();
    }

    @Test
    void developmentDifficultyCanBeSharedBySeveralGames() throws Exception {
        createGame("First Game").andExpect(status().isCreated());
        createGame("Second Game").andExpect(status().isCreated());
        assertThat(gameRepository.count()).isEqualTo(2);
    }

    @Test
    void invalidCompatibilityDoesNotCreateVersion() throws Exception {
        createGame("Nitro Game").andExpect(status().isCreated());
        var game = gameRepository.findAll().getFirst();

        mvc.perform(post("/api/v1/games/{gameId}/versions", game.getId())
                        .with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content(versionJson("Version One", platform.getId() + 999)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("resource_not_found"));
        assertThat(versionRepository.count()).isZero();
    }

    @Test
    void nestedResourcesCannotBeAccessedThroughAnotherGame() throws Exception {
        createGame("First Game").andExpect(status().isCreated());
        createGame("Second Game").andExpect(status().isCreated());
        var games = gameRepository.findAll();
        var first = games.get(0);
        var second = games.get(1);
        mvc.perform(post("/api/v1/games/{gameId}/versions", first.getId())
                        .with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content(versionJson("Version One", platform.getId())))
                .andExpect(status().isCreated());
        var version = versionRepository.findAll().getFirst();

        mvc.perform(get("/api/v1/games/{gameId}/versions/{versionId}",
                        second.getId(), version.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("resource_not_found"));
    }

    @Test
    void duplicateDownloadLinkReturnsConflictWithoutCreatingAnotherRow() throws Exception {
        createGame("Nitro Game").andExpect(status().isCreated());
        var game = gameRepository.findAll().getFirst();
        mvc.perform(post("/api/v1/games/{gameId}/versions", game.getId())
                        .with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content(versionJson("Version One", platform.getId())))
                .andExpect(status().isCreated());
        var version = versionRepository.findAll().getFirst();
        var endpoint = "/api/v1/games/{gameId}/versions/{versionId}/download-links";
        mvc.perform(post(endpoint, game.getId(), version.getId())
                        .with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content(downloadLinkJson("https://files.example/game.zip")))
                .andExpect(status().isCreated());
        mvc.perform(post(endpoint, game.getId(), version.getId())
                        .with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content(downloadLinkJson("https://files.example/game.zip")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("data_conflict"));
        assertThat(downloadLinkRepository.count()).isOne();
    }

    @Test
    void mutationsRequireAdminAndRequestsAreValidated() throws Exception {
        mvc.perform(post("/api/v1/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gameJson("Nitro Game")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication_required"));

        mvc.perform(post("/api/v1/games").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"x","description":"","jam":false,"developerCount":0,
                                 "developmentDifficultyId":null,"genreIds":[]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));
        assertThat(gameRepository.count()).isZero();
    }

    private org.springframework.test.web.servlet.ResultActions createGame(String name) throws Exception {
        return mvc.perform(post("/api/v1/games").with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(gameJson(name)));
    }

    private String gameJson(String name) {
        return """
                {"name":"%s","description":"Great game","jam":false,"developerCount":2,
                 "developmentDifficultyId":%d,"genreIds":[%d]}
                """.formatted(name, difficulty.getId(), genre.getId());
    }

    private String versionJson(String name, Long platformId) {
        return """
                {"name":"%s","programmingLanguageId":%d,"programmingToolId":%d,
                 "platformId":%d,"processorId":%d}
                """.formatted(name, language.getId(), tool.getId(), platformId, processor.getId());
    }

    private String downloadLinkJson(String link) {
        return """
                {"link":"%s","serverHostImageId":%d}
                """.formatted(link, hostImage.getId());
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor admin() {
        return httpBasic(ADMIN_USERNAME, ADMIN_PASSWORD);
    }

    private void clearDatabase() {
        jdbc.update("delete from download_link");
        jdbc.update("delete from game_version");
        jdbc.update("delete from mygames_genres");
        jdbc.update("delete from gamedata");
        jdbc.update("delete from tool_lang");
        jdbc.update("delete from tool_platform");
        jdbc.update("delete from tool_processor");
        jdbc.update("delete from program_tool");
        jdbc.update("delete from program_lang");
        jdbc.update("delete from programtool_type");
        jdbc.update("delete from platform");
        jdbc.update("delete from processor");
        jdbc.update("delete from server_hostimage");
        jdbc.update("delete from game_genres");
        jdbc.update("delete from dev_difficulty");
    }
}
