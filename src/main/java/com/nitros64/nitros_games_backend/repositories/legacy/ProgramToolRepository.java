package com.nitros64.nitros_games_backend.repositories.legacy;

import org.springframework.stereotype.Repository;

import com.nitros64.nitros_games_backend.model.entity.ProgrammingTool;
import com.nitros64.nitros_games_backend.repositories.BaseRepository;

    /********************************************************************************/
    /*                                                                              */
    /*                      PROGRAMMING TOOL REPOSITORY                             */
    /*                                                                              */
    /********************************************************************************/

@Repository
public interface ProgramToolRepository extends BaseRepository<ProgrammingTool,Long>{
    
}
