package com.nitros64.nitros_games_backend.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.nitros64.nitros_games_backend.tooling.api.ProgramLanguageController;
import com.nitros64.nitros_games_backend.tooling.api.ProgramToolController;
import com.nitros64.nitros_games_backend.tooling.api.ProgramToolTypeController;
import com.nitros64.nitros_games_backend.tooling.api.dto.ProgramToolTypeRequest;
import com.nitros64.nitros_games_backend.tooling.api.dto.ProgramToolTypeResponse;
import com.nitros64.nitros_games_backend.tooling.api.dto.ProgrammingLanguageRequest;
import com.nitros64.nitros_games_backend.tooling.api.dto.ProgrammingLanguageResponse;
import com.nitros64.nitros_games_backend.tooling.api.dto.ProgrammingToolRequest;
import com.nitros64.nitros_games_backend.tooling.api.dto.ProgrammingToolResponse;
import com.nitros64.nitros_games_backend.tooling.api.mapper.ProgramToolTypeApiMapper;
import com.nitros64.nitros_games_backend.tooling.api.mapper.ProgrammingLanguageApiMapper;
import com.nitros64.nitros_games_backend.tooling.api.mapper.ProgrammingToolApiMapper;
import com.nitros64.nitros_games_backend.tooling.application.ProgramLangService;
import com.nitros64.nitros_games_backend.tooling.application.ProgramLangServiceImpl;
import com.nitros64.nitros_games_backend.tooling.application.ProgramToolService;
import com.nitros64.nitros_games_backend.tooling.application.ProgramToolServiceImpl;
import com.nitros64.nitros_games_backend.tooling.application.ProgramToolTypeService;
import com.nitros64.nitros_games_backend.tooling.application.ProgramToolTypeServiceImpl;
import com.nitros64.nitros_games_backend.tooling.application.ProgrammingToolSearchCriteria;
import com.nitros64.nitros_games_backend.tooling.application.SaveProgrammingToolCommand;
import com.nitros64.nitros_games_backend.tooling.application.ToolCompatibility;
import com.nitros64.nitros_games_backend.tooling.application.ToolCompatibilityService;
import com.nitros64.nitros_games_backend.tooling.domain.LanguageTool;
import com.nitros64.nitros_games_backend.tooling.domain.LanguageToolId;
import com.nitros64.nitros_games_backend.tooling.domain.ProgrammingLanguage;
import com.nitros64.nitros_games_backend.tooling.domain.ProgrammingTool;
import com.nitros64.nitros_games_backend.tooling.domain.ProgramToolType;
import com.nitros64.nitros_games_backend.tooling.persistence.LanguageToolRepository;
import com.nitros64.nitros_games_backend.tooling.persistence.ProgrammingLanguageRepository;
import com.nitros64.nitros_games_backend.tooling.persistence.ProgrammingToolRepository;
import com.nitros64.nitros_games_backend.tooling.persistence.ProgramToolTypeRepository;

class ToolingModuleStructureTests {

    private static final String TOOLING_PACKAGE =
            "com.nitros64.nitros_games_backend.tooling";

    @Test
    void toolingComponentsStayInsideTheirVerticalModule() {
        List<Class<?>> toolingTypes = List.of(
                ProgrammingLanguage.class, ProgrammingTool.class, ProgramToolType.class,
                LanguageTool.class, LanguageToolId.class,
                ProgramLangService.class, ProgramLangServiceImpl.class,
                ProgramToolService.class, ProgramToolServiceImpl.class,
                ProgramToolTypeService.class, ProgramToolTypeServiceImpl.class,
                ProgrammingLanguageRepository.class, ProgrammingToolRepository.class,
                ProgramToolTypeRepository.class, LanguageToolRepository.class,
                ProgramLanguageController.class, ProgramToolController.class,
                ProgramToolTypeController.class,
                ProgrammingLanguageRequest.class, ProgrammingLanguageResponse.class,
                ProgramToolTypeRequest.class, ProgramToolTypeResponse.class,
                ProgrammingToolRequest.class, ProgrammingToolResponse.class,
                ProgrammingLanguageApiMapper.class, ProgramToolTypeApiMapper.class,
                ProgrammingToolApiMapper.class, SaveProgrammingToolCommand.class,
                ProgrammingToolSearchCriteria.class,
                ToolCompatibility.class, ToolCompatibilityService.class);

        assertThat(toolingTypes)
                .allSatisfy(type -> assertThat(type.getPackageName())
                        .startsWith(TOOLING_PACKAGE + "."));
    }

    @Test
    void toolingControllersDoNotExposeJpaEntities() {
        Stream.of(
                ProgramLanguageController.class,
                ProgramToolTypeController.class,
                ProgramToolController.class)
                .flatMap(controller -> Stream.of(controller.getDeclaredMethods()))
                .map(method -> method.toGenericString())
                .forEach(signature -> assertThat(signature)
                        .doesNotContain(TOOLING_PACKAGE + ".domain."));
    }
}
