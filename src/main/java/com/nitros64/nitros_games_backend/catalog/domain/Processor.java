package com.nitros64.nitros_games_backend.catalog.domain;

import com.nitros64.nitros_games_backend.shared.domain.Base;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "processor")
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class Processor extends Base{
    
    private static final long serialVersionUID = 1L;

    @Column(nullable = false, unique = true)
    private String name;  
}

/*
    CONTROLLER READY
    SERVICE READY
    REPOSITORY READY
 */
