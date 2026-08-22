package com.nitros64.nitros_games_backend.catalog.api;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nitros64.nitros_games_backend.catalog.application.ProcessorService;
import com.nitros64.nitros_games_backend.catalog.domain.Processor;
import com.nitros64.nitros_games_backend.controllers.BaseControllerImpl;

/*******************************************************************************
 *                           PROCESSOR CONTROLLER                              *
 ******************************************************************************/

@RestController
@CrossOrigin(origins = "*")
@RequestMapping(path = "api/v1/processor")
public class ProcessorController extends BaseControllerImpl<Processor, ProcessorService>{
}
