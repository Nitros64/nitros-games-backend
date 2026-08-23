package com.nitros64.nitros_games_backend.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.nitros64.nitros_games_backend.game.api.GameController;
import com.nitros64.nitros_games_backend.game.api.dto.DownloadLinkRequest;
import com.nitros64.nitros_games_backend.game.api.dto.DownloadLinkResponse;
import com.nitros64.nitros_games_backend.game.api.dto.GameRequest;
import com.nitros64.nitros_games_backend.game.api.dto.GameResponse;
import com.nitros64.nitros_games_backend.game.api.dto.GameVersionRequest;
import com.nitros64.nitros_games_backend.game.api.dto.GameVersionResponse;
import com.nitros64.nitros_games_backend.game.api.mapper.GameApiMapper;
import com.nitros64.nitros_games_backend.game.application.DownloadLinkDetails;
import com.nitros64.nitros_games_backend.game.application.GameApplicationService;
import com.nitros64.nitros_games_backend.game.application.GameDetails;
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
                GameApplicationService.class,
                SaveGameCommand.class, SaveGameVersionCommand.class,
                SaveDownloadLinkCommand.class,
                GameDetails.class, GameVersionDetails.class, DownloadLinkDetails.class,
                GameDataRepository.class, GameVersionRepository.class,
                DownloadLinkRepository.class,
                GameController.class, GameApiMapper.class,
                GameRequest.class, GameResponse.class,
                GameVersionRequest.class, GameVersionResponse.class,
                DownloadLinkRequest.class, DownloadLinkResponse.class);

        assertThat(gameTypes)
                .allSatisfy(type -> assertThat(type.getPackageName())
                        .startsWith(GAME_PACKAGE + "."));
    }

    @Test
    void gameControllerDoesNotExposeJpaEntities() {
        Stream.of(GameController.class.getDeclaredMethods())
                .map(method -> method.toGenericString())
                .forEach(signature -> assertThat(signature)
                        .doesNotContain(GAME_PACKAGE + ".domain."));
    }
}
