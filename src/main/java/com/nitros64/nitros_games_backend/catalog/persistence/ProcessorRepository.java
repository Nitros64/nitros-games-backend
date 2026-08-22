package com.nitros64.nitros_games_backend.catalog.persistence;

import org.springframework.stereotype.Repository;

import com.nitros64.nitros_games_backend.catalog.domain.Processor;
import com.nitros64.nitros_games_backend.shared.persistence.BaseRepository;

@Repository
public interface ProcessorRepository extends BaseRepository<Processor,Long>{    
}
