package com.nitros64.nitros_games_backend.catalog.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.nitros64.nitros_games_backend.catalog.domain.GameGenre;

public interface GameGenreRepository extends JpaRepository<GameGenre, Long> {

    Page<GameGenre> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
