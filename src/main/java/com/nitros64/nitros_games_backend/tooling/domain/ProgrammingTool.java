package com.nitros64.nitros_games_backend.tooling.domain;

import java.net.URL;

import com.nitros64.nitros_games_backend.shared.domain.AbstractEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "program_tool")
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class ProgrammingTool extends AbstractEntity {

    private static final long serialVersionUID = 1L;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "web_page", nullable = false, unique = true)
    private URL webPage;

    @Column(name = "imagefile_path", nullable = false, unique = true)
    private String imagefilePath;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_gametooltype")
    private ProgramToolType toolType;

    public void updateDetails(
            String name,
            URL webPage,
            String imagefilePath,
            ProgramToolType toolType) {
        this.name = name;
        this.webPage = webPage;
        this.imagefilePath = imagefilePath;
        this.toolType = toolType;
    }
}
