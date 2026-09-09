package com.nitros64.nitros_games_backend.storage.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

import com.nitros64.nitros_games_backend.storage.application.UploadImageException;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

class S3HostImageStorageTests {

    private static final byte[] PNG_IMAGE = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");

    private S3Client s3Client;
    private S3HostImageStorage storage;

    @BeforeEach
    void setUp() {
        s3Client = org.mockito.Mockito.mock(S3Client.class);
        StorageProperties properties = properties();
        storage = new S3HostImageStorage(
                s3Client,
                properties,
                new HostImageUploadValidator(properties));
    }

    @Test
    void uploadsValidatedImageUnderGeneratedKey() {
        MockMultipartFile image = new MockMultipartFile(
                "fileHostImage",
                "../../untrusted-name.png",
                "image/png",
                PNG_IMAGE);

        String key = storage.store(image);

        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(request.capture(), any(RequestBody.class));
        assertThat(key).matches("host-images/[0-9a-f-]{36}\\.png");
        assertThat(key).doesNotContain("untrusted", "..", "\\");
        assertThat(request.getValue().bucket()).isEqualTo("nitros-games-production-images");
        assertThat(request.getValue().key()).isEqualTo(key);
        assertThat(request.getValue().contentType()).isEqualTo("image/png");
    }

    @Test
    void deletesOnlyTheExactGeneratedStorageKey() {
        String key = "host-images/550e8400-e29b-41d4-a716-446655440000.jpg";

        assertThat(storage.delete(key)).isTrue();

        ArgumentCaptor<DeleteObjectRequest> request =
                ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(request.capture());
        assertThat(request.getValue().bucket()).isEqualTo("nitros-games-production-images");
        assertThat(request.getValue().key()).isEqualTo(key);
    }

    @Test
    void rejectsKeysOutsideTheConfiguredPrefix() {
        assertThatExceptionOfType(UploadImageException.class)
                .isThrownBy(() -> storage.delete("other/550e8400-e29b-41d4-a716-446655440000.png"))
                .satisfies(exception -> assertThat(exception.getHttpStatus())
                        .isEqualTo(HttpStatus.BAD_REQUEST));

        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void translatesSdkUploadFailureWithoutExposingAwsDetails() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(S3Exception.builder().message("sensitive provider detail").build());
        MockMultipartFile image = new MockMultipartFile(
                "fileHostImage", "image.png", "image/png", PNG_IMAGE);

        assertThatExceptionOfType(UploadImageException.class)
                .isThrownBy(() -> storage.store(image))
                .satisfies(exception -> {
                    assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    assertThat(exception.getMessage()).isEqualTo("Could not upload the host image");
                    assertThat(exception.getMessage()).doesNotContain("sensitive");
                });
    }

    private StorageProperties properties() {
        StorageProperties properties = new StorageProperties();
        properties.setBackend(StorageProperties.Backend.S3);
        properties.setMaxFileSize(DataSize.ofMegabytes(1));
        properties.getS3().setBucket("nitros-games-production-images");
        properties.getS3().setRegion("eu-west-1");
        properties.getS3().setPrefix("host-images/");
        return properties;
    }
}
