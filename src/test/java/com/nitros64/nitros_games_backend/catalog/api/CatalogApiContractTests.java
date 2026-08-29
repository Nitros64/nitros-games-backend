package com.nitros64.nitros_games_backend.catalog.api;

import static com.nitros64.nitros_games_backend.security.JwtTestSupport.adminJwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.matchesPattern;

import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;
import com.nitros64.nitros_games_backend.catalog.persistence.GameGenreRepository;
import com.nitros64.nitros_games_backend.catalog.persistence.PlatformRepository;
import com.nitros64.nitros_games_backend.catalog.persistence.ProcessorRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CatalogApiContractTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GameGenreRepository genreRepository;

    @Autowired
    private PlatformRepository platformRepository;

    @Autowired
    private ProcessorRepository processorRepository;

    @BeforeEach
    @AfterEach
    void clearCatalog() {
        genreRepository.deleteAll();
        platformRepository.deleteAll();
        processorRepository.deleteAll();
    }

    @ParameterizedTest(name = "{0} uses DTOs for its complete HTTP contract")
    @MethodSource("catalogResources")
    void completeCrudContractUsesStableDtoShape(
            String resource,
            String initialName,
            String updatedName,
            String bulkNameOne,
            String bulkNameTwo) throws Exception {
        String basePath = "/api/v1/" + resource;

        String createdJson = mockMvc.perform(post(basePath)
                    .with(adminJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(initialName)))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        matchesPattern("http://localhost" + basePath + "/[0-9]+")))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value(initialName))
                .andExpect(jsonPath("$.length()").value(2))
                .andReturn().getResponse().getContentAsString();
        Number id = JsonPath.read(createdJson, "$.id");

        mockMvc.perform(get(basePath + "/" + id.longValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.longValue()))
                .andExpect(jsonPath("$.name").value(initialName))
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(put(basePath + "/" + id.longValue())
                    .with(adminJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(updatedName)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.longValue()))
                .andExpect(jsonPath("$.name").value(updatedName))
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(post(basePath + "/batch")
                    .with(adminJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("[" + json(bulkNameOne) + "," + json(bulkNameTwo) + "]"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value(bulkNameOne))
                .andExpect(jsonPath("$[1].name").value(bulkNameTwo));

        mockMvc.perform(get(basePath))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].name").isString());

        mockMvc.perform(get(basePath + "/paged").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").isNumber())
                .andExpect(jsonPath("$.content[0].name").isString())
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.sort.sorted").value(false));

        mockMvc.perform(get(basePath + "/search")
                    .param("name", updatedName.substring(1).toUpperCase())
                    .param("size", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(id.longValue()))
                .andExpect(jsonPath("$.content[0].name").value(updatedName))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.size").value(100));

        mockMvc.perform(delete(basePath + "/" + id.longValue())
                    .with(adminJwt()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        mockMvc.perform(get(basePath + "/" + id.longValue()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("resource_not_found"));
    }

    @ParameterizedTest(name = "{0} validates its request DTO")
    @MethodSource("catalogValidationCases")
    void requestDtoOwnsValidation(String resource, String invalidName) throws Exception {
        mockMvc.perform(post("/api/v1/" + resource)
                    .with(adminJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(invalidName)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.errors[0].field").value("name"));
    }

    @ParameterizedTest(name = "{0} validates search criteria")
    @MethodSource("catalogSearchResources")
    void searchValidatesItsNameParameter(String resource) throws Exception {
        mockMvc.perform(get("/api/v1/" + resource + "/search").param("name", " "))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.errors[0].field").value("name"));
    }

    @ParameterizedTest(name = "legacy catalog route {0} remains available")
    @MethodSource("legacyCatalogResources")
    void legacyRoutesRemainAvailable(String resource) throws Exception {
        mockMvc.perform(get("/api/v1/" + resource))
                .andExpect(status().isOk());
    }

    private static Stream<Arguments> catalogResources() {
        return Stream.of(
                Arguments.of("game-genres", "Adventure", "Strategy", "Puzzle", "Simulation"),
                Arguments.of("platforms", "Windows", "Linux", "Arcade", "Console"),
                Arguments.of("processors", "Z80", "M68000", "ARM64", "RISC-V"));

    }

    private static Stream<Arguments> catalogValidationCases() {
        return Stream.of(
                Arguments.of("game-genres", "Genre2"),
                Arguments.of("platforms", "Platform2"),
                Arguments.of("processors", "ProcessorNameTooLong"));
    }

    private static Stream<Arguments> catalogSearchResources() {
        return Stream.of(
                Arguments.of("game-genres"),
                Arguments.of("platforms"),
                Arguments.of("processors"));
    }

    private static Stream<Arguments> legacyCatalogResources() {
        return Stream.of(
                Arguments.of("gamegenre"),
                Arguments.of("platform"),
                Arguments.of("processor"));
    }

    private String json(String name) {
        return "{\"name\":\"" + name + "\"}";
    }
}
