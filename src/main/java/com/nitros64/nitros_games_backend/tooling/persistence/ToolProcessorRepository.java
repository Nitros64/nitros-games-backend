package com.nitros64.nitros_games_backend.tooling.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nitros64.nitros_games_backend.tooling.domain.ToolProcessor;
import com.nitros64.nitros_games_backend.tooling.domain.ToolProcessorId;

public interface ToolProcessorRepository extends JpaRepository<ToolProcessor, ToolProcessorId> {
}
