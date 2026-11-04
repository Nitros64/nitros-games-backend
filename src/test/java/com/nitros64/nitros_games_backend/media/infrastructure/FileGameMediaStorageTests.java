package com.nitros64.nitros_games_backend.media.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.nitros64.nitros_games_backend.storage.application.UploadImageException;

class FileGameMediaStorageTests {
    @TempDir Path temporary;

    @Test
    void generatedKeysRoundTripAndDeletionIsIdempotent() throws Exception {
        var properties = new GameMediaProperties();
        properties.setDirectory(temporary.resolve("media"));
        var storage = new FileGameMediaStorage(properties);
        storage.initialize();
        byte[] bytes = {1, 2, 3};
        String key = storage.store(bytes, "png");
        assertThat(key).matches("[0-9a-f-]{36}\\.png");
        assertThat(storage.load(key)).isEqualTo(bytes);
        storage.delete(key);
        storage.delete(key);
        try (var files = Files.list(properties.getDirectory())) {
            assertThat(files.count()).isZero();
        }
    }

    @Test
    void storageRejectsTraversalAbsolutePathsAndForeignKeysWithoutTouchingOutsideFiles() throws Exception {
        var properties = new GameMediaProperties();
        properties.setDirectory(temporary.resolve("media"));
        var storage = new FileGameMediaStorage(properties);
        storage.initialize();
        Path outside = temporary.resolve("outside.png");
        Files.write(outside, new byte[]{4, 5});
        for (String key : List.of("../outside.png", "..\\outside.png", outside.toString(),
                "host-images/550e8400-e29b-41d4-a716-446655440000.png", "cover.png")) {
            assertThatThrownBy(() -> storage.load(key)).isInstanceOf(UploadImageException.class);
            assertThatThrownBy(() -> storage.delete(key)).isInstanceOf(UploadImageException.class);
        }
        assertThat(Files.readAllBytes(outside)).containsExactly(4, 5);
    }
}
