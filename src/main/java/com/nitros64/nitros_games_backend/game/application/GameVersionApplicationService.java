package com.nitros64.nitros_games_backend.game.application;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nitros64.nitros_games_backend.game.domain.GameData;
import com.nitros64.nitros_games_backend.game.domain.GameVersion;
import com.nitros64.nitros_games_backend.game.persistence.GameDataRepository;
import com.nitros64.nitros_games_backend.game.persistence.GameVersionRepository;
import com.nitros64.nitros_games_backend.shared.application.ResourceNotFoundException;
import com.nitros64.nitros_games_backend.tooling.application.ToolCompatibilityService;

@Service
public class GameVersionApplicationService {

    private final GameDataRepository games;
    private final GameVersionRepository versions;
    private final ToolCompatibilityService toolCompatibility;

    public GameVersionApplicationService(
            GameDataRepository games,
            GameVersionRepository versions,
            ToolCompatibilityService toolCompatibility) {
        this.games = games;
        this.versions = versions;
        this.toolCompatibility = toolCompatibility;
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
        version.attachToGame(requireGame(gameId));
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

    private GameVersion apply(GameVersion version, SaveGameVersionCommand command) {
        var compatibility = toolCompatibility.resolve(
                command.programmingLanguageId(),
                command.programmingToolId(),
                command.platformId(),
                command.processorId());
        version.updateCompatibility(
                command.name(),
                compatibility.languageTool(),
                compatibility.toolPlatform(),
                compatibility.toolProcessor(),
                command.platformId(),
                command.processorId());
        return version;
    }

    private GameData requireGame(Long gameId) {
        return games.findById(gameId)
                .orElseThrow(() -> new ResourceNotFoundException("Game not found"));
    }

    private GameVersion requireVersion(Long gameId, Long versionId) {
        return versions.findByIdAndGameId(versionId, gameId)
                .orElseThrow(() -> new ResourceNotFoundException("Game version not found"));
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
}
