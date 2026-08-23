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
import java.net.URI;

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
import com.nitros64.nitros_games_backend.catalog.domain.Platform;
import com.nitros64.nitros_games_backend.catalog.domain.Processor;
import com.nitros64.nitros_games_backend.catalog.persistence.PlatformRepository;
import com.nitros64.nitros_games_backend.catalog.persistence.ProcessorRepository;
import com.nitros64.nitros_games_backend.tooling.domain.LanguageTool;
import com.nitros64.nitros_games_backend.tooling.domain.ProgramToolType;
import com.nitros64.nitros_games_backend.tooling.domain.ProgrammingLanguage;
import com.nitros64.nitros_games_backend.tooling.domain.ProgrammingTool;
import com.nitros64.nitros_games_backend.tooling.domain.ToolPlatform;
import com.nitros64.nitros_games_backend.tooling.domain.ToolProcessor;
import com.nitros64.nitros_games_backend.tooling.persistence.LanguageToolRepository;
import com.nitros64.nitros_games_backend.tooling.persistence.ProgrammingLanguageRepository;
import com.nitros64.nitros_games_backend.tooling.persistence.ProgrammingToolRepository;
import com.nitros64.nitros_games_backend.tooling.persistence.ProgramToolTypeRepository;
import com.nitros64.nitros_games_backend.tooling.persistence.ToolPlatformRepository;
import com.nitros64.nitros_games_backend.tooling.persistence.ToolProcessorRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ToolingApiContractTests {

    private static final String ADMIN_USERNAME = "test-admin";
    private static final String ADMIN_PASSWORD = "test-admin-password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProgrammingLanguageRepository languageRepository;

    @Autowired
    private ProgrammingToolRepository toolRepository;

    @Autowired
    private ProgramToolTypeRepository toolTypeRepository;

    @Autowired
    private LanguageToolRepository languageToolRepository;

    @Autowired
    private ToolPlatformRepository toolPlatformRepository;

    @Autowired
    private ToolProcessorRepository toolProcessorRepository;

    @Autowired
    private PlatformRepository platformRepository;

    @Autowired
    private ProcessorRepository processorRepository;

    @BeforeEach
    @AfterEach
    void clearTooling() {
        languageToolRepository.deleteAll();
        toolPlatformRepository.deleteAll();
        toolProcessorRepository.deleteAll();
        toolRepository.deleteAll();
        toolTypeRepository.deleteAll();
        languageRepository.deleteAll();
        platformRepository.deleteAll();
        processorRepository.deleteAll();
    }

    @Test
    void programmingToolsCanBeFilteredByEveryCompatibilityDimension() throws Exception {
        ProgramToolType ide = toolTypeRepository.saveAndFlush(new ProgramToolType("IDE"));
        ProgramToolType build = toolTypeRepository.saveAndFlush(new ProgramToolType("Build"));
        ProgrammingLanguage java = languageRepository.saveAndFlush(
                new ProgrammingLanguage("Java"));
        ProgrammingLanguage python = languageRepository.saveAndFlush(
                new ProgrammingLanguage("Python"));
        Platform windows = platformRepository.saveAndFlush(new Platform("Windows"));
        Platform linux = platformRepository.saveAndFlush(new Platform("Linux"));
        Processor x64 = processorRepository.saveAndFlush(new Processor("x86-64"));
        Processor arm = processorRepository.saveAndFlush(new Processor("ARM64"));

        ProgrammingTool eclipse = saveTool(
                "Eclipse", "https://eclipse.example", "eclipse.png", ide);
        ProgrammingTool pyCharm = saveTool(
                "PyCharm", "https://pycharm.example", "pycharm.png", ide);
        ProgrammingTool gradle = saveTool(
                "Gradle", "https://gradle.example", "gradle.png", build);

        linkCompatibility(eclipse, java, windows, x64);
        linkCompatibility(pyCharm, python, linux, x64);
        linkCompatibility(gradle, java, linux, x64);

        mockMvc.perform(get("/api/v1/programmingtools/search")
                    .param("name", "grad")
                    .param("toolTypeId", build.getId().toString())
                    .param("languageId", java.getId().toString())
                    .param("platformId", linux.getId().toString())
                    .param("processorId", x64.getId().toString())
                    .param("sort", "name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Gradle"))
                .andExpect(jsonPath("$.content[0].toolTypeId").value(build.getId()))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/v1/programmingtools/search")
                    .param("toolTypeId", ide.getId().toString())
                    .param("size", "500")
                    .param("sort", "name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].name").value("Eclipse"))
                .andExpect(jsonPath("$.content[1].name").value("PyCharm"))
                .andExpect(jsonPath("$.size").value(100));

        mockMvc.perform(get("/api/v1/programmingtools/search")
                    .param("processorId", arm.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void programmingToolSearchRejectsInvalidFilterIdentifiers() throws Exception {
        mockMvc.perform(get("/api/v1/programmingtools/search")
                    .param("toolTypeId", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));
    }

    @Test
    void languagesAndToolTypesSupportCaseInsensitiveNameSearch() throws Exception {
        languageRepository.saveAllAndFlush(java.util.List.of(
                new ProgrammingLanguage("Java"),
                new ProgrammingLanguage("Kotlin")));
        toolTypeRepository.saveAllAndFlush(java.util.List.of(
                new ProgramToolType("Compiler"),
                new ProgramToolType("Debugger")));

        mockMvc.perform(get("/api/v1/programlanguages/search")
                    .param("name", "AVA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Java"));

        mockMvc.perform(get("/api/v1/programtooltypes/search")
                    .param("name", "PILE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Compiler"));

        mockMvc.perform(get("/api/v1/programlanguages/search")
                    .param("name", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));
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

    private ProgrammingTool saveTool(
            String name,
            String webPage,
            String imagePath,
            ProgramToolType type) throws Exception {
        return toolRepository.saveAndFlush(new ProgrammingTool(
                name,
                URI.create(webPage).toURL(),
                imagePath,
                type));
    }

    private void linkCompatibility(
            ProgrammingTool tool,
            ProgrammingLanguage language,
            Platform platform,
            Processor processor) {
        languageToolRepository.saveAndFlush(new LanguageTool(language, tool));
        toolPlatformRepository.saveAndFlush(new ToolPlatform(tool, platform));
        toolProcessorRepository.saveAndFlush(new ToolProcessor(tool, processor));
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
