package com.nitros64.nitros_games_backend.media.infrastructure;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import com.nitros64.nitros_games_backend.media.application.GameMediaStorage;
import com.nitros64.nitros_games_backend.shared.application.ResourceNotFoundException;
import com.nitros64.nitros_games_backend.storage.application.UploadImageException;

@Component
@ConditionalOnProperty(prefix = "app.storage.game-media", name = "backend",
        havingValue = "filesystem", matchIfMissing = true)
public class FileGameMediaStorage implements GameMediaStorage {
    private final Path root;

    public FileGameMediaStorage(GameMediaProperties properties) {
        root = properties.getDirectory().toAbsolutePath().normalize();
        if (root.getParent() == null) throw new IllegalArgumentException("Media storage cannot use a filesystem root");
    }

    @PostConstruct
    public void initialize() {
        try {
            Files.createDirectories(root);
            if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS) || !root.toRealPath().equals(root)) {
                throw new IOException("Media directory must not contain symbolic links");
            }
        } catch (IOException exception) {
            throw failure(exception);
        }
    }

    @Override
    public String store(byte[] content, String extension) {
        if (!extension.equals("png") && !extension.equals("jpg")) throw invalidKey();
        String key = UUID.randomUUID() + "." + extension;
        Path temporary = null;
        try {
            temporary = Files.createTempFile(root, ".upload-", ".tmp");
            Files.write(temporary, content);
            Files.move(temporary, resolve(key), StandardCopyOption.ATOMIC_MOVE);
            return key;
        } catch (IOException exception) {
            throw failure(exception);
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); }
                catch (IOException exception) {
                    org.slf4j.LoggerFactory.getLogger(getClass()).error("Could not remove temporary media file", exception);
                }
            }
        }
    }

    @Override
    public byte[] load(String key) {
        Path file = resolve(key);
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            throw new ResourceNotFoundException("Image not found");
        }
        try (var input = Files.newInputStream(file, LinkOption.NOFOLLOW_LINKS)) {
            return input.readAllBytes();
        } catch (IOException exception) {
            throw failure(exception);
        }
    }

    @Override
    public void delete(String key) {
        Path file = resolve(key);
        if (Files.isSymbolicLink(file)) throw invalidKey();
        try {
            Files.deleteIfExists(file);
        } catch (IOException exception) {
            throw failure(exception);
        }
    }

    private Path resolve(String key) {
        if (key == null || !key.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(png|jpg)")) {
            throw invalidKey();
        }
        return root.resolve(key);
    }

    private UploadImageException invalidKey() {
        return new UploadImageException("Invalid image storage key", null, HttpStatus.BAD_REQUEST);
    }

    private UploadImageException failure(IOException exception) {
        return new UploadImageException("Could not access game media storage", exception, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
