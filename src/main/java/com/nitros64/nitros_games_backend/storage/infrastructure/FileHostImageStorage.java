package com.nitros64.nitros_games_backend.storage.infrastructure;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import jakarta.annotation.PostConstruct;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.nitros64.nitros_games_backend.storage.application.HostImageStorage;
import com.nitros64.nitros_games_backend.storage.application.UploadImageException;

@Service
@ConditionalOnProperty(
        prefix = "app.storage.host-images",
        name = "backend",
        havingValue = "filesystem",
        matchIfMissing = true)
public class FileHostImageStorage implements HostImageStorage {

    private final Path root;
    private final HostImageUploadValidator validator;

    public FileHostImageStorage(
            StorageProperties properties,
            HostImageUploadValidator validator) {
        this.root = properties.getDirectory().toAbsolutePath().normalize();
        this.validator = validator;

        if (root.getParent() == null) {
            throw new IllegalArgumentException("Storage directory cannot be a filesystem root");
        }
    }

    @PostConstruct
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
    public String store(MultipartFile file) {
        HostImageUploadValidator.ImageFormat format = validator.validate(file);
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
}
