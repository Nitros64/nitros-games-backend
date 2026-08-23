package com.nitros64.nitros_games_backend.catalog.application;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nitros64.nitros_games_backend.catalog.domain.Processor;
import com.nitros64.nitros_games_backend.catalog.persistence.ProcessorRepository;
import com.nitros64.nitros_games_backend.shared.application.ResourceNotFoundException;

@Service
public class ProcessorService {

    private final ProcessorRepository processors;

    public ProcessorService(ProcessorRepository processors) {
        this.processors = processors;
    }

    @Transactional(readOnly = true)
    public List<Processor> findAll() {
        return processors.findAll();
    }

    @Transactional(readOnly = true)
    public Page<Processor> findAll(Pageable pageable) {
        return processors.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Processor> searchByName(String name, Pageable pageable) {
        return processors.findByNameContainingIgnoreCase(name.strip(), pageable);
    }

    @Transactional(readOnly = true)
    public Processor findById(Long id) {
        return processors.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entity not found"));
    }

    @Transactional
    public Processor save(Processor processor) {
        return processors.save(processor);
    }

    @Transactional
    public List<Processor> saveAll(List<Processor> processorList) {
        return processors.saveAll(processorList);
    }

    @Transactional
    public Processor update(Long id, Processor processor) {
        Processor existing = findById(id);
        existing.setName(processor.getName());
        return processors.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        if (processors.existsById(id)) {
            processors.deleteById(id);
        }
    }
}
