package com.nitros64.nitros_games_backend.game.application;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nitros64.nitros_games_backend.catalog.application.DevelopDifficultyService;
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
    private final DevelopDifficultyService difficulties;
    private final GameGenreService genres;
    private final ToolCompatibilityService toolCompatibility;
    private final ServerHostImageService hostImages;

    public GameApplicationService(
            GameDataRepository games,
            GameVersionRepository versions,
            DownloadLinkRepository downloadLinks,
            DevelopDifficultyService difficulties,
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
        return games.findAll().stream().map(this::toDetails).toList();
    }

    @Transactional(readOnly = true)
    public Page<GameDetails> findAllGames(Pageable pageable) {
        return games.findAll(pageable).map(this::toDetails);
    }

    @Transactional(readOnly = true)
    public GameDetails findGame(Long gameId) {
        return toDetails(requireGame(gameId));
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
        var resolvedGenres = new LinkedHashSet<GameGenre>();
        command.genreIds().forEach(id -> resolvedGenres.add(genres.findById(id)));
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
        var version = versions.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Game version not found"));
        if (!version.getGame().getId().equals(gameId)) {
            throw new ResourceNotFoundException("Game version not found");
        }
        return version;
    }

    private DownloadLink requireDownloadLink(Long gameId, Long versionId, Long linkId) {
        requireVersion(gameId, versionId);
        var link = downloadLinks.findById(linkId)
                .orElseThrow(() -> new ResourceNotFoundException("Download link not found"));
        if (!link.getGameVersion().getId().equals(versionId)) {
            throw new ResourceNotFoundException("Download link not found");
        }
        return link;
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
                version.getLanguageTool().getProgram_language().getId(),
                version.getLanguageTool().getProgram_tool().getId(),
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
