package com.nitros64.nitros_games_backend.tooling.api.mapper;

import org.springframework.stereotype.Component;

import com.nitros64.nitros_games_backend.tooling.api.dto.ProgrammingToolRequest;
import com.nitros64.nitros_games_backend.tooling.api.dto.ProgrammingToolResponse;
import com.nitros64.nitros_games_backend.tooling.application.SaveProgrammingToolCommand;
import com.nitros64.nitros_games_backend.tooling.domain.ProgrammingTool;

@Component
public class ProgrammingToolApiMapper {

    public SaveProgrammingToolCommand toCommand(ProgrammingToolRequest request) {
        return new SaveProgrammingToolCommand(
                request.name(),
                request.webPage(),
                request.imagefilePath(),
                request.toolTypeId());
    }

    public ProgrammingToolResponse toResponse(ProgrammingTool entity) {
        return new ProgrammingToolResponse(
                entity.getId(),
                entity.getName(),
                entity.getWebPage(),
                entity.getImagefilePath(),
                entity.getToolType().getId());
    }
}
