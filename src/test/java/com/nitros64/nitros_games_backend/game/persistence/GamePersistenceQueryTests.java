package com.nitros64.nitros_games_backend.game.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.Set;

import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.nitros64.nitros_games_backend.catalog.domain.GameGenre;
import com.nitros64.nitros_games_backend.catalog.domain.Platform;
import com.nitros64.nitros_games_backend.catalog.domain.Processor;
import com.nitros64.nitros_games_backend.catalog.persistence.GameGenreRepository;
import com.nitros64.nitros_games_backend.catalog.persistence.PlatformRepository;
import com.nitros64.nitros_games_backend.catalog.persistence.ProcessorRepository;
import com.nitros64.nitros_games_backend.game.domain.DownloadLink;
import com.nitros64.nitros_games_backend.game.domain.GameData;
import com.nitros64.nitros_games_backend.game.domain.GameVersion;
import com.nitros64.nitros_games_backend.storage.domain.ServerHostImage;
import com.nitros64.nitros_games_backend.storage.persistence.ServerHostImageRepository;
import com.nitros64.nitros_games_backend.tooling.domain.LanguageTool;
import com.nitros64.nitros_games_backend.tooling.domain.ProgrammingLanguage;
import com.nitros64.nitros_games_backend.tooling.domain.ProgrammingTool;
import com.nitros64.nitros_games_backend.tooling.domain.ProgramToolType;
import com.nitros64.nitros_games_backend.tooling.domain.ToolPlatform;
import com.nitros64.nitros_games_backend.tooling.domain.ToolProcessor;
import com.nitros64.nitros_games_backend.tooling.persistence.LanguageToolRepository;
import com.nitros64.nitros_games_backend.tooling.persistence.ProgrammingLanguageRepository;
import com.nitros64.nitros_games_backend.tooling.persistence.ProgrammingToolRepository;
import com.nitros64.nitros_games_backend.tooling.persistence.ProgramToolTypeRepository;
import com.nitros64.nitros_games_backend.tooling.persistence.ToolPlatformRepository;
import com.nitros64.nitros_games_backend.tooling.persistence.ToolProcessorRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class GamePersistenceQueryTests {

    @Autowired EntityManager entityManager;
    @Autowired EntityManagerFactory entityManagerFactory;
    @Autowired GameGenreRepository genreRepository;
    @Autowired PlatformRepository platformRepository;
    @Autowired ProcessorRepository processorRepository;
    @Autowired ProgrammingLanguageRepository languageRepository;
    @Autowired ProgramToolTypeRepository toolTypeRepository;
    @Autowired ProgrammingToolRepository toolRepository;
    @Autowired LanguageToolRepository languageToolRepository;
    @Autowired ToolPlatformRepository toolPlatformRepository;
    @Autowired ToolProcessorRepository toolProcessorRepository;
    @Autowired ServerHostImageRepository hostImageRepository;
    @Autowired GameDataRepository gameRepository;
    @Autowired GameVersionRepository versionRepository;
    @Autowired DownloadLinkRepository downloadLinkRepository;

    private Statistics statistics;

    @BeforeEach
    void enableStatistics() {
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
    }

    @AfterEach
    void disableStatistics() {
        statistics.clear();
        statistics.setStatisticsEnabled(false);
    }

    @Test
    void detailedQueriesLoadResponseGraphsInOneStatement() throws Exception {
        var fixture = createFixture();

        resetPersistenceContext();
        var game = gameRepository.findDetailedById(fixture.gameId()).orElseThrow();

        assertThat(Hibernate.isInitialized(game.getGenres())).isTrue();
        assertThat(game.getGenres()).singleElement().extracting(GameGenre::getName)
                .isEqualTo("Adventure");
        assertThat(statistics.getPrepareStatementCount()).isOne();

        resetPersistenceContext();
        var version = versionRepository.findDetailedByIdAndGameId(
                fixture.versionId(), fixture.gameId()).orElseThrow();

        assertThat(Hibernate.isInitialized(version.getGame())).isTrue();
        assertThat(Hibernate.isInitialized(version.getLanguageTool())).isTrue();
        assertThat(Hibernate.isInitialized(version.getLanguageTool().getProgrammingLanguage()))
                .isTrue();
        assertThat(Hibernate.isInitialized(version.getLanguageTool().getProgrammingTool()))
                .isTrue();
        assertThat(statistics.getPrepareStatementCount()).isOne();
    }

    @Test
    void ownedVersionQueryDoesNotInitializeUnneededAssociations() throws Exception {
        var fixture = createFixture();

        resetPersistenceContext();
        var version = versionRepository.findOwnedByIdAndGameId(
                fixture.versionId(), fixture.gameId()).orElseThrow();

        assertThat(Hibernate.isInitialized(version.getGame())).isFalse();
        assertThat(Hibernate.isInitialized(version.getLanguageTool())).isFalse();
        assertThat(Hibernate.isInitialized(version.getToolPlatform())).isFalse();
        assertThat(Hibernate.isInitialized(version.getToolProcessor())).isFalse();
        assertThat(Hibernate.isInitialized(version.getDownloadLinks())).isFalse();
        assertThat(statistics.getPrepareStatementCount()).isOne();
    }

    @Test
    void downloadLinkQueriesLoadResponseGraphAndEnforceHierarchy() throws Exception {
        var fixture = createFixture();
        var otherGame = new GameData();
        otherGame.updateDetails(
                "Other Game", "Other game", false, 1, Set.of());
        otherGame = gameRepository.saveAndFlush(otherGame);

        resetPersistenceContext();
        var links = downloadLinkRepository.findAllDetailedByHierarchy(
                fixture.versionId(), fixture.gameId());

        assertThat(links).singleElement().satisfies(link -> {
            assertThat(Hibernate.isInitialized(link.getGameVersion())).isTrue();
            assertThat(Hibernate.isInitialized(link.getServerImage())).isTrue();
            assertThat(link.getServerImage().getName()).isEqualTo("MediaFire");
        });
        assertThat(statistics.getPrepareStatementCount()).isOne();

        resetPersistenceContext();
        assertThat(downloadLinkRepository.findAllDetailedByHierarchy(
                fixture.versionId(), otherGame.getId())).isEmpty();
        assertThat(statistics.getPrepareStatementCount()).isOne();
    }

    @Test
    void gameSearchAppliesCombinedFiltersWithStablePagination() throws Exception {
        var fixture = createFixture();
        var genre = genreRepository.getReferenceById(fixture.genreId());
        saveGame("Alpha Jam", true, genre);
        saveGame("Zeta Jam", true,  genre);

        resetPersistenceContext();
        var firstPage = gameRepository.searchIds(
                "JAM",  fixture.genreId(), true,
                PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "name")));
        var secondPage = gameRepository.searchIds(
                "JAM",  fixture.genreId(), true,
                PageRequest.of(1, 1, Sort.by(Sort.Direction.DESC, "name")));

        assertThat(firstPage.getTotalElements()).isEqualTo(2);
        assertThat(gameRepository.findDetailedByIdIn(firstPage.getContent()))
                .singleElement().extracting(GameData::getName).isEqualTo("Zeta Jam");
        assertThat(gameRepository.findDetailedByIdIn(secondPage.getContent()))
                .singleElement().extracting(GameData::getName).isEqualTo("Alpha Jam");
    }

    private Fixture createFixture() throws Exception {
        var genre = genreRepository.saveAndFlush(new GameGenre("Adventure"));
        var platform = platformRepository.saveAndFlush(new Platform("Windows"));
        var processor = processorRepository.saveAndFlush(new Processor("x86-64"));
        var language = languageRepository.saveAndFlush(new ProgrammingLanguage("Java"));
        var toolType = toolTypeRepository.saveAndFlush(new ProgramToolType("Engine"));
        var tool = toolRepository.saveAndFlush(new ProgrammingTool(
                "LibGDX", URI.create("https://libgdx.com").toURL(), "libgdx.png", toolType));
        var languageTool = languageToolRepository.saveAndFlush(new LanguageTool(language, tool));
        var toolPlatform = toolPlatformRepository.saveAndFlush(new ToolPlatform(tool, platform));
        var toolProcessor = toolProcessorRepository.saveAndFlush(new ToolProcessor(tool, processor));
        var hostImage = hostImageRepository.saveAndFlush(
                new ServerHostImage("MediaFire", "mediafire.png"));
        var game = saveGame("Nitro Game", false,  genre);

        var version = new GameVersion();
        version.attachToGame(game);
        version.updateCompatibility(
                "Version One", languageTool, toolPlatform, toolProcessor,
                platform.getId(), processor.getId());
        version = versionRepository.saveAndFlush(version);

        var link = new DownloadLink();
        link.attachToVersion(version);
        link.updateDetails("https://files.example/game.zip", hostImage);
        link = downloadLinkRepository.saveAndFlush(link);

        return new Fixture( genre.getId(), game.getId(), version.getId(), link.getId());
    }

    private GameData saveGame(
            String name,
            boolean jam,
            GameGenre genre) {
        var game = new GameData();
        game.updateDetails(name, "Game description", jam, 2, Set.of(genre));
        return gameRepository.saveAndFlush(game);
    }

    private void resetPersistenceContext() {
        entityManager.flush();
        entityManager.clear();
        statistics.clear();
    }

    private record Fixture(
            Long genreId,
            Long gameId,
            Long versionId,
            Long linkId) {
    }
}
