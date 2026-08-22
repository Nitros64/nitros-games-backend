
package com.nitros64.nitros_games_backend.tooling.persistence;

import org.springframework.stereotype.Repository;

import com.nitros64.nitros_games_backend.tooling.domain.ProgramToolType;
import com.nitros64.nitros_games_backend.shared.persistence.BaseRepository;

    /********************************************************************************/
    /*                                                                              */
    /*                  PROGRAMMING TOOL TYPE REPOSITORY                            */
    /*                                                                              */
    /********************************************************************************/

@Repository
public interface ProgramToolTypeRepository extends BaseRepository<ProgramToolType,Long>{
    
}
