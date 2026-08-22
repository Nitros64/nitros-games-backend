package com.nitros64.nitros_games_backend.storage.application;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FilesStorageService {
    void init();
    String write(MultipartFile file);
    Resource load(String filename);
    boolean delete(String filename);
}
