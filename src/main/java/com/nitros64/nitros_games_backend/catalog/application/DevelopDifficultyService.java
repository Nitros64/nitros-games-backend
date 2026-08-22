package com.nitros64.nitros_games_backend.catalog.application;

import org.springframework.stereotype.Service;

import com.nitros64.nitros_games_backend.catalog.domain.DevelopmentDifficulty;
import com.nitros64.nitros_games_backend.shared.application.BaseServiceImpl;
import com.nitros64.nitros_games_backend.shared.persistence.BaseRepository;

/********************************************************************************
 *          DEVELOPMENT DIFFICULTY SERVICE IMPLEMENTATION                       *
 ********************************************************************************/

@Service
public class DevelopDifficultyService extends BaseServiceImpl<DevelopmentDifficulty,Long>{

    public DevelopDifficultyService(BaseRepository<DevelopmentDifficulty, Long> baseRepository) {
        super(baseRepository);
    }
}
