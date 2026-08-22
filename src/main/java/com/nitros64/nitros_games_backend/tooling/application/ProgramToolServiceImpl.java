package com.nitros64.nitros_games_backend.tooling.application;

import org.springframework.stereotype.Service;

import com.nitros64.nitros_games_backend.tooling.domain.ProgrammingTool;
import com.nitros64.nitros_games_backend.shared.persistence.BaseRepository;
import com.nitros64.nitros_games_backend.shared.application.BaseServiceImpl;

@Service
public class ProgramToolServiceImpl extends BaseServiceImpl<ProgrammingTool,Long> implements ProgramToolService {
    
//    @Autowired 
//    private ProgramToolRepository pToolDao;
//    
//    @Autowired 
//    private ProgramToolTypeRepository programToolTypeDao;
    
    public ProgramToolServiceImpl(BaseRepository<ProgrammingTool,Long> baseRepository){
        super(baseRepository);
    }
    
}
