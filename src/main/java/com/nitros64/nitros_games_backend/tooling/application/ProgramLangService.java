package com.nitros64.nitros_games_backend.tooling.application;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.nitros64.nitros_games_backend.tooling.domain.ProgrammingLanguage;

public interface ProgramLangService {

    List<ProgrammingLanguage> findAll();

    Page<ProgrammingLanguage> findAll(Pageable pageable);

    Page<ProgrammingLanguage> searchByName(String name, Pageable pageable);

    ProgrammingLanguage findById(Long id);

    ProgrammingLanguage save(ProgrammingLanguage language);

    List<ProgrammingLanguage> saveAll(List<ProgrammingLanguage> languages);

    ProgrammingLanguage update(Long id, ProgrammingLanguage language);

    void delete(Long id);
}
