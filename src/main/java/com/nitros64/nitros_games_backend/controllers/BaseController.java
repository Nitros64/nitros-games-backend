package com.nitros64.nitros_games_backend.controllers;

import java.io.Serializable;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import com.nitros64.nitros_games_backend.model.entity.Base;

import jakarta.validation.Valid;


public interface BaseController <E extends Base, ID extends Serializable>{
    ResponseEntity<?> getAll();
    ResponseEntity<?> getAll(Pageable paeable);
    ResponseEntity<?> getOne(@PathVariable ID id) throws Exception;
    ResponseEntity<?> save(@Valid @RequestBody E entity) throws Exception;
    //ResponseEntity<?> save(@RequestBody E entity, BindingResult result);
    ResponseEntity<?> saveAll(@Valid @RequestBody List<E> entity) throws Exception;
    ResponseEntity<?> update(@PathVariable ID id, @RequestBody E entity) throws Exception;
    ResponseEntity<?> delete(@PathVariable ID id) throws Exception;
}
