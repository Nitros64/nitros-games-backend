package com.nitros64.nitros_games_backend.tooling.api;

import static org.assertj.core.api.Assertions.assertThat;
import static com.nitros64.nitros_games_backend.security.JwtTestSupport.adminJwt;
import static com.nitros64.nitros_games_backend.security.JwtTestSupport.userJwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.nitros64.nitros_games_backend.catalog.domain.Platform;
import com.nitros64.nitros_games_backend.catalog.domain.Processor;
import com.nitros64.nitros_games_backend.catalog.persistence.PlatformRepository;
import com.nitros64.nitros_games_backend.catalog.persistence.ProcessorRepository;
import com.nitros64.nitros_games_backend.game.domain.GameData;
import com.nitros64.nitros_games_backend.game.domain.GameVersion;
import com.nitros64.nitros_games_backend.game.persistence.GameDataRepository;
import com.nitros64.nitros_games_backend.game.persistence.GameVersionRepository;
import com.nitros64.nitros_games_backend.tooling.domain.LanguageToolId;
import com.nitros64.nitros_games_backend.tooling.domain.ProgrammingLanguage;
import com.nitros64.nitros_games_backend.tooling.domain.ProgrammingTool;
import com.nitros64.nitros_games_backend.tooling.domain.ProgramToolType;
import com.nitros64.nitros_games_backend.tooling.persistence.LanguageToolRepository;
import com.nitros64.nitros_games_backend.tooling.persistence.ProgrammingLanguageRepository;
import com.nitros64.nitros_games_backend.tooling.persistence.ProgrammingToolRepository;
import com.nitros64.nitros_games_backend.tooling.persistence.ProgramToolTypeRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LanguageToolApiContractTests {

    private static final String PATH = "/api/v1/language-tools";

    @Autowired MockMvc mvc;
    @Autowired LanguageToolRepository associations;
    @Autowired ProgrammingLanguageRepository languages;
    @Autowired ProgrammingToolRepository tools;
    @Autowired ProgramToolTypeRepository types;
    @Autowired GameVersionRepository versions;
    @Autowired GameDataRepository games;
    @Autowired PlatformRepository platforms;
    @Autowired ProcessorRepository processors;

    private Long languageId;
    private Long toolId;

    @BeforeEach
    void prepareCatalog() throws Exception {
        languageId = languages.saveAndFlush(new ProgrammingLanguage("Java")).getId();
        var type = types.saveAndFlush(new ProgramToolType("Engine"));
        toolId = tools.saveAndFlush(new ProgrammingTool(
                "LibGDX", URI.create("https://libgdx.com").toURL(), "libgdx.png", type)).getId();
    }

    @AfterEach
    void cleanup() {
        versions.deleteAll();
        games.deleteAll();
        associations.deleteAll();
        tools.deleteAll();
        types.deleteAll();
        languages.deleteAll();
        platforms.deleteAll();
        processors.deleteAll();
    }

    @Test
    void adminCreatesAndAnonymousListsIdentifiers() throws Exception {
        mvc.perform(get(PATH)).andExpect(status().isOk()).andExpect(content().json("[]"));
        create(languageId, toolId).andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost" + pairPath()))
                .andExpect(jsonPath("$.programmingLanguageId").value(languageId))
                .andExpect(jsonPath("$.programmingToolId").value(toolId));
        mvc.perform(get(PATH)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].programmingLanguageId").value(languageId))
                .andExpect(jsonPath("$[0].programmingToolId").value(toolId));
    }

    @Test
    void duplicateIsConflictAndKeepsOneAssociation() throws Exception {
        create(languageId, toolId).andExpect(status().isCreated());
        create(languageId, toolId).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("data_conflict"));
        assertThat(associations.count()).isEqualTo(1);
    }

    @Test
    void missingLanguageOrToolReturnsNotFound() throws Exception {
        create(Long.MAX_VALUE, toolId).andExpect(status().isNotFound());
        create(languageId, Long.MAX_VALUE).andExpect(status().isNotFound());
        assertThat(associations.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}", "{\"programmingLanguageId\":1}", "{\"programmingToolId\":1}",
            "{\"programmingLanguageId\":null,\"programmingToolId\":null}",
            "{\"programmingLanguageId\":0,\"programmingToolId\":1}",
            "{\"programmingLanguageId\":-1,\"programmingToolId\":1}",
            "{\"programmingLanguageId\":1,\"programmingToolId\":0}",
            "{\"programmingLanguageId\":1,\"programmingToolId\":-1}"
    })
    void invalidBodyReturnsBadRequest(String body) throws Exception {
        mvc.perform(post(PATH).with(adminJwt()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));
        assertThat(associations.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/0/1", "/-1/1", "/1/0", "/1/-1", "/abc/1"})
    void invalidPathReturnsBadRequest(String suffix) throws Exception {
        mvc.perform(delete(PATH + suffix).with(adminJwt())).andExpect(status().isBadRequest());
    }

    @Test
    void writesRequireAdministrator() throws Exception {
        mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(body(languageId, toolId)))
                .andExpect(status().isUnauthorized());
        mvc.perform(post(PATH).with(userJwt()).contentType(MediaType.APPLICATION_JSON)
                        .content(body(languageId, toolId)))
                .andExpect(status().isForbidden());
        assertThat(associations.count()).isZero();
        create(languageId, toolId).andExpect(status().isCreated());
        mvc.perform(delete(pairPath())).andExpect(status().isUnauthorized());
        mvc.perform(delete(pairPath()).with(userJwt())).andExpect(status().isForbidden());
        assertThat(associations.existsById(new LanguageToolId(languageId, toolId))).isTrue();
    }

    @Test
    void adminDeletesAndMissingAssociationReturnsNotFound() throws Exception {
        create(languageId, toolId).andExpect(status().isCreated());
        mvc.perform(delete(pairPath()).with(adminJwt()))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        assertThat(associations.count()).isZero();
        assertThat(languages.existsById(languageId)).isTrue();
        assertThat(tools.existsById(toolId)).isTrue();
        mvc.perform(delete(pairPath()).with(adminJwt())).andExpect(status().isNotFound());
    }

    @Test
    void associationInUseCannotBeDeletedAndVersionIsPreserved() throws Exception {
        create(languageId, toolId).andExpect(status().isCreated());
        var pairId = new LanguageToolId(languageId, toolId);
        var game = new GameData();
        game.updateDetails("Test Game", "Test description", false, 1, Set.of());
        game = games.saveAndFlush(game);
        var platform = platforms.saveAndFlush(new Platform("Windows"));
        var processor = processors.saveAndFlush(new Processor("x86-64"));
        var version = new GameVersion();
        version.attachToGame(game);
        version.updateCompatibility("Version One", associations.findById(pairId).orElseThrow(),
                platform, processor);
        version = versions.saveAndFlush(version);

        mvc.perform(delete(pairPath()).with(adminJwt())).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("data_conflict"));
        assertThat(associations.existsById(pairId)).isTrue();
        assertThat(versions.existsById(version.getId())).isTrue();
    }

    private ResultActions create(Long language, Long tool) throws Exception {
        return mvc.perform(post(PATH).with(adminJwt()).contentType(MediaType.APPLICATION_JSON)
                .content(body(language, tool)));
    }

    private String body(Long language, Long tool) {
        return "{\"programmingLanguageId\":%d,\"programmingToolId\":%d}".formatted(language, tool);
    }

    private String pairPath() {
        return PATH + "/" + languageId + "/" + toolId;
    }
}
