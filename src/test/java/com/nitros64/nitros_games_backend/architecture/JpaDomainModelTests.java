package com.nitros64.nitros_games_backend.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.nitros64.nitros_games_backend.catalog.domain.GameGenre;
import com.nitros64.nitros_games_backend.game.domain.DownloadLink;
import com.nitros64.nitros_games_backend.game.domain.GameData;
import com.nitros64.nitros_games_backend.game.domain.GameVersion;
import com.nitros64.nitros_games_backend.storage.domain.ServerHostImage;
import com.nitros64.nitros_games_backend.tooling.domain.LanguageTool;
import com.nitros64.nitros_games_backend.tooling.domain.ProgrammingTool;
import com.nitros64.nitros_games_backend.tooling.domain.ToolPlatform;
import com.nitros64.nitros_games_backend.tooling.domain.ToolProcessor;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

class JpaDomainModelTests {

    @Test
    void entityMappingsUseTheFlywayTableNames() {
        Map<Class<?>, String> expectedTables = Map.of(
                GameGenre.class, "game_genres",
                GameVersion.class, "game_version",
                DownloadLink.class, "download_link");

        expectedTables.forEach((type, tableName) ->
                assertThat(type.getAnnotation(Table.class).name()).isEqualTo(tableName));
    }

    @Test
    void programmingToolMappingsUseTheFlywayColumnNames() throws NoSuchFieldException {
        assertThat(ProgrammingTool.class.getDeclaredField("webPage")
                .getAnnotation(Column.class).name()).isEqualTo("web_page");
        assertThat(ProgrammingTool.class.getDeclaredField("imagefilePath")
                .getAnnotation(Column.class).name()).isEqualTo("imagefile_path");
    }

    @Test
    void databaseCascadeIsNotDuplicatedByHibernate() throws NoSuchFieldException {
        var mapping = GameVersion.class.getDeclaredField("downloadLinks")
                .getAnnotation(OneToMany.class);

        assertThat(mapping.cascade()).doesNotContain(CascadeType.ALL, CascadeType.REMOVE);
    }

    @Test
    void persistentEntitiesExposeDomainOperationsInsteadOfGenericSetters() {
        assertThat(Arrays.asList(
                GameGenre.class,
                GameData.class,
                GameVersion.class,
                DownloadLink.class,
                ProgrammingTool.class,
                LanguageTool.class,
                ToolPlatform.class,
                ToolProcessor.class,
                ServerHostImage.class))
                .allSatisfy(type -> assertThat(Arrays.stream(type.getDeclaredMethods())
                        .map(Method::getName)
                        .filter(name -> name.startsWith("set")))
                        .as(type.getSimpleName())
                        .isEmpty());
    }
}
