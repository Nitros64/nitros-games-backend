package com.nitros64.nitros_games_backend.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.nitros64.nitros_games_backend.game.api.DownloadLinkController;
import com.nitros64.nitros_games_backend.game.api.GameController;
import com.nitros64.nitros_games_backend.game.api.GameVersionController;
import com.nitros64.nitros_games_backend.game.api.dto.DownloadLinkRequest;
import com.nitros64.nitros_games_backend.game.api.dto.DownloadLinkResponse;
import com.nitros64.nitros_games_backend.game.api.dto.GameRequest;
import com.nitros64.nitros_games_backend.game.api.dto.GameResponse;
import com.nitros64.nitros_games_backend.game.api.dto.GameVersionRequest;
import com.nitros64.nitros_games_backend.game.api.dto.GameVersionResponse;
import com.nitros64.nitros_games_backend.game.api.mapper.DownloadLinkApiMapper;
import com.nitros64.nitros_games_backend.game.api.mapper.GameApiMapper;
import com.nitros64.nitros_games_backend.game.api.mapper.GameVersionApiMapper;
import com.nitros64.nitros_games_backend.game.application.DownloadLinkApplicationService;
import com.nitros64.nitros_games_backend.game.application.DownloadLinkDetails;
import com.nitros64.nitros_games_backend.game.application.GameApplicationService;
import com.nitros64.nitros_games_backend.game.application.GameDetails;
import com.nitros64.nitros_games_backend.game.application.GameSearchCriteria;
import com.nitros64.nitros_games_backend.game.application.GameVersionApplicationService;
import com.nitros64.nitros_games_backend.game.application.GameVersionDetails;
import com.nitros64.nitros_games_backend.game.application.SaveDownloadLinkCommand;
import com.nitros64.nitros_games_backend.game.application.SaveGameCommand;
import com.nitros64.nitros_games_backend.game.application.SaveGameVersionCommand;
import com.nitros64.nitros_games_backend.game.domain.DownloadLink;
import com.nitros64.nitros_games_backend.game.domain.GameData;
import com.nitros64.nitros_games_backend.game.domain.GameVersion;
import com.nitros64.nitros_games_backend.game.persistence.DownloadLinkRepository;
import com.nitros64.nitros_games_backend.game.persistence.GameDataRepository;
import com.nitros64.nitros_games_backend.game.persistence.GameVersionRepository;

class GameModuleStructureTests {

    private static final String GAME_PACKAGE =
            "com.nitros64.nitros_games_backend.game";

    @Test
    void gameComponentsStayInsideTheirVerticalModule() {
        List<Class<?>> gameTypes = List.of(
                GameData.class, GameVersion.class, DownloadLink.class,
                GameApplicationService.class, GameVersionApplicationService.class,
                DownloadLinkApplicationService.class,
                SaveGameCommand.class, SaveGameVersionCommand.class,
                SaveDownloadLinkCommand.class,
                GameDetails.class, GameVersionDetails.class, DownloadLinkDetails.class,
                GameSearchCriteria.class,
                GameDataRepository.class, GameVersionRepository.class,
                DownloadLinkRepository.class,
                GameController.class, GameVersionController.class,
                DownloadLinkController.class, GameApiMapper.class,
                GameVersionApiMapper.class, DownloadLinkApiMapper.class,
                GameRequest.class, GameResponse.class,
                GameVersionRequest.class, GameVersionResponse.class,
                DownloadLinkRequest.class, DownloadLinkResponse.class);

        assertThat(gameTypes)
                .allSatisfy(type -> assertThat(type.getPackageName())
                        .startsWith(GAME_PACKAGE + "."));
    }

    @Test
    void gameControllersDoNotExposeJpaEntities() {
        Stream.of(
                        GameController.class,
                        GameVersionController.class,
                        DownloadLinkController.class)
                .flatMap(type -> Arrays.stream(type.getDeclaredMethods()))
                .map(method -> method.toGenericString())
                .forEach(signature -> assertThat(signature)
                        .doesNotContain(GAME_PACKAGE + ".domain."));
    }

    @Test
    void eachControllerOwnsOneResourceLevel() {
        assertThat(methodNames(GameController.class))
                .containsExactlyInAnyOrder("findAll", "findAll", "search", "findOne",
                        "create", "update", "delete");
        assertThat(methodNames(GameVersionController.class))
                .containsExactlyInAnyOrder("findAll", "findOne", "create", "update", "delete");
        assertThat(methodNames(DownloadLinkController.class))
                .containsExactlyInAnyOrder("findAll", "findOne", "create", "update", "delete");
    }

    @Test
    void eachControllerDependsOnItsResourceApplicationService() {
        assertThat(GameController.class.getDeclaredConstructors()[0].getParameterTypes())
                .containsExactly(GameApplicationService.class, GameApiMapper.class);
        assertThat(GameVersionController.class.getDeclaredConstructors()[0].getParameterTypes())
                .containsExactly(GameVersionApplicationService.class, GameVersionApiMapper.class);
        assertThat(DownloadLinkController.class.getDeclaredConstructors()[0].getParameterTypes())
                .containsExactly(DownloadLinkApplicationService.class, DownloadLinkApiMapper.class);
    }

    private List<String> methodNames(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
                .map(method -> method.getName())
                .toList();
    }
}
