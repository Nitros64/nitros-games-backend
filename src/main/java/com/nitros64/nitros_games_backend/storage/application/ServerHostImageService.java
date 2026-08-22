package com.nitros64.nitros_games_backend.storage.application;

import com.nitros64.nitros_games_backend.shared.application.BaseServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.nitros64.nitros_games_backend.storage.domain.ServerHostImage;
import com.nitros64.nitros_games_backend.shared.persistence.BaseRepository;
import com.nitros64.nitros_games_backend.storage.persistence.ServerHostImageRepository;

import java.util.Optional;

/********************************************************************************
 *                      SERVERHOST IMAGE SERVICE IMPLEMENTATION                 *
 ********************************************************************************/

@Service
public class ServerHostImageService extends BaseServiceImpl<ServerHostImage,Long>{

    @Autowired
    private ServerHostImageRepository serverHostImageRepository;

    public ServerHostImageService(BaseRepository<ServerHostImage, Long> baseRepository) {
        super(baseRepository);
    }

    public Optional<ServerHostImage> findByName(String name){
        return this.serverHostImageRepository.findByName(name);
    }
}
