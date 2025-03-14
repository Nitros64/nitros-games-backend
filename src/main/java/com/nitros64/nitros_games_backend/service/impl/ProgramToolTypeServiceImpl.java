package com.nitros64.nitros_games_backend.service.impl;

import java.util.List;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import com.nitros64.nitros_games_backend.model.entity.ProgramToolType;
import com.nitros64.nitros_games_backend.repositories.BaseRepository;
import com.nitros64.nitros_games_backend.service.interfaces.ProgramToolTypeService;

    /********************************************************************************
     *          PROGRAMMING TOOL TYPE SERVICE IMPLEMENTATION                        *
     ********************************************************************************/

@Service
public class ProgramToolTypeServiceImpl extends BaseServiceImpl<ProgramToolType,Long> implements ProgramToolTypeService {
    
//    @Autowired
//    private ProgramToolTypeRepository programtooltyperepo;
            
    public ProgramToolTypeServiceImpl(BaseRepository<ProgramToolType,Long> baseRepository){
        super(baseRepository);
    }

    @Override
    @Transactional
    public void addAll(List<ProgramToolType> programtooltype) throws Exception {        
        this.baseRepository.saveAll(programtooltype);
    }
}
