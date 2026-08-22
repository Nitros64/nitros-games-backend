package com.nitros64.nitros_games_backend.catalog.api;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nitros64.nitros_games_backend.catalog.application.GameGenreService;
import com.nitros64.nitros_games_backend.catalog.domain.GameGenre;
import com.nitros64.nitros_games_backend.shared.api.BaseControllerImpl;

/*********************************************************************
 *                      GAME GENRE CONTROLLER                        *
 ********************************************************************/

@RestController
@RequestMapping(path = "api/v1/gamegenre")
public class GameGenreController extends BaseControllerImpl<GameGenre, GameGenreService>{
}
