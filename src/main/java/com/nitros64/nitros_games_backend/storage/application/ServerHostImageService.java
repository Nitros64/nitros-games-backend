package com.nitros64.nitros_games_backend.storage.application;

import com.nitros64.nitros_games_backend.shared.application.BaseServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.nitros64.nitros_games_backend.storage.domain.ServerHostImage;
import com.nitros64.nitros_games_backend.storage.persistence.ServerHostImageRepository;

import java.util.Optional;

import jakarta.transaction.Transactional;

/********************************************************************************
 *                      SERVERHOST IMAGE SERVICE IMPLEMENTATION                 *
 ********************************************************************************/

@Service
public class ServerHostImageService extends BaseServiceImpl<ServerHostImage,Long>{

    private final ServerHostImageRepository repository;
    private final FileHostImageHandler fileHandler;

    public ServerHostImageService(
            ServerHostImageRepository repository,
            FileHostImageHandler fileHandler) {
        super(repository);
        this.repository = repository;
        this.fileHandler = fileHandler;
    }

    public Optional<ServerHostImage> findByName(String name) {
        return repository.findByName(name);
    }

    @Transactional
    public ServerHostImage create(String name, MultipartFile file) {
        String filename = fileHandler.store(file);
        fileHandler.deleteOnRollback(filename);
        return repository.saveAndFlush(new ServerHostImage(name, filename));
    }

    @Transactional
    public ServerHostImage updateImage(Long id, String name, MultipartFile file) {
        ServerHostImage entity = findById(id);
        String oldFilename = entity.getImagepath();
        String newFilename = fileHandler.store(file);
        fileHandler.deleteOnRollback(newFilename);

        entity.setName(name);
        entity.setImagepath(newFilename);
        ServerHostImage updated = repository.saveAndFlush(entity);
        fileHandler.deleteAfterCommit(oldFilename);
        return updated;
    }

    @Transactional
    public ServerHostImage updateName(Long id, String name) {
        ServerHostImage entity = findById(id);
        entity.setName(name);
        return repository.saveAndFlush(entity);
    }

    @Transactional
    public void deleteWithFile(Long id) {
        ServerHostImage entity = findById(id);
        String filename = entity.getImagepath();
        repository.delete(entity);
        repository.flush();
        fileHandler.deleteAfterCommit(filename);
    }
}
