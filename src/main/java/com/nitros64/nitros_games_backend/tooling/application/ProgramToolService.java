package com.nitros64.nitros_games_backend.tooling.application;

import java.util.List;

import com.nitros64.nitros_games_backend.tooling.domain.ProgrammingTool;
import com.nitros64.nitros_games_backend.shared.application.BaseService;

/********************************************************************************/
/*                      PROGRAMMING TOOL SERVICE                                */
/********************************************************************************/

public interface ProgramToolService extends BaseService<ProgrammingTool,Long>{

    ProgrammingTool create(SaveProgrammingToolCommand command);

    List<ProgrammingTool> createAll(List<SaveProgrammingToolCommand> commands);

    ProgrammingTool updateFromCommand(Long id, SaveProgrammingToolCommand command);
}
