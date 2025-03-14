package com.nitros64.nitros_games_backend.repositories.legacy;


import org.springframework.stereotype.Repository;

import com.nitros64.nitros_games_backend.model.entity.GameGenre;
import com.nitros64.nitros_games_backend.repositories.BaseRepository;


//IVGGenreRepo = VIDEO GAME GENRE REPOSITORY

@Repository
public interface GenreRepository extends BaseRepository<GameGenre,Long>{
    
}
