package com.nitros64.nitros_games_backend.repositories.legacy;

import org.springframework.stereotype.Repository;

import com.nitros64.nitros_games_backend.model.entity.GameData;
import com.nitros64.nitros_games_backend.repositories.BaseRepository;

@Repository
public interface GameDataRepository extends BaseRepository<GameData,Long>{
    
}
