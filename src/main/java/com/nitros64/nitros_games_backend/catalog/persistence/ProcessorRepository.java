package com.nitros64.nitros_games_backend.catalog.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.nitros64.nitros_games_backend.catalog.domain.Processor;
import com.nitros64.nitros_games_backend.shared.persistence.BaseRepository;

public interface ProcessorRepository extends BaseRepository<Processor, Long> {

    Page<Processor> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
