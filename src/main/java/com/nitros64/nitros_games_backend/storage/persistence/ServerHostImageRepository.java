package com.nitros64.nitros_games_backend.storage.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.nitros64.nitros_games_backend.storage.domain.ServerHostImage;
import com.nitros64.nitros_games_backend.shared.persistence.BaseRepository;

public interface ServerHostImageRepository extends BaseRepository<ServerHostImage, Long> {

    Page<ServerHostImage> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
