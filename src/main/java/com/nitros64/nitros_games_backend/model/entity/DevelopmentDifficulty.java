package com.nitros64.nitros_games_backend.model.entity;

import com.nitros64.nitros_games_backend.constrait.NoNumberString;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "DevDifficulty")
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class DevelopmentDifficulty extends Base {
    
    private static final long serialVersionUID = 1L;

    @NoNumberString
    @NotNull(message="cannot be null")
    @NotEmpty(message = "no puede estar vacio")
    @NotBlank(message = "No se permite campo en blanco")
    @Size(min = 4, max = 30, message="el tamaño tiene que estar entre 4 y 30")
    @Column(nullable = false, unique = true)
    private String name;
}

/*
    CONTROLLER READY
    SERVICE READY
    REPOSITORY READY
 */