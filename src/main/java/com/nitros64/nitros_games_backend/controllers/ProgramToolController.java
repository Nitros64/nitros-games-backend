package com.nitros64.nitros_games_backend.controllers;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nitros64.nitros_games_backend.model.entity.ProgrammingTool;
import com.nitros64.nitros_games_backend.service.impl.ProgramToolServiceImpl;

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
