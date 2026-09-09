package com.nitros64.nitros_games_backend.storage.infrastructure;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.nitros64.nitros_games_backend.storage.application.HostImageStorage;
import com.nitros64.nitros_games_backend.storage.application.UploadImageException;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@ConditionalOnProperty(
        prefix = "app.storage.host-images",
        name = "backend",
        havingValue = "s3")
public class S3HostImageStorage implements HostImageStorage {

    private static final Logger log = LoggerFactory.getLogger(S3HostImageStorage.class);
    private static final Pattern STORED_FILENAME = Pattern.compile(
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(png|jpg|gif)");

    private final S3Client s3Client;
    private final HostImageUploadValidator validator;
    private final String bucket;
    private final String prefix;

    public S3HostImageStorage(
            S3Client s3Client,
            StorageProperties properties,
            HostImageUploadValidator validator) {
        this.s3Client = s3Client;
        this.validator = validator;
        this.bucket = properties.getS3().getBucket();
        this.prefix = properties.getS3().getPrefix();
    }

    @Override
    public String store(MultipartFile file) {
        HostImageUploadValidator.ImageFormat format = validator.validate(file);
        String objectKey = prefix + UUID.randomUUID() + "." + format.extension();
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentType(format.contentType())
                .build();

        try (InputStream input = file.getInputStream()) {
            s3Client.putObject(request, RequestBody.fromInputStream(input, file.getSize()));
            return objectKey;
        } catch (IOException | SdkException exception) {
            throw storageError("upload", objectKey, exception);
        }
    }

    @Override
    public boolean delete(String storageKey) {
        validateStorageKey(storageKey);
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(storageKey)
                .build();

        try {
            s3Client.deleteObject(request);
            return true;
        } catch (SdkException exception) {
            throw storageError("delete", storageKey, exception);
        }
    }

    private void validateStorageKey(String storageKey) {
        if (storageKey == null || !storageKey.startsWith(prefix)) {
            throw invalidStorageKey();
        }
        String filename = storageKey.substring(prefix.length());
        if (!STORED_FILENAME.matcher(filename).matches()) {
            throw invalidStorageKey();
        }
    }

    private UploadImageException invalidStorageKey() {
        return new UploadImageException(
                "Invalid image storage key",
                null,
                HttpStatus.BAD_REQUEST);
    }

    private UploadImageException storageError(
            String operation,
            String storageKey,
            Exception cause) {
        String errorCode = cause.getClass().getSimpleName();
        if (cause instanceof AwsServiceException serviceException
                && serviceException.awsErrorDetails() != null
                && serviceException.awsErrorDetails().errorCode() != null) {
            errorCode = serviceException.awsErrorDetails().errorCode();
        }
        log.error(
                "S3 host-image operation {} failed for key {} with code {}",
                operation,
                storageKey,
                errorCode);
        return new UploadImageException(
                "Could not " + operation + " the host image",
                cause,
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
