package com.nitros64.nitros_games_backend.repositories;

import java.io.Serializable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;


@NoRepositoryBean //Esto se pone para no poder crear repertorios de esta interfaz
public interface BaseRepository<E extends Object, ID extends Serializable> extends JpaRepository<E,ID>{

}
