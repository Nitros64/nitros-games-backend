package com.nitros64.nitros_games_backend.shared.application;

import java.io.Serializable;
import java.util.List;
import jakarta.transaction.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.nitros64.nitros_games_backend.shared.domain.Base;
import com.nitros64.nitros_games_backend.shared.persistence.BaseRepository;


public abstract class BaseServiceImpl<E extends Base, ID extends Serializable> implements BaseService<E, ID> {
    
    protected BaseRepository<E,ID> baseRepository;
    
    public BaseServiceImpl(BaseRepository<E,ID> baseRepository){
        this.baseRepository = baseRepository;
    }

    @Override
    @Transactional
    public List<E> findAll() {
        return this.baseRepository.findAll();
    }
    
    @Override
    @Transactional
    public Page<E> findAll(Pageable pageable) {
        return this.baseRepository.findAll(pageable);
    }

    @Override
    @Transactional
    public E findById(ID id) {
        return this.baseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entity not found"));
    }

    @Override
    @Transactional
    public E save(E entity) {
        return this.baseRepository.save(entity);
    }

    @Override
    @Transactional
    public List<E> saveAll(List<E> entity) {
        return this.baseRepository.saveAll(entity);
    }

    @Override
    @Transactional
    public E update(ID id, E entity) {
        E entityOptional = this.findById(id);
        entity.setId(entityOptional.getId());
        return this.baseRepository.save(entity);
    }

    @Override
    @Transactional
    public boolean delete(ID id) {
        if (this.baseRepository.existsById(id)) {
            this.baseRepository.deleteById(id);
            return true;
        }
        return false;
    }
    
}
