package com.nitros64.nitros_games_backend.tooling.api;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nitros64.nitros_games_backend.controllers.BaseControllerImpl;
import com.nitros64.nitros_games_backend.tooling.application.ProgramLangServiceImpl;
import com.nitros64.nitros_games_backend.tooling.domain.ProgrammingLanguage;

    /********************************************************************************
     *                      PROGRAMMING LANGUAGE CONTROLLER                         *
     ********************************************************************************/

@RestController
@CrossOrigin(origins = "*")
@RequestMapping(path = "api/v1/programlanguages")
public class ProgramLanguageController extends BaseControllerImpl<ProgrammingLanguage, ProgramLangServiceImpl>{
    
}
