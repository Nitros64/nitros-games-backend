package com.nitros64.nitros_games_backend.tooling.domain;

import com.nitros64.nitros_games_backend.model.entity.Base;

import com.nitros64.nitros_games_backend.constrait.NoNumberString;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "programtool_type")
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class ProgramToolType extends Base{
    
    private static final long serialVersionUID = 1L;

    @NoNumberString
    @NotEmpty(message = "el campo 'name' no puede estar vacio")
    @Size(min = 4, max = 30, message="el campo 'name' debe tener un tamaño entre 4 y 30")
    @Column(nullable = false, unique = true)
    private String name;
}

/*
    CONTROLLER READY
    SERVICE READY
    REPOSITORY READY
 */
