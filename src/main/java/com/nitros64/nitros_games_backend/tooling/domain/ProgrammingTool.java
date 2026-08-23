package com.nitros64.nitros_games_backend.tooling.domain;

import java.net.URL;

import com.nitros64.nitros_games_backend.shared.domain.Base;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "program_tool")
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class ProgrammingTool extends Base {

    private static final long serialVersionUID = 1L;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true)
    private URL webPage;

    @Column(nullable = false, unique = true)
    private String imagefilePath;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_gametooltype")
    private ProgramToolType toolType;
}
