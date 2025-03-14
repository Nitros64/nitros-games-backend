package com.nitros64.nitros_games_backend.controllers;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nitros64.nitros_games_backend.model.entity.DevelopmentDifficulty;
import com.nitros64.nitros_games_backend.service.impl.DevelopDifficultyService;

/********************************************************************************
*                     DEVELOPMENT DIFFICULTY CONTROLLER                         *
*********************************************************************************/

@RestController
@CrossOrigin(origins = "*")
@RequestMapping(path = "api/v1/developmentdifficulty")
public class DevelopmentDifficultyController extends BaseControllerImpl<DevelopmentDifficulty, DevelopDifficultyService> {
}
