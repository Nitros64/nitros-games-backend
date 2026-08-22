package com.nitros64.nitros_games_backend.storage.persistence;

import org.springframework.stereotype.Repository;

import com.nitros64.nitros_games_backend.storage.domain.ServerHostImage;
import com.nitros64.nitros_games_backend.shared.persistence.BaseRepository;

import java.util.Optional;

@Repository
public interface ServerHostImageRepository extends BaseRepository<ServerHostImage, Long>{
    Optional<ServerHostImage> findByName(String name);
}
