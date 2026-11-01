package com.nitros64.nitros_games_backend.shared.validation;

import static com.nitros64.nitros_games_backend.security.JwtTestSupport.adminJwt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import jakarta.validation.Validator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import com.nitros64.nitros_games_backend.catalog.api.dto.GameGenreRequest;
import com.nitros64.nitros_games_backend.catalog.api.dto.PlatformRequest;
import com.nitros64.nitros_games_backend.catalog.api.dto.ProcessorRequest;
import com.nitros64.nitros_games_backend.game.api.dto.DownloadLinkRequest;
import com.nitros64.nitros_games_backend.game.api.dto.GameRequest;
import com.nitros64.nitros_games_backend.game.api.dto.GameVersionRequest;
import com.nitros64.nitros_games_backend.tooling.api.dto.ProgrammingLanguageRequest;
import com.nitros64.nitros_games_backend.tooling.api.dto.ProgrammingToolRequest;
import com.nitros64.nitros_games_backend.tooling.api.dto.ProgramToolTypeRequest;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RequestValidationContractTests {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    @Autowired MockMvc mvc;
    @Autowired Validator validator;

    @ParameterizedTest(name = "{0}: {1} {2}")
    @MethodSource("invalidFields")
    void rejectsEachInvalidFieldBeforeResolvingReferences(
            String resource, String field, String scenario, String body) throws Exception {
        mvc.perform(post("/api/v1/" + resource).with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.errors[*].field", org.hamcrest.Matchers.hasItem(field)))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @ParameterizedTest(name = "{0}: {1} accepts length {2}")
    @MethodSource("validBoundaries")
    void exactLengthBoundariesAreValid(String resource, String field, int length,
            Class<?> type, String body) {
        assertThat(validator.validate(JSON.readValue(body, type))).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("namedResources")
    void duplicateCatalogNamesReturnConflict(String resource) throws Exception {
        String body = "{\"name\":\"Example\"}";
        mvc.perform(post("/api/v1/" + resource).with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
        mvc.perform(post("/api/v1/" + resource).with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("data_conflict"));
    }

    private static Stream<String> namedResources() {
        return Stream.of("game-genres", "platforms", "processors", "programming-languages", "programming-tool-types");
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("malformedBodies")
    void unreadableValuesReturnMalformedJson(String resource, String scenario, String body) throws Exception {
        mvc.perform(post("/api/v1/" + resource).with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("malformed_json"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    private static Stream<Arguments> malformedBodies() {
        List<Arguments> cases = new ArrayList<>();
        Shape game = shapes().stream().filter(shape -> shape.resource().equals("games")).findFirst().orElseThrow();
        for (String field : List.of("developerCount", "jam")) {
            Map<String, Object> body = new LinkedHashMap<>(game.body());
            body.remove(field);
            cases.add(Arguments.of("games", "missing " + field, JSON.writeValueAsString(body)));
            body.put(field, null);
            cases.add(Arguments.of("games", "null " + field, JSON.writeValueAsString(body)));
        }
        cases.add(Arguments.of("programming-tools", "invalid URL",
                "{\"name\":\"Editor\",\"webPage\":\"not a URL\",\"imagefilePath\":\"tool.png\",\"toolTypeId\":1}"));
        return cases.stream();
    }

    private static Stream<Arguments> invalidFields() {
        List<Arguments> cases = new ArrayList<>();
        for (Shape shape : shapes()) {
            for (String field : shape.body().keySet()) {
                if (field.equals("jam")) continue; // Missing/null primitives are checked at JSON decoding.
                if (!field.equals("developerCount")) {
                    Map<String, Object> omitted = new LinkedHashMap<>(shape.body());
                    omitted.remove(field);
                    cases.add(Arguments.of(shape.resource(), field, "missing", JSON.writeValueAsString(omitted)));
                    add(cases, shape, field, "null", null);
                }
                if (shape.lengths().containsKey(field)) {
                    int[] limits = shape.lengths().get(field);
                    add(cases, shape, field, "blank", " ".repeat(limits[0]));
                    add(cases, shape, field, "below minimum", "a".repeat(limits[0] - 1));
                    add(cases, shape, field, "above maximum", "a".repeat(limits[1] + 1));
                } else if (field.equals("genreIds")) {
                    add(cases, shape, field, "empty", List.of());
                    add(cases, shape, field + "[]", "zero", List.of(0));
                    add(cases, shape, field + "[]", "negative", List.of(-1));
                    add(cases, shape, field + "[]", "null element", java.util.Arrays.asList((Object) null));
                } else if (!field.equals("webPage")) {
                    add(cases, shape, field, "zero", 0);
                    add(cases, shape, field, "negative", -1);
                }
            }
        }
        return cases.stream();
    }

    private static void add(List<Arguments> cases, Shape shape, String field, String scenario, Object value) {
        Map<String, Object> body = new LinkedHashMap<>(shape.body());
        body.put(field.replace("[]", ""), value);
        cases.add(Arguments.of(shape.resource(), field, scenario, JSON.writeValueAsString(body)));
    }

    private static Stream<Arguments> validBoundaries() {
        List<Arguments> cases = new ArrayList<>();
        for (Shape shape : shapes()) {
            shape.lengths().forEach((field, limits) -> {
                for (int length : limits) {
                    Map<String, Object> body = new LinkedHashMap<>(shape.body());
                    body.put(field, "a".repeat(length));
                    cases.add(Arguments.of(shape.resource(), field, length, shape.type(), JSON.writeValueAsString(body)));
                }
            });
        }
        return cases.stream();
    }

    private static List<Shape> shapes() {
        return List.of(
                named("game-genres", GameGenreRequest.class, 4, 30),
                named("platforms", PlatformRequest.class, 1, 30),
                named("processors", ProcessorRequest.class, 1, 10),
                named("programming-languages", ProgrammingLanguageRequest.class, 1, 12),
                named("programming-tool-types", ProgramToolTypeRequest.class, 4, 30),
                new Shape("games", GameRequest.class,
                        Map.of("name", "Test Game", "description", "Description", "jam", false,
                                "developerCount", 1, "genreIds", List.of(1)),
                        Map.of("name", new int[]{4, 30}, "description", new int[]{4, 30})),
                new Shape("games/1/versions", GameVersionRequest.class,
                        Map.of("name", "Version", "programmingLanguageId", 1, "programmingToolId", 1,
                                "platformId", 1, "processorId", 1), Map.of("name", new int[]{4, 30})),
                new Shape("games/1/versions/1/download-links", DownloadLinkRequest.class,
                        Map.of("link", "https://files.example/game.zip", "serverHostImageId", 1),
                        Map.of("link", new int[]{4, 100})),
                new Shape("programming-tools", ProgrammingToolRequest.class,
                        Map.of("name", "Editor", "webPage", "https://tool.example", "imagefilePath", "tool.png",
                                "toolTypeId", 1), Map.of("name", new int[]{4, 30}, "imagefilePath", new int[]{4, 30})));
        // LanguageTool's required/positive fields are already exhaustively covered by its API tests.
    }

    private static Shape named(String resource, Class<?> type, int min, int max) {
        return new Shape(resource, type, Map.of("name", "Example"), Map.of("name", new int[]{min, max}));
    }

    private record Shape(String resource, Class<?> type, Map<String, Object> body, Map<String, int[]> lengths) {}
}
