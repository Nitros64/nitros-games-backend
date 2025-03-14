package com.nitros64.nitros_games_backend.filehandling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileHostImageHandler {
    @Autowired
    @Qualifier("FileHostImageStorage")
    private FilesStorageService filehost_image_storage;
    private static final String IMAGE_DIRECTORY = "uploadImageFileHost";

    public String manage(MultipartFile file){
        return filehost_image_storage.write(file,IMAGE_DIRECTORY);
    }

    public boolean delete(String filename){
        return filehost_image_storage.delete(filename);
    }

    public void deleteAll(){
        filehost_image_storage.deleteAll();
    }

}