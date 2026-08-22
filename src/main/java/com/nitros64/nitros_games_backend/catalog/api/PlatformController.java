package com.nitros64.nitros_games_backend.catalog.api;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nitros64.nitros_games_backend.catalog.application.PlatformService;
import com.nitros64.nitros_games_backend.catalog.domain.Platform;
import com.nitros64.nitros_games_backend.shared.api.BaseControllerImpl;

 /*******************************************************************************
 *                           PLATFORM CONTROLLER                                *
 *******************************************************************************/

@RestController
@CrossOrigin(origins = "*")
@RequestMapping(path = "api/v1/platform")
public class PlatformController extends BaseControllerImpl<Platform, PlatformService>{
}
