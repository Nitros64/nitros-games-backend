package com.nitros64.nitros_games_backend.storage.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.nitros64.nitros_games_backend.storage.domain.ServerHostImage;

public interface ServerHostImageRepository extends JpaRepository<ServerHostImage, Long> {

    Page<ServerHostImage> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
