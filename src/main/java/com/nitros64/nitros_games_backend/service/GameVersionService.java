package com.nitros64.nitros_games_backend.service;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.nitros64.nitros_games_backend.model.entity.GameData;
import com.nitros64.nitros_games_backend.model.relation_entity.GameVersion;
import com.nitros64.nitros_games_backend.repositories.customEntityManager.IGameVersionDaoEM;
import com.nitros64.nitros_games_backend.repositories.legacy.GameDataRepository;

@Transactional
@Service
public class GameVersionService {
    
    @Autowired
    private GameDataRepository gdr;
    
    @Autowired
    @Qualifier("EntityManagerVersion")
    private IGameVersionDaoEM<GameVersion> gameversionDao; // Using Entity Manager 
    
    public GameVersion save(GameVersion arg0){
        return gameversionDao.save(arg0);
    }
    
    //cascadeversion deberia revisarse
    public <S extends GameData> S save(S arg0, boolean cascade_version){
        gdr.save(arg0);
        if(cascade_version){
            arg0.getVersionList().forEach(v -> this.save(v) );
        }        
        return arg0;
    }
    
     public GameVersion getID(GameVersion arg0){
         return gameversionDao.getID(arg0);
     }
}