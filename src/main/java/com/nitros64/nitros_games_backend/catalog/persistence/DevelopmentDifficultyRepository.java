package com.nitros64.nitros_games_backend.catalog.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.nitros64.nitros_games_backend.catalog.domain.DevelopmentDifficulty;
import com.nitros64.nitros_games_backend.shared.persistence.BaseRepository;

public interface DevelopmentDifficultyRepository
        extends BaseRepository<DevelopmentDifficulty, Long> {

    Page<DevelopmentDifficulty> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
