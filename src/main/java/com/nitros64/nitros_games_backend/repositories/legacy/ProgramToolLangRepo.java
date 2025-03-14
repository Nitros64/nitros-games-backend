package com.nitros64.nitros_games_backend.repositories.legacy;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.nitros64.nitros_games_backend.model.relation_entity.LanguageTool;
import com.nitros64.nitros_games_backend.model.relation_entity.LanguageToolId;

@Repository
public interface ProgramToolLangRepo extends CrudRepository<LanguageTool, LanguageToolId>{
    
}
