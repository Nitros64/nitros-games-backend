package com.nitros64.nitros_games_backend.tooling.persistence;

import org.springframework.stereotype.Repository;

import com.nitros64.nitros_games_backend.tooling.domain.ProgrammingLanguage;
import com.nitros64.nitros_games_backend.repositories.BaseRepository;

    /********************************************************************************/
    /*                                                                              */
    /*                      PROGRAMMING LANGUAGE REPOSITORY                         */
    /*                                                                              */
    /********************************************************************************/

@Repository
public interface ProgramLangRepository extends BaseRepository<ProgrammingLanguage,Long>{ 
}
