package com.nitros64.nitros_games_backend.tooling.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nitros64.nitros_games_backend.tooling.domain.LanguageTool;
import com.nitros64.nitros_games_backend.tooling.domain.LanguageToolId;

public interface LanguageToolRepository extends JpaRepository<LanguageTool, LanguageToolId> {

    // INSERT rather than merge: concurrent attempts must also report a duplicate.
    @Modifying
    @Query(value = """
            INSERT INTO tool_lang (program_lang_id, program_tool_id)
            VALUES (:languageId, :toolId)
            """, nativeQuery = true)
    void insert(@Param("languageId") Long languageId, @Param("toolId") Long toolId);
}
