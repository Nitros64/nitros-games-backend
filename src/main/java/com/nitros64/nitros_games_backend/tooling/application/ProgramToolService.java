package com.nitros64.nitros_games_backend.tooling.application;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.nitros64.nitros_games_backend.tooling.domain.ProgrammingTool;

public interface ProgramToolService {

    List<ProgrammingTool> findAll();

    Page<ProgrammingTool> findAll(Pageable pageable);

    Page<ProgrammingTool> search(
            ProgrammingToolSearchCriteria criteria,
            Pageable pageable);

    ProgrammingTool findById(Long id);

    ProgrammingTool create(SaveProgrammingToolCommand command);

    List<ProgrammingTool> createAll(List<SaveProgrammingToolCommand> commands);

    ProgrammingTool updateFromCommand(Long id, SaveProgrammingToolCommand command);

    void delete(Long id);
}
