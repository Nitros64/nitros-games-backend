package com.nitros64.nitros_games_backend.service.interfaces;

import java.util.List;

import com.nitros64.nitros_games_backend.model.entity.ProgramToolType;

/********************************************************************************/
/*                      PROGRAMMING TOOL TYPE SERVICE                           */
/********************************************************************************/

public interface ProgramToolTypeService extends BaseService<ProgramToolType,Long>{
    void addAll(List<ProgramToolType> programtooltype) throws Exception;
}
