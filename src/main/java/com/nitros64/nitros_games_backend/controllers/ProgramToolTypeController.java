package com.nitros64.nitros_games_backend.controllers;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nitros64.nitros_games_backend.model.entity.ProgramToolType;
import com.nitros64.nitros_games_backend.service.interfaces.ProgramToolTypeService;

@Validated
@RestController
@CrossOrigin(origins = "*")
@RequestMapping(path = "api/v1/programtooltypes")
public class ProgramToolTypeController extends BaseControllerImpl<ProgramToolType, ProgramToolTypeService>{
    
//    @PostMapping("addAll")
//    public ResponseEntity<?> addAll(@RequestBody List<ProgramToolType> programtooltype) throws Exception{
//        this.servicio.addAll(programtooltype);
//        return null;
//    }
    
}
