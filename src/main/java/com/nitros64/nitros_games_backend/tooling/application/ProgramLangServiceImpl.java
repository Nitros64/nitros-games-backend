package com.nitros64.nitros_games_backend.tooling.application;

import java.util.List;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import com.nitros64.nitros_games_backend.tooling.domain.ProgrammingLanguage;
import com.nitros64.nitros_games_backend.repositories.BaseRepository;
import com.nitros64.nitros_games_backend.service.impl.BaseServiceImpl;

    /********************************************************************************
     *          PROGRAMMING LANGUAGE SERVICE IMPLEMENTATION                         *
     ********************************************************************************/

@Service
public class ProgramLangServiceImpl extends BaseServiceImpl<ProgrammingLanguage,Long>
            implements ProgramLangService {
  
    public ProgramLangServiceImpl(BaseRepository<ProgrammingLanguage,Long> baseRepository){
        super(baseRepository);
    }

    @Override
    @Transactional
    public void addAll(List<ProgrammingLanguage> programlangs) throws Exception {
        this.baseRepository.saveAll(programlangs);
    }
}
