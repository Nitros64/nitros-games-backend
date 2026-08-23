package com.nitros64.nitros_games_backend.storage.domain;

import com.nitros64.nitros_games_backend.shared.domain.Base;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "server_hostimage")
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class ServerHostImage extends Base {
    private static final long serialVersionUID = 1L;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true)
    private String imagepath;

}
