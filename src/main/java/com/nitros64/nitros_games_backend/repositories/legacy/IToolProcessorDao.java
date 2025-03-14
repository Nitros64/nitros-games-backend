package com.nitros64.nitros_games_backend.repositories.legacy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nitros64.nitros_games_backend.model.relation_entity.ToolProcessor;
import com.nitros64.nitros_games_backend.model.relation_entity.ToolProcessorId;

@Repository
public interface IToolProcessorDao extends JpaRepository<ToolProcessor, ToolProcessorId>{
    
}
