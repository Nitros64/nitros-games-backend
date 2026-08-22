package com.nitros64.nitros_games_backend.storage.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

import com.nitros64.nitros_games_backend.storage.application.UploadImageException;

class FileHostImageStorageTests {

    private static final byte[] PNG_IMAGE = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");

    @TempDir
    Path temporaryDirectory;

    private Path storageDirectory;
    private FileHostImageStorage storage;

    @BeforeEach
    void setUp() {
        storageDirectory = temporaryDirectory.resolve("host-images");
        storage = createStorage(DataSize.ofMegabytes(1));
        storage.init();
    }

    @Test
    void storesValidImagesUnderServerGeneratedNames() throws IOException {
        MockMultipartFile image = new MockMultipartFile(
                "fileHostImage",
                "../../attacker controlled name.png",
                "image/png",
                PNG_IMAGE);

        String filename = storage.write(image);

        assertThat(filename).matches("[0-9a-f-]{36}\\.png");
        assertThat(filename).doesNotContain("attacker", "..", "/", "\\");
        assertThat(storage.load(filename).getContentAsByteArray()).isEqualTo(PNG_IMAGE);
        try (var files = Files.list(storageDirectory)) {
            assertThat(files.map(path -> path.getFileName().toString()).toList())
                    .containsExactly(filename);
        }
    }

    @Test
    void rejectsContentWhoseSignatureDoesNotMatchItsMediaType() {
        MockMultipartFile disguisedText = new MockMultipartFile(
                "fileHostImage",
                "not-an-image.png",
                "image/png",
                "plain text".getBytes(StandardCharsets.UTF_8));

        assertStatus(disguisedText, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    void rejectsFilesThatExceedTheConfiguredLimit() {
        storage = createStorage(DataSize.ofBytes(PNG_IMAGE.length - 1));
        storage.init();
        MockMultipartFile oversized = new MockMultipartFile(
                "fileHostImage", "image.png", "image/png", PNG_IMAGE);

        assertStatus(oversized, HttpStatus.CONTENT_TOO_LARGE);
    }

    @Test
    void rejectsTraversalAndAbsolutePaths() throws IOException {
        Path outsideFile = temporaryDirectory.resolve("outside.png");
        Files.write(outsideFile, PNG_IMAGE);

        assertThatExceptionOfType(UploadImageException.class)
                .isThrownBy(() -> storage.load("../outside.png"))
                .satisfies(exception -> assertThat(exception.getHttpStatus())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
        assertThatExceptionOfType(UploadImageException.class)
                .isThrownBy(() -> storage.delete(outsideFile.toAbsolutePath().toString()))
                .satisfies(exception -> assertThat(exception.getHttpStatus())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
        assertThat(outsideFile).exists();
    }

    @Test
    void deletesOnlyFilesInsideTheConfiguredRoot() {
        MockMultipartFile image = new MockMultipartFile(
                "fileHostImage", "image.png", "image/png", PNG_IMAGE);
        String filename = storage.write(image);

        assertThat(storage.delete(filename)).isTrue();
        assertThat(storageDirectory.resolve(filename)).doesNotExist();
    }

    private FileHostImageStorage createStorage(DataSize maxFileSize) {
        StorageProperties properties = new StorageProperties();
        properties.setDirectory(storageDirectory);
        properties.setMaxFileSize(maxFileSize);
        return new FileHostImageStorage(properties);
    }

    private void assertStatus(MockMultipartFile file, HttpStatus expectedStatus) {
        assertThatExceptionOfType(UploadImageException.class)
                .isThrownBy(() -> storage.write(file))
                .satisfies(exception -> assertThat(exception.getHttpStatus())
                        .isEqualTo(expectedStatus));
    }
}
