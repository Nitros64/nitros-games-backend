package com.nitros64.nitros_games_backend.game.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.nitros64.nitros_games_backend.game.domain.GameVersion;

public interface GameVersionRepository extends JpaRepository<GameVersion, Long> {

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
