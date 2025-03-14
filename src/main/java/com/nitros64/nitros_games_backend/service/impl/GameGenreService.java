package com.nitros64.nitros_games_backend.service.impl;

import org.springframework.stereotype.Service;

import com.nitros64.nitros_games_backend.model.entity.GameGenre;
import com.nitros64.nitros_games_backend.repositories.BaseRepository;

/********************************************************************************
 *                      GAME GENRE SERVICE IMPLEMENTATION                       *
 ********************************************************************************/

@Service
public class GameGenreService extends BaseServiceImpl<GameGenre,Long> {

    public GameGenreService(BaseRepository<GameGenre, Long> baseRepository) {
        super(baseRepository);
    }
}
