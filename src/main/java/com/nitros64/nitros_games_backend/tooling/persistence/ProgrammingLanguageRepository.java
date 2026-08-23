package com.nitros64.nitros_games_backend.tooling.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.nitros64.nitros_games_backend.tooling.domain.ProgrammingLanguage;

public interface ProgrammingLanguageRepository
        extends JpaRepository<ProgrammingLanguage, Long> {

    Page<ProgrammingLanguage> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
