package com.nitros64.nitros_games_backend.tooling.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nitros64.nitros_games_backend.catalog.application.PlatformService;
import com.nitros64.nitros_games_backend.catalog.application.ProcessorService;
import com.nitros64.nitros_games_backend.shared.application.ResourceNotFoundException;
import com.nitros64.nitros_games_backend.tooling.domain.LanguageToolId;
import com.nitros64.nitros_games_backend.tooling.persistence.LanguageToolRepository;

@Service
public class ToolCompatibilityService {

    private final LanguageToolRepository languageTools;
    private final PlatformService platforms;
    private final ProcessorService processors;

    public ToolCompatibilityService(
            LanguageToolRepository languageTools,
            PlatformService platforms,
            ProcessorService processors) {
        this.languageTools = languageTools;
        this.platforms = platforms;
        this.processors = processors;
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
        return new ToolCompatibility(
                languageTool,
                platforms.findById(platformId),
                processors.findById(processorId));
    }
}
