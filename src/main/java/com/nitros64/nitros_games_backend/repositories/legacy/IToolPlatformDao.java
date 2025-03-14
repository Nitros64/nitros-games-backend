package com.nitros64.nitros_games_backend.repositories.legacy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nitros64.nitros_games_backend.model.relation_entity.ToolPlatform;
import com.nitros64.nitros_games_backend.model.relation_entity.ToolPlatformId;

@Repository
public interface IToolPlatformDao extends JpaRepository<ToolPlatform, ToolPlatformId>{
    
}
