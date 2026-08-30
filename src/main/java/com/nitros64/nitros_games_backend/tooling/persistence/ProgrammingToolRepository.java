package com.nitros64.nitros_games_backend.tooling.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nitros64.nitros_games_backend.tooling.domain.ProgrammingTool;

public interface ProgrammingToolRepository extends JpaRepository<ProgrammingTool, Long> {

    @EntityGraph(attributePaths = "toolType")
    @Query("select tool from ProgrammingTool tool where tool.id = :id")
    Optional<ProgrammingTool> findDetailedById(@Param("id") Long id);

    @EntityGraph(attributePaths = "toolType")
    @Query("select tool from ProgrammingTool tool")
    List<ProgrammingTool> findAllDetailed();

    @EntityGraph(attributePaths = "toolType")
    @Query(
            value = "select tool from ProgrammingTool tool",
            countQuery = "select count(tool) from ProgrammingTool tool")
    Page<ProgrammingTool> findAllDetailed(Pageable pageable);

    @EntityGraph(attributePaths = "toolType")
    @Query(
            value = """
                    select tool
                    from ProgrammingTool tool
                    where (:name is null
                            or lower(tool.name) like lower(concat('%', :name, '%')))
                      and (:toolTypeId is null or tool.toolType.id = :toolTypeId)
                      and (:languageId is null or exists (
                            select languageTool
                            from LanguageTool languageTool
                            where languageTool.programmingTool = tool
                              and languageTool.programmingLanguage.id = :languageId))
                    """,
            countQuery = """
                    select count(tool)
                    from ProgrammingTool tool
                    where (:name is null
                            or lower(tool.name) like lower(concat('%', :name, '%')))
                      and (:toolTypeId is null or tool.toolType.id = :toolTypeId)
                      and (:languageId is null or exists (
                            select languageTool
                            from LanguageTool languageTool
                            where languageTool.programmingTool = tool
                              and languageTool.programmingLanguage.id = :languageId))
                    """)
    Page<ProgrammingTool> search(
            @Param("name") String name,
            @Param("toolTypeId") Long toolTypeId,
            @Param("languageId") Long languageId,
            Pageable pageable);
}
