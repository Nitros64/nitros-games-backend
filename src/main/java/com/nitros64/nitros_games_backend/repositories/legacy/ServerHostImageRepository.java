package com.nitros64.nitros_games_backend.repositories.legacy;

import org.springframework.stereotype.Repository;

import com.nitros64.nitros_games_backend.model.entity.ServerHostImage;
import com.nitros64.nitros_games_backend.repositories.BaseRepository;

import java.util.Optional;

@Repository
public interface ServerHostImageRepository extends BaseRepository<ServerHostImage, Long>{
    Optional<ServerHostImage> findByName(String name);
}
