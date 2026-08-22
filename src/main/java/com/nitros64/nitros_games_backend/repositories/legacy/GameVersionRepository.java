package com.nitros64.nitros_games_backend.repositories.legacy;


import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.nitros64.nitros_games_backend.model.relation_entity.GameVersion;
import com.nitros64.nitros_games_backend.shared.persistence.BaseRepository;

@Repository
public interface GameVersionRepository extends BaseRepository<GameVersion,Long>{
    
    @Transactional
    @Modifying //(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "insert into game_version (name, fk_gamedata, fk_idlang, fk_idtool, fk_idprocessor, fk_idplatform) values (?1, ?2, ?3, ?4, ?5, ?6)", nativeQuery = true)//SpEL Expressions
    public void save(String name, Long fk_gamedata, Long fk_idlang, Long fk_idtool, Long fk_idprocessor, Long fk_idplatform);
    
//    @Modifying(clearAutomatically=true, flushAutomatically=true)
//    @Query(value = "insert into author(first_name, last_name, version) values(?#{#author.firstName}, ?#{#author.lastName}, ?#{#author.version})",nativeQuery = true)//SpEL Expressions
//    void insert(@Param("author") Author author);
    
}

//insert into game_version (name, fk_gamedata, fk_idlang, fk_idtool, fk_idprocessor, fk_idplatform) values (?, ?, ?, ?, ?, ?)
