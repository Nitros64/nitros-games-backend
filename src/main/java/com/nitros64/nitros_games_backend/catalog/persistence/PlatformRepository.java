package com.nitros64.nitros_games_backend.catalog.persistence;

import org.springframework.stereotype.Repository;

import com.nitros64.nitros_games_backend.catalog.domain.Platform;
import com.nitros64.nitros_games_backend.shared.persistence.BaseRepository;

@Repository
public interface PlatformRepository extends BaseRepository<Platform,Long>{
    
}
