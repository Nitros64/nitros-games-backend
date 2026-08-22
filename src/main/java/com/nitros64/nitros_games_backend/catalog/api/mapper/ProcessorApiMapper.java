package com.nitros64.nitros_games_backend.catalog.api.mapper;

import org.springframework.stereotype.Component;

import com.nitros64.nitros_games_backend.catalog.api.dto.ProcessorRequest;
import com.nitros64.nitros_games_backend.catalog.api.dto.ProcessorResponse;
import com.nitros64.nitros_games_backend.catalog.domain.Processor;

@Component
public class ProcessorApiMapper {

    public Processor toEntity(ProcessorRequest request) {
        return new Processor(request.name());
    }

    public ProcessorResponse toResponse(Processor entity) {
        return new ProcessorResponse(entity.getId(), entity.getName());
    }
}
