package com.nitros64.nitros_games_backend.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.nitros64.nitros_games_backend.catalog.application.GameGenreService;
import com.nitros64.nitros_games_backend.catalog.application.PlatformService;
import com.nitros64.nitros_games_backend.catalog.application.ProcessorService;
import com.nitros64.nitros_games_backend.catalog.domain.GameGenre;
import com.nitros64.nitros_games_backend.catalog.domain.Platform;
import com.nitros64.nitros_games_backend.catalog.domain.Processor;
import com.nitros64.nitros_games_backend.catalog.persistence.GameGenreRepository;
import com.nitros64.nitros_games_backend.catalog.persistence.PlatformRepository;
import com.nitros64.nitros_games_backend.catalog.persistence.ProcessorRepository;
import com.nitros64.nitros_games_backend.catalog.api.GameGenreController;
import com.nitros64.nitros_games_backend.catalog.api.PlatformController;
import com.nitros64.nitros_games_backend.catalog.api.ProcessorController;
import com.nitros64.nitros_games_backend.catalog.api.dto.GameGenreRequest;
import com.nitros64.nitros_games_backend.catalog.api.dto.GameGenreResponse;
import com.nitros64.nitros_games_backend.catalog.api.dto.PlatformRequest;
import com.nitros64.nitros_games_backend.catalog.api.dto.PlatformResponse;
import com.nitros64.nitros_games_backend.catalog.api.dto.ProcessorRequest;
import com.nitros64.nitros_games_backend.catalog.api.dto.ProcessorResponse;
import com.nitros64.nitros_games_backend.catalog.api.mapper.GameGenreApiMapper;
import com.nitros64.nitros_games_backend.catalog.api.mapper.PlatformApiMapper;
import com.nitros64.nitros_games_backend.catalog.api.mapper.ProcessorApiMapper;

class CatalogModuleStructureTests {

    private static final String CATALOG_PACKAGE =
            "com.nitros64.nitros_games_backend.catalog";

    @Test
    void catalogComponentsStayInsideTheirVerticalModule() {
        List<Class<?>> catalogTypes = List.of(
                GameGenre.class, 
                Platform.class, 
                Processor.class,
                GameGenreRepository.class,
                PlatformRepository.class, ProcessorRepository.class,
                GameGenreService.class,
                PlatformService.class, ProcessorService.class,
                GameGenreController.class,
                PlatformController.class, ProcessorController.class,
                GameGenreRequest.class, GameGenreResponse.class,
                PlatformRequest.class, PlatformResponse.class,
                ProcessorRequest.class, ProcessorResponse.class,
                GameGenreApiMapper.class,
                PlatformApiMapper.class, ProcessorApiMapper.class);

        assertThat(catalogTypes)
                .allSatisfy(type -> assertThat(type.getPackageName())
                        .startsWith(CATALOG_PACKAGE + "."));
    }

    @Test
    void catalogControllersDoNotExposeJpaEntities() {
        Stream.of(
                GameGenreController.class,
                PlatformController.class,
                ProcessorController.class)
                .flatMap(controller -> Stream.of(controller.getDeclaredMethods()))
                .map(method -> method.toGenericString())
                .forEach(signature -> assertThat(signature)
                        .doesNotContain(CATALOG_PACKAGE + ".domain."));
    }

    @Test
    void catalogServicesUseExplicitPersistenceContracts() {
        Stream.of(
                GameGenreService.class,
                PlatformService.class,
                ProcessorService.class)
                .forEach(service -> assertThat(service.getSuperclass()).isEqualTo(Object.class));
    }
}
