package com.nitros64.nitros_games_backend.storage.infrastructure;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.nitros64.nitros_games_backend.storage.application.UploadImageException;

@Component
public class HostImageUploadValidator {

    private static final Map<String, ImageFormat> ALLOWED_IMAGES = Map.of(
            MediaType.IMAGE_PNG_VALUE, new ImageFormat("png", MediaType.IMAGE_PNG_VALUE, new byte[] {
                    (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a}),
            MediaType.IMAGE_JPEG_VALUE, new ImageFormat("jpg", MediaType.IMAGE_JPEG_VALUE, new byte[] {
                    (byte) 0xff, (byte) 0xd8, (byte) 0xff}),
            MediaType.IMAGE_GIF_VALUE, new ImageFormat("gif", MediaType.IMAGE_GIF_VALUE, new byte[] {
                    0x47, 0x49, 0x46, 0x38}));

    private final long maxFileSize;

    public HostImageUploadValidator(StorageProperties properties) {
        this.maxFileSize = properties.getMaxFileSize().toBytes();
        if (maxFileSize <= 0) {
            throw new IllegalArgumentException("Storage max file size must be greater than zero");
        }
    }

    public ImageFormat validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new UploadImageException("The uploaded image is empty", null, HttpStatus.BAD_REQUEST);
        }
        if (file.getSize() > maxFileSize) {
            throw new UploadImageException(
                    "The uploaded image exceeds the configured size limit",
                    null,
                    HttpStatus.CONTENT_TOO_LARGE);
        }

        String contentType = file.getContentType();
        ImageFormat format = contentType == null
                ? null
                : ALLOWED_IMAGES.get(contentType.toLowerCase(Locale.ROOT));
        if (format == null || !hasExpectedSignature(file, format.signature())) {
            throw new UploadImageException(
                    "Only valid PNG, JPEG and GIF images are supported",
                    null,
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        }
        return format;
    }

    private boolean hasExpectedSignature(MultipartFile file, byte[] signature) {
        try (InputStream input = file.getInputStream()) {
            return Arrays.equals(signature, input.readNBytes(signature.length));
        } catch (IOException exception) {
            throw new UploadImageException(
                    "Could not inspect the uploaded image",
                    exception,
                    HttpStatus.BAD_REQUEST);
        }
    }

    public record ImageFormat(String extension, String contentType, byte[] signature) {
        public ImageFormat {
            signature = Arrays.copyOf(signature, signature.length);
        }

        @Override
        public byte[] signature() {
            return Arrays.copyOf(signature, signature.length);
        }
    }
}
