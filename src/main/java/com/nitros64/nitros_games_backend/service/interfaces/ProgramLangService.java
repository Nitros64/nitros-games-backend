package com.nitros64.nitros_games_backend.service.interfaces;

import java.util.List;

import com.nitros64.nitros_games_backend.model.entity.ProgrammingLanguage;

    /********************************************************************************
     *                      PROGRAMMING LANGUAGE SERVICE                            *
     ********************************************************************************/

public interface ProgramLangService extends BaseService<ProgrammingLanguage,Long>{
    void addAll(List<ProgrammingLanguage> programlangs) throws Exception;
}
