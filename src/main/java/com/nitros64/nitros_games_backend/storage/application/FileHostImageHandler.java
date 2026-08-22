package com.nitros64.nitros_games_backend.storage.application;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileHostImageHandler {

    private final FilesStorageService storage;

    public FileHostImageHandler(@Qualifier("FileHostImageStorage") FilesStorageService storage) {
        this.storage = storage;
    }

    public String manage(MultipartFile file){
        return storage.write(file);
    }

    public boolean delete(String filename){
        return storage.delete(filename);
    }
}
