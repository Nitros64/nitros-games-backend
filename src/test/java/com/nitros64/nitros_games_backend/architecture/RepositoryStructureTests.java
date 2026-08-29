package com.nitros64.nitros_games_backend.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

import com.nitros64.nitros_games_backend.catalog.persistence.GameGenreRepository;
import com.nitros64.nitros_games_backend.catalog.persistence.PlatformRepository;
import com.nitros64.nitros_games_backend.catalog.persistence.ProcessorRepository;
import com.nitros64.nitros_games_backend.game.persistence.DownloadLinkRepository;
import com.nitros64.nitros_games_backend.game.persistence.GameDataRepository;
import com.nitros64.nitros_games_backend.game.persistence.GameVersionRepository;
import com.nitros64.nitros_games_backend.storage.persistence.ServerHostImageRepository;
import com.nitros64.nitros_games_backend.tooling.persistence.LanguageToolRepository;
import com.nitros64.nitros_games_backend.tooling.persistence.ProgrammingLanguageRepository;
import com.nitros64.nitros_games_backend.tooling.persistence.ProgrammingToolRepository;
import com.nitros64.nitros_games_backend.tooling.persistence.ProgramToolTypeRepository;
import com.nitros64.nitros_games_backend.tooling.persistence.ToolPlatformRepository;
import com.nitros64.nitros_games_backend.tooling.persistence.ToolProcessorRepository;

class RepositoryStructureTests {

    @Test
    void repositoriesDeclareTheirSpringDataContractDirectly() {
        Stream.of(
                GameGenreRepository.class,
                PlatformRepository.class,
                ProcessorRepository.class,
                DownloadLinkRepository.class,
                GameDataRepository.class,
                GameVersionRepository.class,
                ServerHostImageRepository.class,
                LanguageToolRepository.class,
                ProgrammingLanguageRepository.class,
                ProgrammingToolRepository.class,
                ProgramToolTypeRepository.class,
                ToolPlatformRepository.class,
                ToolProcessorRepository.class)
                .forEach(repository -> assertThat(repository.getInterfaces())
                        .containsExactly(JpaRepository.class));
    }
}
