package com.nitros64.nitros_games_backend.catalog.application;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nitros64.nitros_games_backend.catalog.domain.GameGenre;
import com.nitros64.nitros_games_backend.catalog.persistence.GameGenreRepository;
import com.nitros64.nitros_games_backend.shared.application.ResourceNotFoundException;

@Service
public class GameGenreService {

    private final GameGenreRepository genres;

    public GameGenreService(GameGenreRepository genres) {
        this.genres = genres;
    }

    @Transactional(readOnly = true)
    public List<GameGenre> findAll() {
        return genres.findAll();
    }

    @Transactional(readOnly = true)
    public Page<GameGenre> findAll(Pageable pageable) {
        return genres.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<GameGenre> searchByName(String name, Pageable pageable) {
        return genres.findByNameContainingIgnoreCase(name.strip(), pageable);
    }

    @Transactional(readOnly = true)
    public GameGenre findById(Long id) {
        return genres.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entity not found"));
    }

    @Transactional(readOnly = true)
    public List<GameGenre> findAllById(Set<Long> ids) {
        var genresById = genres.findAllById(ids).stream()
                .collect(Collectors.toMap(GameGenre::getId, Function.identity()));
        if (genresById.size() != ids.size()) {
            throw new ResourceNotFoundException("One or more game genres were not found");
        }
        return ids.stream().map(genresById::get).toList();
    }

    @Transactional
    public GameGenre save(GameGenre genre) {
        return genres.save(genre);
    }

    @Transactional
    public List<GameGenre> saveAll(List<GameGenre> genreList) {
        return genres.saveAll(genreList);
    }

    @Transactional
    public GameGenre update(Long id, GameGenre genre) {
        GameGenre existing = findById(id);
        existing.setName(genre.getName());
        return genres.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        if (genres.existsById(id)) {
            genres.deleteById(id);
        }
    }
}
