package com.nitros64.nitros_games_backend.catalog.application;

import org.springframework.stereotype.Service;

import com.nitros64.nitros_games_backend.catalog.domain.Platform;
import com.nitros64.nitros_games_backend.service.impl.BaseServiceImpl;
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
