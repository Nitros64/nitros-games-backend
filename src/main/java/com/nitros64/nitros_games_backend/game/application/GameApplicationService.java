package com.nitros64.nitros_games_backend.game.application;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nitros64.nitros_games_backend.catalog.application.DevelopmentDifficultyService;
import com.nitros64.nitros_games_backend.catalog.application.GameGenreService;
import com.nitros64.nitros_games_backend.catalog.domain.GameGenre;
import com.nitros64.nitros_games_backend.game.domain.DownloadLink;
import com.nitros64.nitros_games_backend.game.domain.GameData;
import com.nitros64.nitros_games_backend.game.domain.GameVersion;
import com.nitros64.nitros_games_backend.game.persistence.DownloadLinkRepository;
import com.nitros64.nitros_games_backend.game.persistence.GameDataRepository;
import com.nitros64.nitros_games_backend.game.persistence.GameVersionRepository;
import com.nitros64.nitros_games_backend.shared.application.ResourceNotFoundException;
import com.nitros64.nitros_games_backend.storage.application.ServerHostImageService;
import com.nitros64.nitros_games_backend.tooling.application.ToolCompatibilityService;

@Service
public class GameApplicationService {

    private final GameDataRepository games;
    private final GameVersionRepository versions;
    private final DownloadLinkRepository downloadLinks;
    private final DevelopmentDifficultyService difficulties;
    private final GameGenreService genres;
    private final ToolCompatibilityService toolCompatibility;
    private final ServerHostImageService hostImages;

    public GameApplicationService(
            GameDataRepository games,
            GameVersionRepository versions,
            DownloadLinkRepository downloadLinks,
            DevelopmentDifficultyService difficulties,
            GameGenreService genres,
            ToolCompatibilityService toolCompatibility,
            ServerHostImageService hostImages) {
        this.games = games;
        this.versions = versions;
        this.downloadLinks = downloadLinks;
        this.difficulties = difficulties;
        this.genres = genres;
        this.toolCompatibility = toolCompatibility;
        this.hostImages = hostImages;
    }

    @Transactional(readOnly = true)
    public List<GameDetails> findAllGames() {
        return games.findAllDetailed().stream().map(this::toDetails).toList();
    }

    @Transactional(readOnly = true)
    public Page<GameDetails> findAllGames(Pageable pageable) {
        return hydrate(games.findAllIds(pageable), pageable);
    }

    @Transactional(readOnly = true)
    public Page<GameDetails> searchGames(GameSearchCriteria criteria, Pageable pageable) {
        return hydrate(games.searchIds(
                criteria.name(),
                criteria.developmentDifficultyId(),
                criteria.genreId(),
                criteria.jam(),
                pageable), pageable);
    }

    @Transactional(readOnly = true)
    public GameDetails findGame(Long gameId) {
        return toDetails(games.findDetailedById(gameId)
                .orElseThrow(() -> new ResourceNotFoundException("Game not found")));
    }

    @Transactional
    public GameDetails createGame(SaveGameCommand command) {
        return toDetails(games.saveAndFlush(apply(new GameData(), command)));
    }

    @Transactional
    public GameDetails updateGame(Long gameId, SaveGameCommand command) {
        return toDetails(games.saveAndFlush(apply(requireGame(gameId), command)));
    }

    @Transactional
    public void deleteGame(Long gameId) {
        games.delete(requireGame(gameId));
        games.flush();
    }

    @Transactional(readOnly = true)
    public List<GameVersionDetails> findVersions(Long gameId) {
        requireGame(gameId);
        return versions.findAllByGameIdOrderById(gameId).stream()
                .map(this::toDetails)
                .toList();
    }

    @Transactional(readOnly = true)
    public GameVersionDetails findVersion(Long gameId, Long versionId) {
        return toDetails(requireVersion(gameId, versionId));
    }

    @Transactional
    public GameVersionDetails createVersion(Long gameId, SaveGameVersionCommand command) {
        var version = new GameVersion();
        version.setGame(requireGame(gameId));
        return toDetails(versions.saveAndFlush(apply(version, command)));
    }

    @Transactional
    public GameVersionDetails updateVersion(
            Long gameId,
            Long versionId,
            SaveGameVersionCommand command) {
        return toDetails(versions.saveAndFlush(apply(requireVersion(gameId, versionId), command)));
    }

    @Transactional
    public void deleteVersion(Long gameId, Long versionId) {
        versions.delete(requireVersion(gameId, versionId));
        versions.flush();
    }

    @Transactional(readOnly = true)
    public List<DownloadLinkDetails> findDownloadLinks(Long gameId, Long versionId) {
        requireVersion(gameId, versionId);
        return downloadLinks.findAllByGameVersionIdOrderById(versionId).stream()
                .map(this::toDetails)
                .toList();
    }

    @Transactional(readOnly = true)
    public DownloadLinkDetails findDownloadLink(Long gameId, Long versionId, Long linkId) {
        return toDetails(requireDownloadLink(gameId, versionId, linkId));
    }

    @Transactional
    public DownloadLinkDetails createDownloadLink(
            Long gameId,
            Long versionId,
            SaveDownloadLinkCommand command) {
        var link = new DownloadLink();
        link.setGameVersion(requireVersion(gameId, versionId));
        return toDetails(downloadLinks.saveAndFlush(apply(link, command)));
    }

    @Transactional
    public DownloadLinkDetails updateDownloadLink(
            Long gameId,
            Long versionId,
            Long linkId,
            SaveDownloadLinkCommand command) {
        var link = requireDownloadLink(gameId, versionId, linkId);
        return toDetails(downloadLinks.saveAndFlush(apply(link, command)));
    }

    @Transactional
    public void deleteDownloadLink(Long gameId, Long versionId, Long linkId) {
        downloadLinks.delete(requireDownloadLink(gameId, versionId, linkId));
        downloadLinks.flush();
    }

    private GameData apply(GameData game, SaveGameCommand command) {
        game.setName(command.name());
        game.setDescription(command.description());
        game.setJam(command.jam());
        game.setDeveloperCount(command.developerCount());
        game.setDevelopmentDifficulty(difficulties.findById(command.developmentDifficultyId()));
        var resolvedGenres = new LinkedHashSet<GameGenre>(genres.findAllById(command.genreIds()));
        game.replaceGenres(resolvedGenres);
        return game;
    }

    private GameVersion apply(GameVersion version, SaveGameVersionCommand command) {
        var compatibility = toolCompatibility.resolve(
                command.programmingLanguageId(),
                command.programmingToolId(),
                command.platformId(),
                command.processorId());
        version.setName(command.name());
        version.setLanguageTool(compatibility.languageTool());
        version.setToolPlatform(compatibility.toolPlatform());
        version.setToolProcessor(compatibility.toolProcessor());
        version.setPlatformId(command.platformId());
        version.setProcessorId(command.processorId());
        return version;
    }

    private DownloadLink apply(DownloadLink link, SaveDownloadLinkCommand command) {
        link.setLink(command.link());
        link.setServerImage(hostImages.findById(command.serverHostImageId()));
        return link;
    }

    private GameData requireGame(Long gameId) {
        return games.findById(gameId)
                .orElseThrow(() -> new ResourceNotFoundException("Game not found"));
    }

    private GameVersion requireVersion(Long gameId, Long versionId) {
        return versions.findByIdAndGameId(versionId, gameId)
                .orElseThrow(() -> new ResourceNotFoundException("Game version not found"));
    }

    private DownloadLink requireDownloadLink(Long gameId, Long versionId, Long linkId) {
        return downloadLinks.findDetailedByIdAndHierarchy(linkId, versionId, gameId)
                .orElseThrow(() -> new ResourceNotFoundException("Download link not found"));
    }

    private Page<GameDetails> hydrate(Page<Long> gameIds, Pageable pageable) {
        if (gameIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, gameIds.getTotalElements());
        }
        var gamesById = games.findDetailedByIdIn(gameIds.getContent()).stream()
                .collect(Collectors.toMap(GameData::getId, Function.identity()));
        var details = gameIds.getContent().stream()
                .map(gamesById::get)
                .map(this::toDetails)
                .toList();
        return new PageImpl<>(details, pageable, gameIds.getTotalElements());
    }

    private GameDetails toDetails(GameData game) {
        return new GameDetails(
                game.getId(),
                game.getName(),
                game.getDescription(),
                game.isJam(),
                game.getDeveloperCount(),
                game.getDevelopmentDifficulty().getId(),
                game.getGenres().stream()
                        .map(GameGenre::getId)
                        .collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    private GameVersionDetails toDetails(GameVersion version) {
        return new GameVersionDetails(
                version.getId(),
                version.getGame().getId(),
                version.getName(),
                version.getLanguageTool().getProgrammingLanguage().getId(),
                version.getLanguageTool().getProgrammingTool().getId(),
                version.getPlatformId(),
                version.getProcessorId());
    }

    private DownloadLinkDetails toDetails(DownloadLink link) {
        return new DownloadLinkDetails(
                link.getId(),
                link.getGameVersion().getId(),
                link.getLink(),
                link.getServerImage().getId());
    }
}
