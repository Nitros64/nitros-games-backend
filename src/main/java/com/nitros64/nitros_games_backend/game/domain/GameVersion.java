package com.nitros64.nitros_games_backend.game.domain;

import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.nitros64.nitros_games_backend.catalog.domain.Platform;
import com.nitros64.nitros_games_backend.catalog.domain.Processor;
import com.nitros64.nitros_games_backend.shared.domain.AbstractEntity;
import com.nitros64.nitros_games_backend.tooling.domain.LanguageTool;

@Entity
@Table(name = "game_version")
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class GameVersion extends AbstractEntity {
    private static final long serialVersionUID = 1L;

    @Column(nullable = false)
    private String name;
    
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_gamedata")
    private GameData game;
    
    @JoinColumns({
        @JoinColumn(name = "fk_idlang", referencedColumnName = "program_lang_id"),
        @JoinColumn(name = "fk_idtool", referencedColumnName = "program_tool_id")})
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private LanguageTool languageTool;
    
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_idplatform", nullable = false)
    private Platform platform;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_idprocessor", nullable = false)
    private Processor processor;

    @OneToMany(mappedBy = "gameVersion",
               fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Set<DownloadLink> downloadLinks = new HashSet<>();
    
    public void attachToGame(GameData game) {
        this.game = game;
    }

    public void updateCompatibility(
        String name,
        LanguageTool languageTool,
        Platform platform,
        Processor processor) {

        this.name = name;
        this.languageTool = languageTool;
        this.platform = platform;
        this.processor = processor;
    }
}
