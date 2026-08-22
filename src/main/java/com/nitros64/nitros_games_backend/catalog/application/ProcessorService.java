package com.nitros64.nitros_games_backend.catalog.application;

import org.springframework.stereotype.Service;

import com.nitros64.nitros_games_backend.catalog.domain.Processor;
import com.nitros64.nitros_games_backend.shared.application.BaseServiceImpl;
import com.nitros64.nitros_games_backend.shared.persistence.BaseRepository;

/********************************************************************************
 *                      PROCESSOR SERVICE IMPLEMENTATION                        *
 ********************************************************************************/

@Service
public class ProcessorService extends BaseServiceImpl<Processor,Long>{
    public ProcessorService(BaseRepository<Processor, Long> baseRepository) {
        super(baseRepository);
    }
}
