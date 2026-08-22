package com.nitros64.nitros_games_backend.tooling.application;

import java.util.List;

import com.nitros64.nitros_games_backend.tooling.domain.ProgramToolType;
import com.nitros64.nitros_games_backend.shared.application.BaseService;

/********************************************************************************/
/*                      PROGRAMMING TOOL TYPE SERVICE                           */
/********************************************************************************/

public interface ProgramToolTypeService extends BaseService<ProgramToolType,Long>{
    void addAll(List<ProgramToolType> programtooltype) throws Exception;
}
