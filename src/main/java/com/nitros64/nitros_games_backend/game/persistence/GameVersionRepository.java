package com.nitros64.nitros_games_backend.game.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nitros64.nitros_games_backend.game.domain.GameVersion;

public interface GameVersionRepository extends JpaRepository<GameVersion, Long> {

    @EntityGraph(attributePaths = {
            "languageTool.programmingLanguage",
            "languageTool.programmingTool"
    })
    List<GameVersion> findAllDetailedByGameIdOrderById(Long gameId);

    @EntityGraph(attributePaths = {
            "game",
            "languageTool.programmingLanguage",
            "languageTool.programmingTool"
    })
    Optional<GameVersion> findDetailedByIdAndGameId(Long id, Long gameId);

    @Query("""
            select version
            from GameVersion version
            where version.id = :id
              and version.game.id = :gameId
            """)
    Optional<GameVersion> findOwnedByIdAndGameId(
            @Param("id") Long id,
            @Param("gameId") Long gameId);

    boolean existsByIdAndGameId(Long id, Long gameId);
}
