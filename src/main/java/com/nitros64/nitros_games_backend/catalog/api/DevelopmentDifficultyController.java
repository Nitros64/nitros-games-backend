package com.nitros64.nitros_games_backend.catalog.api;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nitros64.nitros_games_backend.catalog.application.DevelopDifficultyService;
import com.nitros64.nitros_games_backend.catalog.domain.DevelopmentDifficulty;
import com.nitros64.nitros_games_backend.shared.api.BaseControllerImpl;

/********************************************************************************
*                     DEVELOPMENT DIFFICULTY CONTROLLER                         *
*********************************************************************************/

@RestController
@CrossOrigin(origins = "*")
@RequestMapping(path = "api/v1/developmentdifficulty")
public class DevelopmentDifficultyController extends BaseControllerImpl<DevelopmentDifficulty, DevelopDifficultyService> {
}
