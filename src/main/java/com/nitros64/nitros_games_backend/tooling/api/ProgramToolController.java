package com.nitros64.nitros_games_backend.tooling.api;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nitros64.nitros_games_backend.shared.api.BaseControllerImpl;
import com.nitros64.nitros_games_backend.tooling.application.ProgramToolServiceImpl;
import com.nitros64.nitros_games_backend.tooling.domain.ProgrammingTool;

    /********************************************************************************
     *                                                                              *
     *                      PROGRAMMING TOOL CONTROLLER                             *
     *                                                                              *
     ********************************************************************************/

@RestController
@CrossOrigin(origins = "*")
@RequestMapping(path = "api/v1/programmingtools")
public class ProgramToolController extends BaseControllerImpl<ProgrammingTool, ProgramToolServiceImpl>{
    
}
