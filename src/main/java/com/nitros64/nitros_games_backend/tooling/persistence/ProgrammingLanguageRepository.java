package com.nitros64.nitros_games_backend.tooling.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.nitros64.nitros_games_backend.shared.persistence.BaseRepository;
import com.nitros64.nitros_games_backend.tooling.domain.ProgrammingLanguage;

public interface ProgrammingLanguageRepository
        extends BaseRepository<ProgrammingLanguage, Long> {

    Page<ProgrammingLanguage> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
