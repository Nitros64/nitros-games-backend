package com.nitros64.nitros_games_backend.catalog.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.nitros64.nitros_games_backend.catalog.domain.Platform;

public interface PlatformRepository extends JpaRepository<Platform, Long> {

    Page<Platform> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
