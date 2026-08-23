package com.nitros64.nitros_games_backend.tooling.application;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nitros64.nitros_games_backend.shared.application.ResourceNotFoundException;
import com.nitros64.nitros_games_backend.tooling.domain.ProgrammingLanguage;
import com.nitros64.nitros_games_backend.tooling.persistence.ProgrammingLanguageRepository;

@Service
public class ProgramLangServiceImpl implements ProgramLangService {

    private final ProgrammingLanguageRepository languages;

    public ProgramLangServiceImpl(ProgrammingLanguageRepository languages) {
        this.languages = languages;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProgrammingLanguage> findAll() {
        return languages.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProgrammingLanguage> findAll(Pageable pageable) {
        return languages.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProgrammingLanguage> searchByName(String name, Pageable pageable) {
        return languages.findByNameContainingIgnoreCase(name.strip(), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public ProgrammingLanguage findById(Long id) {
        return languages.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Programming language not found"));
    }

    @Override
    @Transactional
    public ProgrammingLanguage save(ProgrammingLanguage language) {
        return languages.save(language);
    }

    @Override
    @Transactional
    public List<ProgrammingLanguage> saveAll(List<ProgrammingLanguage> languageList) {
        return languages.saveAll(languageList);
    }

    @Override
    @Transactional
    public ProgrammingLanguage update(Long id, ProgrammingLanguage language) {
        ProgrammingLanguage existing = findById(id);
        existing.setName(language.getName());
        return languages.save(existing);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        languages.delete(findById(id));
    }
}
