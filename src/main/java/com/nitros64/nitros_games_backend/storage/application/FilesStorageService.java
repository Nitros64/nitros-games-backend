package com.nitros64.nitros_games_backend.storage.application;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FilesStorageService {
    void init();
    void init(String customDirectory);
    String write(MultipartFile file);
    String write(MultipartFile file, String customDirectory);
    Resource load(String filename);
    Resource load(String customDirectory, String filename);
    boolean delete(String filename);
    boolean delete(String customDirectory, String filename );
    void deleteAll();
}
