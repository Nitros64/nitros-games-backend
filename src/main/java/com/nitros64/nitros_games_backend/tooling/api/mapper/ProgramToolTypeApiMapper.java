package com.nitros64.nitros_games_backend.tooling.api.mapper;

import org.springframework.stereotype.Component;

import com.nitros64.nitros_games_backend.tooling.api.dto.ProgramToolTypeRequest;
import com.nitros64.nitros_games_backend.tooling.api.dto.ProgramToolTypeResponse;
import com.nitros64.nitros_games_backend.tooling.domain.ProgramToolType;

@Component
public class ProgramToolTypeApiMapper {

    public ProgramToolType toEntity(ProgramToolTypeRequest request) {
        return new ProgramToolType(request.name());
    }

    public ProgramToolTypeResponse toResponse(ProgramToolType entity) {
        return new ProgramToolTypeResponse(entity.getId(), entity.getName());
    }
}
