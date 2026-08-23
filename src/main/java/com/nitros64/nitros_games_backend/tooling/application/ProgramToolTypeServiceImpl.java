package com.nitros64.nitros_games_backend.tooling.application;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nitros64.nitros_games_backend.shared.application.ResourceNotFoundException;
import com.nitros64.nitros_games_backend.tooling.domain.ProgramToolType;
import com.nitros64.nitros_games_backend.tooling.persistence.ProgramToolTypeRepository;

@Service
public class ProgramToolTypeServiceImpl implements ProgramToolTypeService {

    private final ProgramToolTypeRepository toolTypes;

    public ProgramToolTypeServiceImpl(ProgramToolTypeRepository toolTypes) {
        this.toolTypes = toolTypes;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProgramToolType> findAll() {
        return toolTypes.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProgramToolType> findAll(Pageable pageable) {
        return toolTypes.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProgramToolType> searchByName(String name, Pageable pageable) {
        return toolTypes.findByNameContainingIgnoreCase(name.strip(), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public ProgramToolType findById(Long id) {
        return toolTypes.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Programming tool type not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, ProgramToolType> findByIds(Set<Long> ids) {
        Map<Long, ProgramToolType> resolved = toolTypes.findAllById(ids).stream()
                .collect(Collectors.toMap(ProgramToolType::getId, Function.identity()));
        if (resolved.size() != ids.size()) {
            throw new ResourceNotFoundException("Programming tool type not found");
        }
        return resolved;
    }

    @Override
    @Transactional
    public ProgramToolType save(ProgramToolType type) {
        return toolTypes.save(type);
    }

    @Override
    @Transactional
    public List<ProgramToolType> saveAll(List<ProgramToolType> types) {
        return toolTypes.saveAll(types);
    }

    @Override
    @Transactional
    public ProgramToolType update(Long id, ProgramToolType type) {
        ProgramToolType existing = findById(id);
        existing.rename(type.getName());
        return toolTypes.save(existing);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        toolTypes.delete(findById(id));
    }
}
