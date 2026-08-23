package com.nitros64.nitros_games_backend.tooling.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nitros64.nitros_games_backend.tooling.domain.LanguageTool;
import com.nitros64.nitros_games_backend.tooling.domain.LanguageToolId;

public interface LanguageToolRepository extends JpaRepository<LanguageTool, LanguageToolId> {
}
