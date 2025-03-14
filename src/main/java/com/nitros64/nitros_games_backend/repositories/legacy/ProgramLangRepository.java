package com.nitros64.nitros_games_backend.repositories.legacy;

import org.springframework.stereotype.Repository;

import com.nitros64.nitros_games_backend.model.entity.ProgrammingLanguage;
import com.nitros64.nitros_games_backend.repositories.BaseRepository;

    /********************************************************************************/
    /*                                                                              */
    /*                      PROGRAMMING LANGUAGE REPOSITORY                         */
    /*                                                                              */
    /********************************************************************************/

@Repository
public interface ProgramLangRepository extends BaseRepository<ProgrammingLanguage,Long>{ 
}