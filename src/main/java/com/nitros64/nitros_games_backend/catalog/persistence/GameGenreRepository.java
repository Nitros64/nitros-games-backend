package com.nitros64.nitros_games_backend.catalog.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.nitros64.nitros_games_backend.catalog.domain.GameGenre;
import com.nitros64.nitros_games_backend.shared.persistence.BaseRepository;

public interface GameGenreRepository extends BaseRepository<GameGenre, Long> {

    Page<GameGenre> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
