package com.nitros64.nitros_games_backend.shared.application;

import java.io.Serializable;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.nitros64.nitros_games_backend.shared.domain.Base;

public interface BaseService <E extends Base, ID extends Serializable>{
    List<E> findAll();
    Page<E> findAll(Pageable pageable);
    E findById(ID id);
    //List<E> findByName(String name) throws Exception;
    E save(E entity);
    List<E> saveAll(List<E> entity);
    E update(ID id, E entity);
    boolean delete(ID id);
}
