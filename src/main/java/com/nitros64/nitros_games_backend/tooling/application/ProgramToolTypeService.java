package com.nitros64.nitros_games_backend.tooling.application;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.nitros64.nitros_games_backend.tooling.domain.ProgramToolType;

public interface ProgramToolTypeService {

    List<ProgramToolType> findAll();

    Page<ProgramToolType> findAll(Pageable pageable);

    Page<ProgramToolType> searchByName(String name, Pageable pageable);

    ProgramToolType findById(Long id);

    Map<Long, ProgramToolType> findByIds(Set<Long> ids);

    ProgramToolType save(ProgramToolType type);

    List<ProgramToolType> saveAll(List<ProgramToolType> types);

    ProgramToolType update(Long id, ProgramToolType type);

    void delete(Long id);
}
