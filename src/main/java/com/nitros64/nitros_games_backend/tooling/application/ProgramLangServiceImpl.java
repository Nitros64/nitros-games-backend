package com.nitros64.nitros_games_backend.tooling.application;

import org.springframework.stereotype.Service;

import com.nitros64.nitros_games_backend.tooling.domain.ProgrammingLanguage;
import com.nitros64.nitros_games_backend.shared.persistence.BaseRepository;
import com.nitros64.nitros_games_backend.shared.application.BaseServiceImpl;

    /********************************************************************************
     *          PROGRAMMING LANGUAGE SERVICE IMPLEMENTATION                         *
     ********************************************************************************/

@Service
public class ProgramLangServiceImpl extends BaseServiceImpl<ProgrammingLanguage,Long>
            implements ProgramLangService {
  
    public ProgramLangServiceImpl(BaseRepository<ProgrammingLanguage,Long> baseRepository){
        super(baseRepository);
    }
}
