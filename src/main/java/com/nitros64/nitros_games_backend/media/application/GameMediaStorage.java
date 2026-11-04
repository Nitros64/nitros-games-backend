package com.nitros64.nitros_games_backend.media.application;

/** Private immutable objects. An S3 adapter must also keep objects private and serve reads through this port. */
public interface GameMediaStorage {
    String store(byte[] content, String extension);
    byte[] load(String key);
    void delete(String key);
}
