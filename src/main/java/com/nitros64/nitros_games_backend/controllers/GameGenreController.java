package com.nitros64.nitros_games_backend.controllers;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nitros64.nitros_games_backend.model.entity.GameGenre;
import com.nitros64.nitros_games_backend.service.impl.GameGenreService;

/*********************************************************************
 *                      GAME GENRE CONTROLLER                        *
 ********************************************************************/

@RestController
@CrossOrigin(origins = "*")
@RequestMapping(path = "api/v1/gamegenre")
public class GameGenreController extends BaseControllerImpl<GameGenre, GameGenreService>{
}
