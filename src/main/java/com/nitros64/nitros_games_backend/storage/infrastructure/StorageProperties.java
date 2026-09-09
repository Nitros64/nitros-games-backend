package com.nitros64.nitros_games_backend.storage.infrastructure;

import java.nio.file.Path;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.storage.host-images")
public class StorageProperties {

    @NotNull
    private Backend backend = Backend.FILESYSTEM;

    @NotNull
    private Path directory = Path.of("uploadImageFileHost");

    @NotNull
    private DataSize maxFileSize = DataSize.ofMegabytes(10);

    @Valid
    @NotNull
    private S3 s3 = new S3();

    public Backend getBackend() {
        return backend;
    }

    public void setBackend(Backend backend) {
        this.backend = backend;
    }

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

    public S3 getS3() {
        return s3;
    }

    public void setS3(S3 s3) {
        this.s3 = s3;
    }

    @AssertTrue(message = "S3 host-image storage requires a bucket, AWS region and safe prefix")
    public boolean isBackendConfigurationValid() {
        if (backend != Backend.S3) {
            return true;
        }
        return hasText(s3.bucket)
                && hasText(s3.region)
                && hasSafePrefix(s3.prefix);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean hasSafePrefix(String value) {
        return hasText(value)
                && !value.startsWith("/")
                && !value.contains("..")
                && !value.contains("\\")
                && value.matches("(?:[a-z0-9][a-z0-9-]*/)+");
    }

    public enum Backend {
        FILESYSTEM,
        S3
    }

    public static class S3 {

        private String bucket;
        private String region;
        private String prefix = "host-images/";

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }
    }
}
