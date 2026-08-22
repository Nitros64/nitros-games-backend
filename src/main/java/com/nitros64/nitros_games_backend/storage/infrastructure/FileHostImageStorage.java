package com.nitros64.nitros_games_backend.storage.infrastructure;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import com.nitros64.nitros_games_backend.storage.application.FilesStorageService;
import com.nitros64.nitros_games_backend.storage.application.UploadImageException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service("FileHostImageStorage")
public class FileHostImageStorage implements FilesStorageService{

    private Path root = Paths.get("uploadImageFileHost");
    private static final String LOAD_ERROR_MESSAGE = "Error al cargar la imagen ";
    private static final String LOAD_ERROR_MESSAGE2 = "MalformedURLException";
    private static final String WRITE_ERROR_MESSAGE = "Error archivo vacio o nulo";
    private static final String WRITE_ERROR_MESSAGE2 = "Error al subir la imagen ";
    private static final String DELETE_ERROR_MESSAGE = "Error al borrar la imagen ";

    @Override
    public void init() {
        try {
            if(!Files.isDirectory(root))
                Files.createDirectory(root);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize folder for upload!");
        }
    }

    @Override
    public void init(String customDirectory) {
        try {
            this.root = Paths.get(customDirectory);
            if(!Files.isDirectory(root))
                Files.createDirectory(root);

        } catch (IOException e) {
            throw new RuntimeException("Could not initialize folder for upload!");
        }
    }

    @Override
    public String write(MultipartFile file) {
        if(file.isEmpty())
            throw new UploadImageException(WRITE_ERROR_MESSAGE, null, HttpStatus.BAD_REQUEST);

        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename().replace(" ", "");
        try {
            Files.copy(file.getInputStream(), this.root.resolve(fileName));
        }catch (IOException e) {
            throw new UploadImageException(WRITE_ERROR_MESSAGE2 + fileName, e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return fileName;
    }

    @Override
    public String write(MultipartFile file, String customDirectory) {
        init(customDirectory);
        return this.write(file);
    }

    @Override
    public Resource load(String filename) {
        try {
            Path file = root.resolve(filename);
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new UploadImageException(LOAD_ERROR_MESSAGE + filename, new RuntimeException("Could not read the file!"), HttpStatus.NOT_FOUND);
            }

        } catch (MalformedURLException e) {
            throw new UploadImageException(LOAD_ERROR_MESSAGE2 + filename, e, HttpStatus.NOT_FOUND);
        }
    }

    @Override
    public Resource load(String customDirectory, String filename) {
        root = Paths.get(customDirectory);
        return this.load(filename);
    }

    @Override
    public boolean delete( String filename ) {
        try {
            return this.load(filename).getFile().delete();
        } catch (IOException e) {
            throw new UploadImageException(DELETE_ERROR_MESSAGE + filename, e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public boolean delete(String customDirectory, String filename ) {
        root = Paths.get(customDirectory);
        return delete(filename);
    }

//    @Override
//    public Path getPath(String nombreFoto){
//        Path rutaArchivo = null;
//        try {
//            rutaArchivo = root.resolve(nombreFoto).toAbsolutePath();
//        }catch(InvalidPathException e){
//            throw new UploadImageException("Path Error " + e, e.getCause(),HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//
//        return rutaArchivo;
//    }

    @Override
    public void deleteAll() {
        FileSystemUtils.deleteRecursively(root.toFile());
    }
}

/*
    Este servicio gestiona la subida imagenes logotipos de empresas de hosteo de archivos,
    tales empresas pueden ser Mega, DropBox, Etc
 */
