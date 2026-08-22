package com.nitros64.nitros_games_backend.service.impl;

import java.io.Serializable;
import java.util.List;
import jakarta.transaction.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import com.nitros64.nitros_games_backend.exception.httpexception.NotFoundException;
import com.nitros64.nitros_games_backend.model.entity.Base;
import com.nitros64.nitros_games_backend.repositories.BaseRepository;
import com.nitros64.nitros_games_backend.service.interfaces.BaseService;


public abstract class BaseServiceImpl<E extends Base, ID extends Serializable> implements BaseService<E, ID> {
    
    protected BaseRepository<E,ID> baseRepository;
    
    public BaseServiceImpl(BaseRepository<E,ID> baseRepository){
        this.baseRepository = baseRepository;
    }

    @Override
    @Transactional
    public List<E> findAll() throws Exception {
        try {
            return this.baseRepository.findAll();
        } catch (Exception e) {
             throw new Exception(e.getMessage());
        }        
    }
    
    @Override
    @Transactional
    public Page<E> findAll(Pageable pageable) throws Exception {
        try {
            return this.baseRepository.findAll(pageable);
        } catch (Exception e) {
             throw new Exception(e.getMessage());
        }
    }

    @Override
    @Transactional
    public E findById(ID id) throws Exception {
        return this.baseRepository.findById(id).orElseThrow( () -> new NotFoundException("Entity Not Found", HttpStatus.NOT_FOUND) );
    }

    @Override
    @Transactional
    public E save(E entity) throws Exception {
        return this.baseRepository.save(entity);
    }

    @Override
    @Transactional
    public List<E> saveAll(List<E> entity) throws Exception{
        //try {            
            return this.baseRepository.saveAll(entity);
//        } catch (Exception e) {
//            throw new Exception(e.getMessage());
//        }
    }

    @Override
    @Transactional
    public E update(ID id, E entity) throws Exception {
        // try {
            E entityOptional = this.findById(id);
            entity.setId(entityOptional.getId());
            return this.baseRepository.save(entity);
            
//        } catch (Exception e) {
//            throw new Exception(e.getMessage());
//        }
    }

    @Override
    @Transactional
    public boolean delete(ID id) throws Exception {
         try {             
            if(this.baseRepository.existsById(id)){
                this.baseRepository.deleteById(id);
                return true;
            }
            else {
                new Exception();
            }
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
         return false;
    }
    
}
