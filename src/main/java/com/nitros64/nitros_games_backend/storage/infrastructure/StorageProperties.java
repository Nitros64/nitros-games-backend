package com.nitros64.nitros_games_backend.storage.infrastructure;

import java.nio.file.Path;

import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.storage.host-images")
public class StorageProperties {

    @NotNull
    private Path directory = Path.of("uploadImageFileHost");

    @NotNull
    private DataSize maxFileSize = DataSize.ofMegabytes(10);

    public Path getDirectory() {
        return directory;
    }

    public void setDirectory(Path directory) {
        this.directory = directory;
    }

    public DataSize getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(DataSize maxFileSize) {
        this.maxFileSize = maxFileSize;
    }
}
