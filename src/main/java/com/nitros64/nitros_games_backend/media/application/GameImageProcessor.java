package com.nitros64.nitros_games_backend.media.application;

public interface GameImageProcessor {
    ProcessedImage process(String filename, String contentType, byte[] content);

    record ProcessedImage(byte[] content, String extension, String contentType, int width, int height) {}
}
