package com.nitros64.nitros_games_backend.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.nitros64.nitros_games_backend.game.application.GameVersionService;
import com.nitros64.nitros_games_backend.game.domain.DownloadLink;
import com.nitros64.nitros_games_backend.game.domain.GameData;
import com.nitros64.nitros_games_backend.game.domain.GameVersion;
import com.nitros64.nitros_games_backend.game.persistence.DownloadLinkRepository;
import com.nitros64.nitros_games_backend.game.persistence.GameDataRepository;
import com.nitros64.nitros_games_backend.game.persistence.GameVersionDaoEMImpl;
import com.nitros64.nitros_games_backend.game.persistence.GameVersionRepository;
import com.nitros64.nitros_games_backend.game.persistence.IGameVersionDaoEM;

class GameModuleStructureTests {

    private static final String GAME_PACKAGE =
            "com.nitros64.nitros_games_backend.game";

    @Test
    void gameComponentsStayInsideTheirVerticalModule() {
        List<Class<?>> gameTypes = List.of(
                GameData.class, GameVersion.class, DownloadLink.class,
                GameVersionService.class,
                GameDataRepository.class, GameVersionRepository.class,
                DownloadLinkRepository.class, IGameVersionDaoEM.class,
                GameVersionDaoEMImpl.class);

        assertThat(gameTypes)
                .allSatisfy(type -> assertThat(type.getPackageName())
                        .startsWith(GAME_PACKAGE + "."));
    }
}
