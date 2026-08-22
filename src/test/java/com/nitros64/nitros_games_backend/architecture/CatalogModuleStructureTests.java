package com.nitros64.nitros_games_backend.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.nitros64.nitros_games_backend.catalog.application.DevelopDifficultyService;
import com.nitros64.nitros_games_backend.catalog.application.GameGenreService;
import com.nitros64.nitros_games_backend.catalog.application.PlatformService;
import com.nitros64.nitros_games_backend.catalog.application.ProcessorService;
import com.nitros64.nitros_games_backend.catalog.domain.DevelopmentDifficulty;
import com.nitros64.nitros_games_backend.catalog.domain.GameGenre;
import com.nitros64.nitros_games_backend.catalog.domain.Platform;
import com.nitros64.nitros_games_backend.catalog.domain.Processor;
import com.nitros64.nitros_games_backend.catalog.persistence.DevDifficultyRepository;
import com.nitros64.nitros_games_backend.catalog.persistence.GenreRepository;
import com.nitros64.nitros_games_backend.catalog.persistence.PlatformRepository;
import com.nitros64.nitros_games_backend.catalog.persistence.ProcessorRepository;
import com.nitros64.nitros_games_backend.catalog.web.DevelopmentDifficultyController;
import com.nitros64.nitros_games_backend.catalog.web.GameGenreController;
import com.nitros64.nitros_games_backend.catalog.web.PlatformController;
import com.nitros64.nitros_games_backend.catalog.web.ProcessorController;

class CatalogModuleStructureTests {

    private static final String CATALOG_PACKAGE =
            "com.nitros64.nitros_games_backend.catalog";

    @Test
    void catalogComponentsStayInsideTheirVerticalModule() {
        List<Class<?>> catalogTypes = List.of(
                DevelopmentDifficulty.class, GameGenre.class, Platform.class, Processor.class,
                DevDifficultyRepository.class, GenreRepository.class,
                PlatformRepository.class, ProcessorRepository.class,
                DevelopDifficultyService.class, GameGenreService.class,
                PlatformService.class, ProcessorService.class,
                DevelopmentDifficultyController.class, GameGenreController.class,
                PlatformController.class, ProcessorController.class);

        assertThat(catalogTypes)
                .allSatisfy(type -> assertThat(type.getPackageName())
                        .startsWith(CATALOG_PACKAGE + "."));
    }
}
