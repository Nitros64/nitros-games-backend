package com.nitros64.nitros_games_backend.media.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.nitros64.nitros_games_backend.media.domain.GameMedia;
import com.nitros64.nitros_games_backend.media.domain.GameMediaType;

public interface GameMediaRepository extends JpaRepository<GameMedia, Long> {
    List<GameMedia> findAllByGameIdOrderByTypeAscPositionAsc(Long gameId);
    List<GameMedia> findAllByGameIdAndTypeOrderByPositionAsc(Long gameId, GameMediaType type);
    Optional<GameMedia> findByIdAndGameId(Long id, Long gameId);
    Optional<GameMedia> findByGameIdAndType(Long gameId, GameMediaType type);
}
