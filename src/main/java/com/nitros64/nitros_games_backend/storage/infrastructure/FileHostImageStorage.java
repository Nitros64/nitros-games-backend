package com.nitros64.nitros_games_backend.storage.infrastructure;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import jakarta.annotation.PostConstruct;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.nitros64.nitros_games_backend.storage.application.FilesStorageService;
import com.nitros64.nitros_games_backend.storage.application.UploadImageException;

@Service("FileHostImageStorage")
public class FileHostImageStorage implements FilesStorageService {

    private static final Map<String, ImageFormat> ALLOWED_IMAGES = Map.of(
            MediaType.IMAGE_PNG_VALUE, new ImageFormat("png", new byte[] {
                    (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a}),
            MediaType.IMAGE_JPEG_VALUE, new ImageFormat("jpg", new byte[] {
                    (byte) 0xff, (byte) 0xd8, (byte) 0xff}),
            MediaType.IMAGE_GIF_VALUE, new ImageFormat("gif", new byte[] {
                    0x47, 0x49, 0x46, 0x38}));

    private final Path root;
    private final long maxFileSize;

    public FileHostImageStorage(StorageProperties properties) {
        this.root = properties.getDirectory().toAbsolutePath().normalize();
        this.maxFileSize = properties.getMaxFileSize().toBytes();

        if (root.getParent() == null) {
            throw new IllegalArgumentException("Storage directory cannot be a filesystem root");
        }
        if (maxFileSize <= 0) {
            throw new IllegalArgumentException("Storage max file size must be greater than zero");
        }
    }

    @PostConstruct
    @Override
    public void init() {
        try {
            Files.createDirectories(root);
            if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("Configured storage path is not a directory");
            }
        } catch (IOException e) {
            throw storageError("Could not initialize the image storage directory", e);
        }
    }

    @Override
    public String write(MultipartFile file) {
        ImageFormat format = validate(file);
        String filename = UUID.randomUUID() + "." + format.extension();
        Path target = resolveInsideRoot(filename);
        Path temporary = null;

        try {
            temporary = Files.createTempFile(root, ".upload-", ".tmp");
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, temporary, StandardCopyOption.REPLACE_EXISTING);
            }
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
            return filename;
        } catch (IOException e) {
            throw storageError("Could not store the uploaded image", e);
        } finally {
            deleteTemporaryFile(temporary);
        }
    }

    @Override
    public Resource load(String filename) {
        Path file = existingRegularFile(filename);
        try {
            return new UrlResource(file.toUri());
        } catch (MalformedURLException e) {
            throw new UploadImageException(
                    "Could not load the requested image", e, HttpStatus.NOT_FOUND);
        }
    }

    @Override
    public boolean delete(String filename) {
        Path file = existingRegularFile(filename);
        try {
            Files.delete(file);
            return true;
        } catch (IOException e) {
            throw storageError("Could not delete the requested image", e);
        }
    }

    private ImageFormat validate(MultipartFile file) {
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
        } catch (IOException e) {
            throw new UploadImageException(
                    "Could not inspect the uploaded image", e, HttpStatus.BAD_REQUEST);
        }
    }

    private Path existingRegularFile(String filename) {
        Path file = resolveInsideRoot(filename);
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            throw new UploadImageException(
                    "The requested image does not exist", null, HttpStatus.NOT_FOUND);
        }
        return file;
    }

    private Path resolveInsideRoot(String filename) {
        try {
            if (filename == null || filename.isBlank()) {
                throw invalidPath();
            }

            Path requested = Path.of(filename);
            if (requested.isAbsolute()
                    || requested.getNameCount() != 1
                    || !requested.getFileName().toString().equals(filename)) {
                throw invalidPath();
            }

            Path resolved = root.resolve(requested).normalize();
            if (!resolved.startsWith(root)) {
                throw invalidPath();
            }
            return resolved;
        } catch (InvalidPathException e) {
            throw new UploadImageException("Invalid image filename", e, HttpStatus.BAD_REQUEST);
        }
    }

    private UploadImageException invalidPath() {
        return new UploadImageException("Invalid image filename", null, HttpStatus.BAD_REQUEST);
    }

    private UploadImageException storageError(String message, IOException cause) {
        return new UploadImageException(message, cause, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private void deleteTemporaryFile(Path temporary) {
        if (temporary == null) {
            return;
        }
        try {
            Files.deleteIfExists(temporary);
        } catch (IOException ignored) {
            // The primary storage exception, if any, must remain visible to the caller.
        }
    }

    private record ImageFormat(String extension, byte[] signature) {
    }
}
