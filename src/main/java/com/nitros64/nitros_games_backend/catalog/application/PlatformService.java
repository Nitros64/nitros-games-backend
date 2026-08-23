package com.nitros64.nitros_games_backend.catalog.application;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nitros64.nitros_games_backend.catalog.domain.Platform;
import com.nitros64.nitros_games_backend.catalog.persistence.PlatformRepository;
import com.nitros64.nitros_games_backend.shared.application.ResourceNotFoundException;

@Service
public class PlatformService {

    private final PlatformRepository platforms;

    public PlatformService(PlatformRepository platforms) {
        this.platforms = platforms;
    }

    @Transactional(readOnly = true)
    public List<Platform> findAll() {
        return platforms.findAll();
    }

    @Transactional(readOnly = true)
    public Page<Platform> findAll(Pageable pageable) {
        return platforms.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Platform> searchByName(String name, Pageable pageable) {
        return platforms.findByNameContainingIgnoreCase(name.strip(), pageable);
    }

    @Transactional(readOnly = true)
    public Platform findById(Long id) {
        return platforms.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entity not found"));
    }

    @Transactional
    public Platform save(Platform platform) {
        return platforms.save(platform);
    }

    @Transactional
    public List<Platform> saveAll(List<Platform> platformList) {
        return platforms.saveAll(platformList);
    }

    @Transactional
    public Platform update(Long id, Platform platform) {
        Platform existing = findById(id);
        existing.rename(platform.getName());
        return platforms.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        if (platforms.existsById(id)) {
            platforms.deleteById(id);
        }
    }
}
