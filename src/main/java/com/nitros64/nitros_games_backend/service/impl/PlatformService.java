package com.nitros64.nitros_games_backend.service.impl;

import org.springframework.stereotype.Service;

import com.nitros64.nitros_games_backend.model.entity.Platform;
import com.nitros64.nitros_games_backend.repositories.BaseRepository;

/********************************************************************************
 *                      PLATFORM SERVICE IMPLEMENTATION                         *
 ********************************************************************************/

@Service
public class PlatformService extends BaseServiceImpl<Platform,Long>{
    public PlatformService(BaseRepository<Platform, Long> baseRepository) {
        super(baseRepository);
    }
}
