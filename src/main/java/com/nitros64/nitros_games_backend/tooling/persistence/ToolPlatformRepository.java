package com.nitros64.nitros_games_backend.tooling.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nitros64.nitros_games_backend.tooling.domain.ToolPlatform;
import com.nitros64.nitros_games_backend.tooling.domain.ToolPlatformId;

public interface ToolPlatformRepository extends JpaRepository<ToolPlatform, ToolPlatformId> {
}
