package com.nitros64.nitros_games_backend.storage.domain;

import com.nitros64.nitros_games_backend.shared.domain.AbstractEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "server_hostimage")
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class ServerHostImage extends AbstractEntity {
    private static final long serialVersionUID = 1L;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true)
    private String imagepath;

    public void rename(String name) {
        this.name = name;
    }

    public void replace(String name, String imagepath) {
        this.name = name;
        this.imagepath = imagepath;
    }
}
