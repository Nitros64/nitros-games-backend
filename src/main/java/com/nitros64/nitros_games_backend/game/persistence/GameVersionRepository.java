package com.nitros64.nitros_games_backend.game.persistence;


import org.springframework.stereotype.Repository;

import com.nitros64.nitros_games_backend.game.domain.GameVersion;
import com.nitros64.nitros_games_backend.shared.persistence.BaseRepository;

@Repository
public interface GameVersionRepository extends BaseRepository<GameVersion,Long>{
    java.util.List<GameVersion> findAllByGameIdOrderById(Long gameId);
}
