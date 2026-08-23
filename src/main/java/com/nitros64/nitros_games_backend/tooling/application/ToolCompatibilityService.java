package com.nitros64.nitros_games_backend.tooling.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nitros64.nitros_games_backend.shared.application.ResourceNotFoundException;
import com.nitros64.nitros_games_backend.tooling.domain.LanguageToolId;
import com.nitros64.nitros_games_backend.tooling.domain.ToolPlatformId;
import com.nitros64.nitros_games_backend.tooling.domain.ToolProcessorId;
import com.nitros64.nitros_games_backend.tooling.persistence.LanguageToolRepository;
import com.nitros64.nitros_games_backend.tooling.persistence.ToolPlatformRepository;
import com.nitros64.nitros_games_backend.tooling.persistence.ToolProcessorRepository;

@Service
public class ToolCompatibilityService {

    private final LanguageToolRepository languageTools;
    private final ToolPlatformRepository toolPlatforms;
    private final ToolProcessorRepository toolProcessors;

    public ToolCompatibilityService(
            LanguageToolRepository languageTools,
            ToolPlatformRepository toolPlatforms,
            ToolProcessorRepository toolProcessors) {
        this.languageTools = languageTools;
        this.toolPlatforms = toolPlatforms;
        this.toolProcessors = toolProcessors;
    }

    @Transactional(readOnly = true)
    public ToolCompatibility resolve(
            Long languageId,
            Long toolId,
            Long platformId,
            Long processorId) {
        var languageTool = languageTools.findById(new LanguageToolId(languageId, toolId))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "The language is not supported by the selected tool"));
        var toolPlatform = toolPlatforms.findById(new ToolPlatformId(toolId, platformId))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "The platform is not supported by the selected tool"));
        var toolProcessor = toolProcessors.findById(new ToolProcessorId(toolId, processorId))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "The processor is not supported by the selected tool"));
        return new ToolCompatibility(languageTool, toolPlatform, toolProcessor);
    }
}
