package com.nitros64.nitros_games_backend.game.persistence;

import org.springframework.stereotype.Repository;

import com.nitros64.nitros_games_backend.game.domain.GameData;
import com.nitros64.nitros_games_backend.shared.persistence.BaseRepository;

@Repository
public interface GameDataRepository extends BaseRepository<GameData,Long>{
    
}
