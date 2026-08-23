package com.nitros64.nitros_games_backend.storage.application;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.nitros64.nitros_games_backend.shared.application.ResourceNotFoundException;
import com.nitros64.nitros_games_backend.storage.domain.ServerHostImage;
import com.nitros64.nitros_games_backend.storage.persistence.ServerHostImageRepository;

@Service
public class ServerHostImageService {

    private final ServerHostImageRepository images;
    private final FileHostImageHandler fileHandler;

    public ServerHostImageService(
            ServerHostImageRepository images,
            FileHostImageHandler fileHandler) {
        this.images = images;
        this.fileHandler = fileHandler;
    }

    @Transactional(readOnly = true)
    public List<ServerHostImage> findAll() {
        return images.findAll();
    }

    @Transactional(readOnly = true)
    public Page<ServerHostImage> findAll(Pageable pageable) {
        return images.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<ServerHostImage> searchByName(String name, Pageable pageable) {
        return images.findByNameContainingIgnoreCase(name.strip(), pageable);
    }

    @Transactional(readOnly = true)
    public ServerHostImage findById(Long id) {
        return images.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Host image not found"));
    }

    @Transactional
    public ServerHostImage create(String name, MultipartFile file) {
        String filename = fileHandler.store(file);
        fileHandler.deleteOnRollback(filename);
        return images.saveAndFlush(new ServerHostImage(name, filename));
    }

    @Transactional
    public ServerHostImage updateImage(Long id, String name, MultipartFile file) {
        ServerHostImage entity = findById(id);
        String oldFilename = entity.getImagepath();
        String newFilename = fileHandler.store(file);
        fileHandler.deleteOnRollback(newFilename);

        entity.setName(name);
        entity.setImagepath(newFilename);
        ServerHostImage updated = images.saveAndFlush(entity);
        fileHandler.deleteAfterCommit(oldFilename);
        return updated;
    }

    @Transactional
    public ServerHostImage updateName(Long id, String name) {
        ServerHostImage entity = findById(id);
        entity.setName(name);
        return images.saveAndFlush(entity);
    }

    @Transactional
    public void deleteWithFile(Long id) {
        ServerHostImage entity = findById(id);
        String filename = entity.getImagepath();
        images.delete(entity);
        images.flush();
        fileHandler.deleteAfterCommit(filename);
    }
}
