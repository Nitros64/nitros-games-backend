package com.nitros64.nitros_games_backend.tooling.application;

import org.springframework.stereotype.Service;

import com.nitros64.nitros_games_backend.tooling.domain.ProgramToolType;
import com.nitros64.nitros_games_backend.shared.persistence.BaseRepository;
import com.nitros64.nitros_games_backend.shared.application.BaseServiceImpl;

    /********************************************************************************
     *          PROGRAMMING TOOL TYPE SERVICE IMPLEMENTATION                        *
     ********************************************************************************/

@Service
public class ProgramToolTypeServiceImpl extends BaseServiceImpl<ProgramToolType,Long> implements ProgramToolTypeService {
    
    public ProgramToolTypeServiceImpl(BaseRepository<ProgramToolType,Long> baseRepository){
        super(baseRepository);
    }
}
