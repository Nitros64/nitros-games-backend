package com.nitros64.nitros_games_backend.storage.application;

import org.springframework.web.multipart.MultipartFile;

public interface HostImageStorage {
    String store(MultipartFile file);
    boolean delete(String storageKey);
}
