package com.nitros64.nitros_games_backend.tooling.application;

import java.util.List;

import com.nitros64.nitros_games_backend.tooling.domain.ProgrammingLanguage;
import com.nitros64.nitros_games_backend.shared.application.BaseService;

    /********************************************************************************
     *                      PROGRAMMING LANGUAGE SERVICE                            *
     ********************************************************************************/

public interface ProgramLangService extends BaseService<ProgrammingLanguage,Long>{
    void addAll(List<ProgrammingLanguage> programlangs) throws Exception;
}
