package com.nitros64.nitros_games_backend.media.infrastructure;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import com.nitros64.nitros_games_backend.media.application.GameImageProcessor;
import com.nitros64.nitros_games_backend.storage.application.UploadImageException;

@Component
public class ImageIoGameImageProcessor implements GameImageProcessor {
    private final GameMediaProperties properties;

    public ImageIoGameImageProcessor(GameMediaProperties properties) {
        this.properties = properties;
    }

    @Override
    public ProcessedImage process(String filename, String contentType, byte[] content) {
        if (filename == null || !filename.matches("[\\p{L}\\p{N} _().-]{1,120}")
                || filename.startsWith(".") || filename.contains("..")) {
            throw error("Invalid image filename", HttpStatus.BAD_REQUEST);
        }
        if (content.length == 0) throw error("The uploaded image is empty", HttpStatus.BAD_REQUEST);
        if (content.length > properties.getMaxBytes()) {
            throw error("The uploaded image exceeds the configured size limit", HttpStatus.CONTENT_TOO_LARGE);
        }
        String name = filename.toLowerCase(Locale.ROOT);
        String format;
        if ("image/png".equals(contentType) && name.endsWith(".png")) format = "png";
        else if ("image/jpeg".equals(contentType) && (name.endsWith(".jpg") || name.endsWith(".jpeg"))) format = "jpeg";
        else throw error("Only PNG and JPEG images with matching filename and content type are supported",
                HttpStatus.UNSUPPORTED_MEDIA_TYPE);

        try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(content))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw error("Invalid image content", HttpStatus.UNSUPPORTED_MEDIA_TYPE);
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                reader.addIIOReadWarningListener((source, warning) -> {
                    throw error("Damaged image content", HttpStatus.UNSUPPORTED_MEDIA_TYPE);
                });
                if (!reader.getFormatName().equalsIgnoreCase(format)) {
                    throw error("Image content does not match its declared format", HttpStatus.UNSUPPORTED_MEDIA_TYPE);
                }
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > properties.getMaxPixels()) {
                    throw error("Image dimensions exceed the configured pixel limit", HttpStatus.CONTENT_TOO_LARGE);
                }
                var decoded = reader.read(0);
                if (decoded == null) throw error("Invalid image content", HttpStatus.UNSUPPORTED_MEDIA_TYPE);
                var output = new ByteArrayOutputStream();
                // Re-encoding strips original metadata, trailing payloads and client-supplied filenames.
                if (!ImageIO.write(decoded, format, output)) {
                    throw error("Unsupported image format", HttpStatus.UNSUPPORTED_MEDIA_TYPE);
                }
                if (output.size() > properties.getMaxBytes()) {
                    throw error("Normalized image exceeds the configured size limit", HttpStatus.CONTENT_TOO_LARGE);
                }
                return new ProcessedImage(output.toByteArray(), format.equals("jpeg") ? "jpg" : "png",
                        contentType, width, height);
            } finally {
                reader.dispose();
            }
        } catch (IOException | IllegalArgumentException exception) {
            throw new UploadImageException("Invalid image content", exception, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        }
    }

    private UploadImageException error(String message, HttpStatus status) {
        return new UploadImageException(message, null, status);
    }
}
