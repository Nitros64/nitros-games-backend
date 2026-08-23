package com.nitros64.nitros_games_backend.storage.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.matchesPattern;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;
import com.nitros64.nitros_games_backend.storage.persistence.ServerHostImageRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StorageApiContractTests {

    private static final byte[] PNG_IMAGE = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");
    private static final String ADMIN_USERNAME = "test-admin";
    private static final String ADMIN_PASSWORD = "test-admin-password";
    private static final Path STORAGE_DIRECTORY = Path.of(
            "target/test-storage/host-images").toAbsolutePath().normalize();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ServerHostImageRepository repository;

    @BeforeEach
    @AfterEach
    void clearStorage() throws IOException {
        repository.deleteAll();
        Files.createDirectories(STORAGE_DIRECTORY);
        try (var files = Files.list(STORAGE_DIRECTORY)) {
            for (Path file : files.toList()) {
                if (Files.isRegularFile(file)) {
                    Files.deleteIfExists(file);
                }
            }
        }
    }

    @Test
    void completeLifecycleKeepsDatabaseAndFilesystemConsistent() throws Exception {
        String createdJson = mockMvc.perform(multipart("/api/v1/serverhostimage/upload_image")
                    .file(validPng("host.png"))
                    .param("name", "MediaFire")
                    .with(admin()))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        matchesPattern("http://localhost/api/v1/serverhostimage/[0-9]+")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("MediaFire"))
                .andExpect(jsonPath("$.imagepath").isString())
                .andExpect(jsonPath("$.length()").value(3))
                .andReturn().getResponse().getContentAsString();
        Number id = JsonPath.read(createdJson, "$.id");
        String originalFilename = JsonPath.read(createdJson, "$.imagepath");
        assertThat(STORAGE_DIRECTORY.resolve(originalFilename)).exists();

        mockMvc.perform(multipart("/api/v1/serverhostimage/update_name/" + id.longValue())
                    .param("name", "Dropbox")
                    .with(admin())
                    .with(request -> {
                        request.setMethod("PUT");
                        return request;
                    }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.longValue()))
                .andExpect(jsonPath("$.name").value("Dropbox"))
                .andExpect(jsonPath("$.imagepath").value(originalFilename));

        String updatedJson = mockMvc.perform(multipart(
                        "/api/v1/serverhostimage/upload_image/" + id.longValue())
                    .file(validPng("replacement.png"))
                    .param("name", "Dropbox")
                    .with(admin())
                    .with(request -> {
                        request.setMethod("PUT");
                        return request;
                    }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.longValue()))
                .andExpect(jsonPath("$.name").value("Dropbox"))
                .andExpect(jsonPath("$.imagepath").isString())
                .andReturn().getResponse().getContentAsString();
        String replacementFilename = JsonPath.read(updatedJson, "$.imagepath");
        assertThat(replacementFilename).isNotEqualTo(originalFilename);
        assertThat(STORAGE_DIRECTORY.resolve(originalFilename)).doesNotExist();
        assertThat(STORAGE_DIRECTORY.resolve(replacementFilename)).exists();

        mockMvc.perform(get("/api/v1/serverhostimage/" + id.longValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Dropbox"))
                .andExpect(jsonPath("$.imagepath").value(replacementFilename));
        mockMvc.perform(get("/api/v1/serverhostimage/paged").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/v1/serverhostimage/search")
                        .param("name", "ROP")
                        .param("size", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(id.longValue()))
                .andExpect(jsonPath("$.content[0].name").value("Dropbox"))
                .andExpect(jsonPath("$.size").value(100));

        mockMvc.perform(delete("/api/v1/serverhostimage/" + id.longValue()).with(admin()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        assertThat(STORAGE_DIRECTORY.resolve(replacementFilename)).doesNotExist();
        assertThat(repository.findById(id.longValue())).isEmpty();
    }

    @Test
    void duplicateNameRollsBackNewFile() throws Exception {
        mockMvc.perform(multipart("/api/v1/serverhostimage/upload_image")
                    .file(validPng("first.png"))
                    .param("name", "MediaFire")
                    .with(admin()))
                .andExpect(status().isCreated());
        assertThat(storedFileCount()).isEqualTo(1);

        mockMvc.perform(multipart("/api/v1/serverhostimage/upload_image")
                    .file(validPng("duplicate.png"))
                    .param("name", "MediaFire")
                    .with(admin()))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("data_conflict"));

        assertThat(repository.count()).isEqualTo(1);
        assertThat(storedFileCount()).isEqualTo(1);
    }

    @Test
    void failedReplacementKeepsOldFileAndRemovesNewFile() throws Exception {
        String firstJson = upload("MediaFire", "first.png");
        String secondJson = upload("Dropbox", "second.png");
        Number firstId = JsonPath.read(firstJson, "$.id");
        String firstFilename = JsonPath.read(firstJson, "$.imagepath");
        String secondFilename = JsonPath.read(secondJson, "$.imagepath");

        mockMvc.perform(multipart(
                        "/api/v1/serverhostimage/upload_image/" + firstId.longValue())
                    .file(validPng("replacement.png"))
                    .param("name", "Dropbox")
                    .with(admin())
                    .with(request -> {
                        request.setMethod("PUT");
                        return request;
                    }))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("data_conflict"));

        var persisted = repository.findById(firstId.longValue()).orElseThrow();
        assertThat(persisted.getName()).isEqualTo("MediaFire");
        assertThat(persisted.getImagepath()).isEqualTo(firstFilename);
        assertThat(STORAGE_DIRECTORY.resolve(firstFilename)).exists();
        assertThat(STORAGE_DIRECTORY.resolve(secondFilename)).exists();
        assertThat(storedFileCount()).isEqualTo(2);
    }

    @Test
    void invalidImageAndInvalidNameDoNotWriteFiles() throws Exception {
        MockMultipartFile disguisedText = new MockMultipartFile(
                "fileHostImage",
                "not-image.png",
                MediaType.IMAGE_PNG_VALUE,
                "plain text".getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart("/api/v1/serverhostimage/upload_image")
                    .file(disguisedText)
                    .param("name", "MediaFire")
                    .with(admin()))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("image_storage_error"));

        mockMvc.perform(multipart("/api/v1/serverhostimage/upload_image")
                    .file(validPng("valid.png"))
                    .param("name", "Mega2")
                    .with(admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));

        assertThat(repository.count()).isZero();
        assertThat(storedFileCount()).isZero();
    }

    @Test
    void unknownUpdateDoesNotWriteAFile() throws Exception {
        mockMvc.perform(multipart("/api/v1/serverhostimage/upload_image/9999")
                    .file(validPng("unused.png"))
                    .param("name", "MediaFire")
                    .with(admin())
                    .with(request -> {
                        request.setMethod("PUT");
                        return request;
                    }))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("resource_not_found"));
        assertThat(storedFileCount()).isZero();
    }

    @Test
    void metadataCannotBeCreatedOrUpdatedWithoutFileOperations() throws Exception {
        mockMvc.perform(post("/api/v1/serverhostimage/add")
                    .with(admin())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"MediaFire\",\"imagepath\":\"client.png\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("operation_not_allowed"));

        mockMvc.perform(put("/api/v1/serverhostimage/9999")
                    .with(admin())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"MediaFire\",\"imagepath\":\"client.png\"}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("method_not_allowed"));
    }

    @Test
    void searchValidatesItsNameParameter() throws Exception {
        mockMvc.perform(get("/api/v1/serverhostimage/search").param("name", " "))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.errors[0].field").value("name"));
    }

    private MockMultipartFile validPng(String originalFilename) {
        return new MockMultipartFile(
                "fileHostImage",
                originalFilename,
                MediaType.IMAGE_PNG_VALUE,
                PNG_IMAGE);
    }

    private String upload(String name, String originalFilename) throws Exception {
        return mockMvc.perform(multipart("/api/v1/serverhostimage/upload_image")
                    .file(validPng(originalFilename))
                    .param("name", name)
                    .with(admin()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor admin() {
        return httpBasic(ADMIN_USERNAME, ADMIN_PASSWORD);
    }

    private long storedFileCount() throws IOException {
        try (var files = Files.list(STORAGE_DIRECTORY)) {
            return files.filter(Files::isRegularFile).count();
        }
    }
}
