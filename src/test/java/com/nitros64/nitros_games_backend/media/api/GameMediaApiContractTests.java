package com.nitros64.nitros_games_backend.media.api;

import static com.nitros64.nitros_games_backend.security.JwtTestSupport.adminJwt;
import static com.nitros64.nitros_games_backend.security.JwtTestSupport.userJwt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.nitros64.nitros_games_backend.game.application.GameApplicationService;
import com.nitros64.nitros_games_backend.game.domain.GameData;
import com.nitros64.nitros_games_backend.game.domain.PublicationStatus;
import com.nitros64.nitros_games_backend.game.persistence.GameDataRepository;
import com.nitros64.nitros_games_backend.media.application.GameMediaService;
import com.nitros64.nitros_games_backend.media.domain.GameMediaType;
import com.nitros64.nitros_games_backend.media.infrastructure.GameMediaProperties;
import com.nitros64.nitros_games_backend.media.persistence.GameMediaRepository;

@SpringBootTest(properties = {
        "app.storage.game-media.directory=target/test-storage/game-media-api",
        "app.storage.game-media.max-bytes=4096",
        "app.storage.game-media.max-pixels=1024"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class GameMediaApiContractTests {
    @Autowired MockMvc mvc;
    @Autowired GameDataRepository games;
    @Autowired GameMediaRepository media;
    @Autowired GameApplicationService gameService;
    @Autowired GameMediaService service;
    @Autowired GameMediaProperties properties;
    @Autowired PlatformTransactionManager transactions;
    private Long gameId;
    private Long otherGameId;

    @BeforeEach
    void fixture() {
        gameId = createGame("Media Test Game");
        otherGameId = createGame("Other Media Game");
    }

    @AfterEach
    void cleanup() {
        if (games.existsById(gameId)) gameService.deleteGame(gameId);
        if (games.existsById(otherGameId)) gameService.deleteGame(otherGameId);
    }

    @Test
    void uploadAndRetrievePublishedImageWithoutExposingStorageKeys() throws Exception {
        byte[] source = png(4, 3);
        long id = upload("cover", source).andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("COVER"))
                .andExpect(jsonPath("$.position").value(0))
                .andExpect(jsonPath("$.width").value(4)).andExpect(jsonPath("$.height").value(3))
                .andExpect(jsonPath("$.storageKey").doesNotExist())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern(
                        "http://localhost" + adminRoot() + "/[0-9]+/content")))
                .andReturn().getResponse().getContentAsString().transform(json -> ((Number) JsonPath.read(json, "$.id")).longValue());
        gameService.changePublicationStatus(gameId, PublicationStatus.PUBLISHED);
        String url = publicRoot() + "/" + id + "/content";
        byte[] returned = mvc.perform(get(url)).andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(header().string("Cache-Control", "no-store, private"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(ImageIO.read(new java.io.ByteArrayInputStream(returned)).getWidth()).isEqualTo(4);
        mvc.perform(get(publicRoot())).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].contentUrl").value(url));
        mvc.perform(head(url)).andExpect(status().isOk())
                .andExpect(header().longValue("Content-Length", returned.length));
    }

    @Test
    void draftsArePrivateIncludingHeadAndBecomePrivateAgainWhenUnpublished() throws Exception {
        long id = uploadId("cover");
        for (String suffix : List.of("", "/" + id + "/content")) {
            mvc.perform(get(publicRoot() + suffix)).andExpect(status().isNotFound());
            mvc.perform(get(publicRoot() + suffix).with(adminJwt())).andExpect(status().isNotFound());
            mvc.perform(head(publicRoot() + suffix)).andExpect(status().isNotFound());
            mvc.perform(get(adminRoot() + suffix).with(adminJwt())).andExpect(status().isOk())
                    .andExpect(header().string("Cache-Control", "no-store, private"));
            mvc.perform(get(adminRoot() + suffix)).andExpect(status().isUnauthorized());
            mvc.perform(get(adminRoot() + suffix).with(userJwt())).andExpect(status().isForbidden());
            mvc.perform(head(adminRoot() + suffix)).andExpect(status().isUnauthorized());
        }
        gameService.changePublicationStatus(gameId, PublicationStatus.PUBLISHED);
        mvc.perform(get(publicRoot() + "/" + id + "/content")).andExpect(status().isOk());
        gameService.changePublicationStatus(gameId, PublicationStatus.DRAFT);
        mvc.perform(get(publicRoot() + "/" + id + "/content")).andExpect(status().isNotFound());
        assertThat(media.existsById(id)).isTrue();
    }

    @Test
    void replacingCoverKeepsIdentityAndDeletesOldFileOnlyOnSuccess() throws Exception {
        long id = uploadId("cover");
        Path old = file(id);
        upload("cover", png(2, 2)).andExpect(status().isConflict());
        assertThat(fileCount()).isEqualTo(1);
        mvc.perform(multipart(HttpMethod.PUT, adminRoot() + "/cover").file(image(png(3, 2))).with(adminJwt()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.width").value(3));
        assertThat(Files.exists(old)).isFalse();
        assertThat(Files.exists(file(id))).isTrue();
        assertThat(fileCount()).isEqualTo(1);
        mvc.perform(multipart(HttpMethod.PUT, adminRoot() + "/cover")
                        .file(image(new byte[]{1, 2, 3})).with(adminJwt())).andExpect(status().isUnsupportedMediaType());
        assertThat(Files.exists(file(id))).isTrue();
        mvc.perform(delete(adminRoot() + "/cover").with(adminJwt())).andExpect(status().isNoContent());
        assertThat(fileCount()).isZero();
        mvc.perform(multipart(HttpMethod.PUT, adminRoot() + "/cover").file(image(png(2, 2))).with(adminJwt()))
                .andExpect(status().isNotFound());
    }

    @Test
    void screenshotsCanBeReorderedEditedAndDeletedWithCompactPositions() throws Exception {
        long first = uploadId("screenshots");
        long second = uploadId("screenshots");
        long third = uploadId("screenshots");
        mvc.perform(put(adminRoot() + "/screenshots/order").with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"screenshotIds\":[" + third + "," + first + "," + second + "]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(third))
                .andExpect(jsonPath("$[0].position").value(1)).andExpect(jsonPath("$[2].position").value(3));
        mvc.perform(put(adminRoot() + "/" + first + "/metadata").with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"altText\":\"Menu\",\"caption\":\"Main menu\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.caption").value("Main menu"));
        Path old = file(first);
        mvc.perform(delete(adminRoot() + "/screenshots/" + first).with(adminJwt())).andExpect(status().isNoContent());
        assertThat(Files.exists(old)).isFalse();
        mvc.perform(get(adminRoot()).with(adminJwt())).andExpect(jsonPath("$[0].id").value(third))
                .andExpect(jsonPath("$[1].id").value(second)).andExpect(jsonPath("$[1].position").value(2));
        upload("screenshots", png(2, 2)).andExpect(status().isCreated()).andExpect(jsonPath("$.position").value(3));
    }

    @Test
    void invalidReordersAndCrossGameAccessCannotChangeImages() throws Exception {
        long first = uploadId("screenshots");
        long second = uploadId("screenshots");
        long cover = uploadId("cover");
        for (String ids : List.of("[]", "[" + first + "]", "[" + first + "," + first + "]",
                "[" + first + "," + cover + "]", "[" + first + ",9223372036854775807]")) {
            mvc.perform(put(adminRoot() + "/screenshots/order").with(adminJwt())
                            .contentType(MediaType.APPLICATION_JSON).content("{\"screenshotIds\":" + ids + "}"))
                    .andExpect(status().isBadRequest());
        }
        assertThat(media.findById(first).orElseThrow().getPosition()).isEqualTo(1);
        assertThat(media.findById(second).orElseThrow().getPosition()).isEqualTo(2);
        String wrongRoot = "/api/v1/admin/games/" + otherGameId + "/media";
        mvc.perform(get(wrongRoot + "/" + first + "/content").with(adminJwt())).andExpect(status().isNotFound());
        mvc.perform(delete(wrongRoot + "/screenshots/" + first).with(adminJwt())).andExpect(status().isNotFound());
        mvc.perform(put(wrongRoot + "/" + first + "/metadata").with(adminJwt()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"altText\":\"\",\"caption\":\"\"}")).andExpect(status().isNotFound());
        gameService.changePublicationStatus(otherGameId, PublicationStatus.PUBLISHED);
        mvc.perform(get("/api/v1/games/" + otherGameId + "/media/" + first + "/content")).andExpect(status().isNotFound());
        mvc.perform(delete(adminRoot() + "/screenshots/" + cover).with(adminJwt())).andExpect(status().isNotFound());
    }

    @Test
    void writesRequireAdminForEveryOperation() throws Exception {
        long id = uploadId("screenshots");
        for (var auth : List.of(false, true)) {
            int expected = auth ? 403 : 401;
            for (var path : List.of("/cover", "/screenshots")) {
                var request = multipart(adminRoot() + path).file(image(png(2, 2)));
                if (auth) request.with(userJwt());
                mvc.perform(request).andExpect(status().is(expected));
            }
            var replacement = multipart(HttpMethod.PUT, adminRoot() + "/cover").file(image(png(2, 2)));
            if (auth) replacement.with(userJwt());
            mvc.perform(replacement).andExpect(status().is(expected));
            for (var path : List.of("/cover", "/screenshots/" + id)) {
                var request = delete(adminRoot() + path);
                if (auth) request.with(userJwt());
                mvc.perform(request).andExpect(status().is(expected));
            }
            for (var path : List.of("/screenshots/order", "/" + id + "/metadata")) {
                var request = put(adminRoot() + path).contentType(MediaType.APPLICATION_JSON).content("{}");
                if (auth) request.with(userJwt());
                mvc.perform(request).andExpect(status().is(expected));
            }
        }
        assertThat(media.existsById(id)).isTrue();
        assertThat(fileCount()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"../cover.png", "..\\cover.png", "/cover.png", "C:\\cover.png",
            "cover.png.exe", "cover.svg", ".hidden.png", "cover\n.png"})
    void rejectsUnsafeNamesAndUnsupportedExtensions(String name) throws Exception {
        mvc.perform(multipart(adminRoot() + "/cover").with(adminJwt())
                        .file(new MockMultipartFile("file", name, "image/png", png(2, 2))))
                .andExpect(status().is(name.equals("cover.png.exe") || name.equals("cover.svg") ? 415 : 400));
        assertThat(fileCount()).isZero();
    }

    @Test
    void validatesEmptySpoofedTruncatedOversizedAndExcessivePixelContent() throws Exception {
        upload("cover", new byte[0]).andExpect(status().isBadRequest());
        upload("cover", new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 13, 10, 26, 10})
                .andExpect(status().isUnsupportedMediaType());
        upload("cover", new byte[4097]).andExpect(status().isContentTooLarge());
        upload("cover", png(33, 33)).andExpect(status().isContentTooLarge());
        mvc.perform(multipart(adminRoot() + "/cover").with(adminJwt())
                        .file(new MockMultipartFile("file", "fake.jpg", "image/jpeg", png(2, 2))))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(multipart(adminRoot() + "/cover").with(adminJwt()))
                .andExpect(status().isBadRequest());
        mvc.perform(multipart(adminRoot() + "/cover").with(adminJwt()).file(image(png(2, 2)))
                        .param("altText", "x".repeat(201))).andExpect(status().isBadRequest());
        assertThat(fileCount()).isZero();
    }

    @Test
    void jpegIsDecodedAndTrailingPayloadIsDiscarded() throws Exception {
        var output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "jpeg", output);
        byte[] clean = output.toByteArray();
        output.write("untrusted trailing payload".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        mvc.perform(multipart(adminRoot() + "/cover").with(adminJwt())
                        .file(new MockMultipartFile("file", "cover.jpeg", "image/jpeg", output.toByteArray())))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.contentType").value("image/jpeg"));
        var stored = media.findByGameIdAndType(gameId, GameMediaType.COVER).orElseThrow();
        assertThat(Files.readAllBytes(file(stored.getId()))).isEqualTo(clean);
    }

    @Test
    void metadataAndOrderAndPathValidationReturn400() throws Exception {
        long id = uploadId("cover");
        for (String body : List.of("{}", "{\"altText\":null,\"caption\":\"\"}",
                "{\"altText\":\"\",\"caption\":\"" + "x".repeat(501) + "\"}")) {
            mvc.perform(put(adminRoot() + "/" + id + "/metadata").with(adminJwt())
                            .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        }
        for (String body : List.of("{}", "{\"screenshotIds\":[0]}", "{\"screenshotIds\":[null]}", "{")) {
            mvc.perform(put(adminRoot() + "/screenshots/order").with(adminJwt())
                            .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/v1/games/0/media")).andExpect(status().isBadRequest());
        mvc.perform(get(publicRoot() + "/-1/content")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/games/9223372036854775807/media")).andExpect(status().isNotFound());
    }

    @Test
    void deletingGameRemovesAllMediaAndFilesAndCoverRemainsOptional() throws Exception {
        gameService.changePublicationStatus(gameId, PublicationStatus.PUBLISHED);
        mvc.perform(get(publicRoot())).andExpect(status().isOk()).andExpect(content().json("[]"));
        uploadId("cover");
        uploadId("screenshots");
        mvc.perform(delete("/api/v1/games/" + gameId).with(adminJwt())).andExpect(status().isNoContent());
        assertThat(media.findAllByGameIdOrderByTypeAscPositionAsc(gameId)).isEmpty();
        assertThat(fileCount()).isZero();
        mvc.perform(get(publicRoot())).andExpect(status().isNotFound());
    }

    @Test
    void rollbackCleansNewFileAndPreservesPreviousCover() throws Exception {
        long id = uploadId("cover");
        Path old = file(id);
        var transaction = new TransactionTemplate(transactions);
        byte[] replacement = png(3, 3);
        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            service.upload(gameId, GameMediaType.COVER, true, "replacement.png", "image/png", replacement, "", "");
            throw new IllegalStateException("force rollback");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(file(id)).isEqualTo(old);
        assertThat(Files.exists(old)).isTrue();
        assertThat(fileCount()).isEqualTo(1);
        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            service.deleteCover(gameId);
            throw new IllegalStateException("force rollback");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(Files.exists(old)).isTrue();
        assertThat(media.existsById(id)).isTrue();
    }

    @Test
    void concurrentCoverCreationProducesOneCoverAndOneConflict() throws Exception {
        var start = new CountDownLatch(1);
        byte[] content = png(2, 2);
        try (var pool = Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Integer> create = () -> {
                start.await();
                return upload("cover", content).andReturn().getResponse().getStatus();
            };
            var first = pool.submit(create);
            var second = pool.submit(create);
            start.countDown();
            assertThat(List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 409);
        }
        assertThat(media.findAllByGameIdOrderByTypeAscPositionAsc(gameId)).hasSize(1);
        assertThat(fileCount()).isEqualTo(1);
    }

    private Long createGame(String name) {
        var game = new GameData();
        game.updateDetails(name, "Test only", false, 1, Set.of());
        return games.saveAndFlush(game).getId();
    }

    private String adminRoot() { return "/api/v1/admin/games/" + gameId + "/media"; }
    private String publicRoot() { return "/api/v1/games/" + gameId + "/media"; }
    private MockMultipartFile image(byte[] bytes) { return new MockMultipartFile("file", "image.png", "image/png", bytes); }
    private ResultActions upload(String path, byte[] bytes) throws Exception {
        return mvc.perform(multipart(adminRoot() + "/" + path).with(adminJwt()).file(image(bytes)));
    }
    private long uploadId(String path) throws Exception {
        String json = upload(path, png(2, 2)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }
    private Path file(long id) {
        return properties.getDirectory().resolve(media.findById(id).orElseThrow().getStorageKey());
    }
    private long fileCount() throws Exception {
        try (var files = Files.list(properties.getDirectory())) { return files.count(); }
    }
    private byte[] png(int width, int height) throws Exception {
        var output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB), "png", output);
        return output.toByteArray();
    }
}
