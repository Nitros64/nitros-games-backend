package com.nitros64.nitros_games_backend.tooling.application;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nitros64.nitros_games_backend.shared.application.ResourceNotFoundException;
import com.nitros64.nitros_games_backend.tooling.domain.LanguageTool;
import com.nitros64.nitros_games_backend.tooling.domain.LanguageToolId;
import com.nitros64.nitros_games_backend.tooling.persistence.LanguageToolRepository;

@Service
public class LanguageToolService {

    private final LanguageToolRepository languageTools;
    private final ProgramLangService languages;
    private final ProgramToolService tools;

    public LanguageToolService(
            LanguageToolRepository languageTools, ProgramLangService languages, ProgramToolService tools) {
        this.languageTools = languageTools;
        this.languages = languages;
        this.tools = tools;
    }

    @Transactional(readOnly = true)
    public List<LanguageTool> findAll() {
        return languageTools.findAll(Sort.by("programmingLanguage.id", "programmingTool.id"));
    }

    @Transactional
    public LanguageTool create(Long languageId, Long toolId) {
        var language = languages.findById(languageId);
        var tool = tools.findById(toolId);
        languageTools.insert(languageId, toolId);
        return new LanguageTool(language, tool);
    }

    @Transactional
    public void delete(Long languageId, Long toolId) {
        var association = languageTools.findById(new LanguageToolId(languageId, toolId))
                .orElseThrow(() -> new ResourceNotFoundException("Language-tool association not found"));
        languageTools.delete(association);
        // The existing composite FK rejects deletion if any version uses this pair,
        // including a version inserted concurrently. The shared handler returns 409.
        languageTools.flush();
    }
}
