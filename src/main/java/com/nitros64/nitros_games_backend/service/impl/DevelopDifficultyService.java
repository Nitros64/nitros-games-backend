package com.nitros64.nitros_games_backend.service.impl;

import org.springframework.stereotype.Service;

import com.nitros64.nitros_games_backend.model.entity.DevelopmentDifficulty;
import com.nitros64.nitros_games_backend.repositories.BaseRepository;

/********************************************************************************
 *          DEVELOPMENT DIFFICULTY SERVICE IMPLEMENTATION                       *
 ********************************************************************************/

@Service
public class DevelopDifficultyService extends BaseServiceImpl<DevelopmentDifficulty,Long>{

    public DevelopDifficultyService(BaseRepository<DevelopmentDifficulty, Long> baseRepository) {
        super(baseRepository);
    }
}
