package com.nitros64.nitros_games_backend.shared.application;

import java.io.Serializable;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.nitros64.nitros_games_backend.shared.domain.Base;

public interface BaseService <E extends Base, ID extends Serializable>{
    List<E> findAll() throws Exception;
    Page<E> findAll(Pageable pageable) throws Exception;
    E findById(ID id) throws Exception;
    //List<E> findByName(String name) throws Exception;
    E save(E entity) throws Exception;
    List<E> saveAll(List<E> entity) throws Exception;
    E update(ID id, E entity) throws Exception;
    boolean delete(ID id) throws Exception;
}
