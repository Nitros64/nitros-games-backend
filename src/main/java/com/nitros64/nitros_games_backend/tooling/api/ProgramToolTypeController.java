package com.nitros64.nitros_games_backend.tooling.api;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nitros64.nitros_games_backend.shared.api.BaseControllerImpl;
import com.nitros64.nitros_games_backend.tooling.application.ProgramToolTypeService;
import com.nitros64.nitros_games_backend.tooling.domain.ProgramToolType;

@Validated
@RestController
@RequestMapping(path = "api/v1/programtooltypes")
public class ProgramToolTypeController extends BaseControllerImpl<ProgramToolType, ProgramToolTypeService>{
    
//    @PostMapping("addAll")
//    public ResponseEntity<?> addAll(@RequestBody List<ProgramToolType> programtooltype) throws Exception{
//        this.servicio.addAll(programtooltype);
//        return null;
//    }
    
}
