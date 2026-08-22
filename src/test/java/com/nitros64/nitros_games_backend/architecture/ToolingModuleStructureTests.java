package com.nitros64.nitros_games_backend.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.nitros64.nitros_games_backend.tooling.api.ProgramLanguageController;
import com.nitros64.nitros_games_backend.tooling.api.ProgramToolController;
import com.nitros64.nitros_games_backend.tooling.api.ProgramToolTypeController;
import com.nitros64.nitros_games_backend.tooling.application.ProgramLangService;
import com.nitros64.nitros_games_backend.tooling.application.ProgramLangServiceImpl;
import com.nitros64.nitros_games_backend.tooling.application.ProgramToolService;
import com.nitros64.nitros_games_backend.tooling.application.ProgramToolServiceImpl;
import com.nitros64.nitros_games_backend.tooling.application.ProgramToolTypeService;
import com.nitros64.nitros_games_backend.tooling.application.ProgramToolTypeServiceImpl;
import com.nitros64.nitros_games_backend.tooling.domain.LanguageTool;
import com.nitros64.nitros_games_backend.tooling.domain.LanguageToolId;
import com.nitros64.nitros_games_backend.tooling.domain.ProgrammingLanguage;
import com.nitros64.nitros_games_backend.tooling.domain.ProgrammingTool;
import com.nitros64.nitros_games_backend.tooling.domain.ProgramToolType;
import com.nitros64.nitros_games_backend.tooling.domain.ToolPlatform;
import com.nitros64.nitros_games_backend.tooling.domain.ToolPlatformId;
import com.nitros64.nitros_games_backend.tooling.domain.ToolProcessor;
import com.nitros64.nitros_games_backend.tooling.domain.ToolProcessorId;
import com.nitros64.nitros_games_backend.tooling.persistence.IToolPlatformDao;
import com.nitros64.nitros_games_backend.tooling.persistence.IToolProcessorDao;
import com.nitros64.nitros_games_backend.tooling.persistence.ProgramLangRepository;
import com.nitros64.nitros_games_backend.tooling.persistence.ProgramToolLangRepo;
import com.nitros64.nitros_games_backend.tooling.persistence.ProgramToolRepository;
import com.nitros64.nitros_games_backend.tooling.persistence.ProgramToolTypeRepository;

class ToolingModuleStructureTests {

    private static final String TOOLING_PACKAGE =
            "com.nitros64.nitros_games_backend.tooling";

    @Test
    void toolingComponentsStayInsideTheirVerticalModule() {
        List<Class<?>> toolingTypes = List.of(
                ProgrammingLanguage.class, ProgrammingTool.class, ProgramToolType.class,
                LanguageTool.class, LanguageToolId.class,
                ToolPlatform.class, ToolPlatformId.class,
                ToolProcessor.class, ToolProcessorId.class,
                ProgramLangService.class, ProgramLangServiceImpl.class,
                ProgramToolService.class, ProgramToolServiceImpl.class,
                ProgramToolTypeService.class, ProgramToolTypeServiceImpl.class,
                ProgramLangRepository.class, ProgramToolRepository.class,
                ProgramToolTypeRepository.class, ProgramToolLangRepo.class,
                IToolPlatformDao.class, IToolProcessorDao.class,
                ProgramLanguageController.class, ProgramToolController.class,
                ProgramToolTypeController.class);

        assertThat(toolingTypes)
                .allSatisfy(type -> assertThat(type.getPackageName())
                        .startsWith(TOOLING_PACKAGE + "."));
    }
}
