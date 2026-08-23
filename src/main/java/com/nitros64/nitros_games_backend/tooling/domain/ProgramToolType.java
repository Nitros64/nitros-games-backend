package com.nitros64.nitros_games_backend.tooling.domain;

import com.nitros64.nitros_games_backend.shared.domain.AbstractEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "programtool_type")
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class ProgramToolType extends AbstractEntity {
    
    private static final long serialVersionUID = 1L;

    @Column(nullable = false, unique = true)
    private String name;

    public void rename(String name) {
        this.name = name;
    }
}
