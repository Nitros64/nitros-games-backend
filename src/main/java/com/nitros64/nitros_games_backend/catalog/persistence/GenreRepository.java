package com.nitros64.nitros_games_backend.catalog.persistence;


import org.springframework.stereotype.Repository;

import com.nitros64.nitros_games_backend.catalog.domain.GameGenre;
import com.nitros64.nitros_games_backend.shared.persistence.BaseRepository;


//IVGGenreRepo = VIDEO GAME GENRE REPOSITORY

@Repository
public interface GenreRepository extends BaseRepository<GameGenre,Long>{
    
}
