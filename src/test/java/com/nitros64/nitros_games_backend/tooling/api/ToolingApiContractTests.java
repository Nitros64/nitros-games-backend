package com.nitros64.nitros_games_backend.tooling.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import com.nitros64.nitros_games_backend.tooling.persistence.ProgramLangRepository;
import com.nitros64.nitros_games_backend.tooling.persistence.ProgramToolRepository;
import com.nitros64.nitros_games_backend.tooling.persistence.ProgramToolTypeRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ToolingApiContractTests {

    private static final String ADMIN_USERNAME = "test-admin";
    private static final String ADMIN_PASSWORD = "test-admin-password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProgramLangRepository languageRepository;

    @Autowired
    private ProgramToolRepository toolRepository;

    @Autowired
    private ProgramToolTypeRepository toolTypeRepository;

    @BeforeEach
    @AfterEach
    void clearTooling() {
        toolRepository.deleteAll();
        toolTypeRepository.deleteAll();
        languageRepository.deleteAll();
    }

    @ParameterizedTest(name = "{0} uses a stable DTO contract")
    @MethodSource("namedResources")
    void namedResourceCrudUsesDtos(
            String resource,
            String initialName,
            String updatedName,
            String bulkNameOne,
            String bulkNameTwo) throws Exception {
        String basePath = "/api/v1/" + resource;
        String createdJson = mockMvc.perform(post(basePath + "/add")
                    .with(admin())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(nameJson(initialName)))
                .andExpect(status().isCreated())
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
                    .with(admin())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(nameJson(updatedName)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(id.longValue()))
                .andExpect(jsonPath("$.name").value(updatedName));

        mockMvc.perform(post(basePath + "/addAll")
                    .with(admin())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("[" + nameJson(bulkNameOne) + "," + nameJson(bulkNameTwo) + "]"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value(bulkNameOne))
                .andExpect(jsonPath("$[1].name").value(bulkNameTwo));

        mockMvc.perform(get(basePath))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));

        mockMvc.perform(get(basePath + "/paged").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3));

        mockMvc.perform(delete(basePath + "/" + id.longValue()).with(admin()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        mockMvc.perform(get(basePath + "/" + id.longValue()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("resource_not_found"));
    }

    @Test
    void programmingToolUsesToolTypeIdAndPreservesSharedTypeOnDelete() throws Exception {
        long compilerId = createToolType("Compiler");
        long debuggerId = createToolType("Debugger");

        String createdJson = mockMvc.perform(post("/api/v1/programmingtools/add")
                    .with(admin())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toolJson("Eclipse", "https://eclipse.example", "eclipse.png", compilerId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Eclipse"))
                .andExpect(jsonPath("$.webPage").value("https://eclipse.example"))
                .andExpect(jsonPath("$.imagefilePath").value("eclipse.png"))
                .andExpect(jsonPath("$.toolTypeId").value(compilerId))
                .andExpect(jsonPath("$.toolType").doesNotExist())
                .andExpect(jsonPath("$.length()").value(5))
                .andReturn().getResponse().getContentAsString();
        Number toolId = JsonPath.read(createdJson, "$.id");

        mockMvc.perform(put("/api/v1/programmingtools/" + toolId.longValue())
                    .with(admin())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toolJson("NetBeans", "https://netbeans.example", "netbeans.png", debuggerId)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(toolId.longValue()))
                .andExpect(jsonPath("$.name").value("NetBeans"))
                .andExpect(jsonPath("$.toolTypeId").value(debuggerId));

        mockMvc.perform(post("/api/v1/programmingtools/addAll")
                    .with(admin())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("["
                            + toolJson("CodeBlocks", "https://codeblocks.example", "codeblocks.png", compilerId)
                            + ","
                            + toolJson("VisualStudio", "https://visualstudio.example", "visualstudio.png", debuggerId)
                            + "]"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].toolTypeId").value(compilerId))
                .andExpect(jsonPath("$[1].toolTypeId").value(debuggerId));

        mockMvc.perform(get("/api/v1/programmingtools/paged").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].toolTypeId").isNumber())
                .andExpect(jsonPath("$.totalElements").value(3));

        mockMvc.perform(delete("/api/v1/programmingtools/" + toolId.longValue()).with(admin()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/programtooltypes/" + debuggerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Debugger"));
    }

    @Test
    void programmingToolRejectsUnknownToolType() throws Exception {
        mockMvc.perform(post("/api/v1/programmingtools/add")
                    .with(admin())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toolJson("Eclipse", "https://eclipse.example", "eclipse.png", 9999)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("resource_not_found"));
    }

    @ParameterizedTest(name = "{0} validates its request DTO")
    @MethodSource("validationCases")
    void requestDtoOwnsValidation(String resource, String body) throws Exception {
        mockMvc.perform(post("/api/v1/" + resource + "/add")
                    .with(admin())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("validation_failed"));
    }

    private static Stream<Arguments> namedResources() {
        return Stream.of(
                Arguments.of("programlanguages", "Java", "Python", "Kotlin", "Swift"),
                Arguments.of("programtooltypes", "Editor", "Compiler", "Debugger", "Profiler"));
    }

    private static Stream<Arguments> validationCases() {
        return Stream.of(
                Arguments.of("programlanguages", "{\"name\":\"Java17\"}"),
                Arguments.of("programtooltypes", "{\"name\":\"Type2\"}"),
                Arguments.of("programmingtools",
                        "{\"name\":\"Tool2\",\"webPage\":\"https://tool.example\","
                                + "\"imagefilePath\":\"tool.png\",\"toolTypeId\":1}"));
    }

    private long createToolType(String name) throws Exception {
        String json = mockMvc.perform(post("/api/v1/programtooltypes/add")
                    .with(admin())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(nameJson(name)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Number id = JsonPath.read(json, "$.id");
        return id.longValue();
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor admin() {
        return httpBasic(ADMIN_USERNAME, ADMIN_PASSWORD);
    }

    private String nameJson(String name) {
        return "{\"name\":\"" + name + "\"}";
    }

    private String toolJson(
            String name,
            String webPage,
            String imagefilePath,
            long toolTypeId) {
        return "{\"name\":\"" + name
                + "\",\"webPage\":\"" + webPage
                + "\",\"imagefilePath\":\"" + imagefilePath
                + "\",\"toolTypeId\":" + toolTypeId + "}";
    }
}
