package com.nitros64.nitros_games_backend.controllers;


import java.util.List;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.nitros64.nitros_games_backend.model.entity.Base;
import com.nitros64.nitros_games_backend.service.interfaces.BaseService;

public abstract class BaseControllerImpl<E extends Base, S extends BaseService<E,Long>> implements BaseController<E, Long> {
    
    @Autowired
    protected S servicio;
    
    @Override
    @GetMapping    
    public ResponseEntity<?> getAll(){ //InvalidDefinitionException
        try {
            return ResponseEntity.status(HttpStatus.OK).body(this.servicio.findAll());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{ \"error\": \"Error. Por favor intente mas tarde.\"}");
        }
    }
    
    @Override
    @GetMapping("/paged")
    public ResponseEntity<?> getAll(Pageable pageable){
        try {
            return ResponseEntity.status(HttpStatus.OK).body(this.servicio.findAll(pageable));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{ \"error\": \"Error. Por favor intente mas tarde.\"}");
        }
    }
    
    @Override
    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(@PathVariable Long id) throws Exception {
        //try {
            return ResponseEntity.status(HttpStatus.OK).body(servicio.findById(id));
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{ \"error\": \"Error. Por favor intente mas tarde.\"}");
//        }
        
    }
    
    @Override
    @PostMapping(path = "add",consumes = {MediaType.APPLICATION_JSON_VALUE}) //@Valid
    public ResponseEntity<?> save(@RequestBody E entity) throws Exception { //HttpMessageNotReadableException
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.save(entity));
    }
    
    @Override
    @PutMapping("/{id}")
    public ResponseEntity<?> update (@PathVariable Long id, @RequestBody E entity) throws Exception{
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(servicio.update(id,entity));
    }
    
    @Override
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) throws Exception {
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(servicio.delete(id));
    }
    
    @Override
    @PostMapping("addAll")
    public ResponseEntity<?> saveAll(@Valid @RequestBody List<E> entity) throws Exception{
        //try {
            return ResponseEntity.status(HttpStatus.CREATED).body(this.servicio.saveAll(entity));
//        } catch (Exception ex) {
//            System.err.println(ex.getMessage());   
//            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
//        }
    }
}
