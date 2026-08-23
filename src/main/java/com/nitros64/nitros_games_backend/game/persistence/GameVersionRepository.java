package com.nitros64.nitros_games_backend.game.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;

import com.nitros64.nitros_games_backend.game.domain.GameVersion;
import com.nitros64.nitros_games_backend.shared.persistence.BaseRepository;

public interface GameVersionRepository extends BaseRepository<GameVersion, Long> {

    @EntityGraph(attributePaths = {
            "languageTool.programmingLanguage",
            "languageTool.programmingTool"
    })
    List<GameVersion> findAllByGameIdOrderById(Long gameId);

    @EntityGraph(attributePaths = {
            "game",
            "languageTool.programmingLanguage",
            "languageTool.programmingTool"
    })
    Optional<GameVersion> findByIdAndGameId(Long id, Long gameId);
}
