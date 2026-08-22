package com.nitros64.nitros_games_backend.tooling.persistence;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.nitros64.nitros_games_backend.tooling.domain.LanguageTool;
import com.nitros64.nitros_games_backend.tooling.domain.LanguageToolId;

@Repository
public interface ProgramToolLangRepo extends CrudRepository<LanguageTool, LanguageToolId>{
    
}
