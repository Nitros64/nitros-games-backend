package com.nitros64.nitros_games_backend.controllers;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nitros64.nitros_games_backend.model.entity.Processor;
import com.nitros64.nitros_games_backend.service.impl.ProcessorService;

/*******************************************************************************
 *                           PROCESSOR CONTROLLER                              *
 ******************************************************************************/

@RestController
@CrossOrigin(origins = "*")
@RequestMapping(path = "api/v1/processor")
public class ProcessorController extends BaseControllerImpl<Processor, ProcessorService>{
}
