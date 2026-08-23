package com.nitros64.nitros_games_backend.catalog.application;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nitros64.nitros_games_backend.catalog.domain.DevelopmentDifficulty;
import com.nitros64.nitros_games_backend.catalog.persistence.DevelopmentDifficultyRepository;
import com.nitros64.nitros_games_backend.shared.application.ResourceNotFoundException;

@Service
public class DevelopmentDifficultyService {

    private final DevelopmentDifficultyRepository difficulties;

    public DevelopmentDifficultyService(DevelopmentDifficultyRepository difficulties) {
        this.difficulties = difficulties;
    }

    @Transactional(readOnly = true)
    public List<DevelopmentDifficulty> findAll() {
        return difficulties.findAll();
    }

    @Transactional(readOnly = true)
    public Page<DevelopmentDifficulty> findAll(Pageable pageable) {
        return difficulties.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<DevelopmentDifficulty> searchByName(String name, Pageable pageable) {
        return difficulties.findByNameContainingIgnoreCase(name.strip(), pageable);
    }

    @Transactional(readOnly = true)
    public DevelopmentDifficulty findById(Long id) {
        return difficulties.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entity not found"));
    }

    @Transactional
    public DevelopmentDifficulty save(DevelopmentDifficulty difficulty) {
        return difficulties.save(difficulty);
    }

    @Transactional
    public List<DevelopmentDifficulty> saveAll(List<DevelopmentDifficulty> difficultyList) {
        return difficulties.saveAll(difficultyList);
    }

    @Transactional
    public DevelopmentDifficulty update(Long id, DevelopmentDifficulty difficulty) {
        DevelopmentDifficulty existing = findById(id);
        existing.setName(difficulty.getName());
        return difficulties.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        if (difficulties.existsById(id)) {
            difficulties.deleteById(id);
        }
    }
}
