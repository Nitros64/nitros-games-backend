package com.nitros64.nitros_games_backend.service.impl;

import org.springframework.stereotype.Service;

import com.nitros64.nitros_games_backend.model.entity.ProgrammingTool;
import com.nitros64.nitros_games_backend.repositories.BaseRepository;
import com.nitros64.nitros_games_backend.service.interfaces.ProgramToolService;

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
