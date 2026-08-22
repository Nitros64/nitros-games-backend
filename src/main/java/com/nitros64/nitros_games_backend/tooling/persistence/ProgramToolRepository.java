package com.nitros64.nitros_games_backend.tooling.persistence;

import org.springframework.stereotype.Repository;

import com.nitros64.nitros_games_backend.tooling.domain.ProgrammingTool;
import com.nitros64.nitros_games_backend.shared.persistence.BaseRepository;

    /********************************************************************************/
    /*                                                                              */
    /*                      PROGRAMMING TOOL REPOSITORY                             */
    /*                                                                              */
    /********************************************************************************/

@Repository
public interface ProgramToolRepository extends BaseRepository<ProgrammingTool,Long>{
    
}
