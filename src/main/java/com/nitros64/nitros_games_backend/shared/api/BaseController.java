package com.nitros64.nitros_games_backend.shared.api;

import java.io.Serializable;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import com.nitros64.nitros_games_backend.shared.domain.Base;

import jakarta.validation.Valid;


public interface BaseController <E extends Base, ID extends Serializable>{
    ResponseEntity<?> getAll();
    ResponseEntity<?> getAll(Pageable paeable);
    ResponseEntity<?> getOne(@PathVariable ID id);
    ResponseEntity<?> save(@Valid @RequestBody E entity);
    ResponseEntity<?> saveAll(@RequestBody List<@Valid E> entity);
    ResponseEntity<?> update(@PathVariable ID id, @Valid @RequestBody E entity);
    ResponseEntity<?> delete(@PathVariable ID id);
}
