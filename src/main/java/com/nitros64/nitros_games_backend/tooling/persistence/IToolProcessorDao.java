package com.nitros64.nitros_games_backend.tooling.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nitros64.nitros_games_backend.tooling.domain.ToolProcessor;
import com.nitros64.nitros_games_backend.tooling.domain.ToolProcessorId;

@Repository
public interface IToolProcessorDao extends JpaRepository<ToolProcessor, ToolProcessorId>{
    
}
