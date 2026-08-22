package com.nitros64.nitros_games_backend.tooling.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nitros64.nitros_games_backend.tooling.domain.ToolPlatform;
import com.nitros64.nitros_games_backend.tooling.domain.ToolPlatformId;

@Repository
public interface IToolPlatformDao extends JpaRepository<ToolPlatform, ToolPlatformId>{
    
}
