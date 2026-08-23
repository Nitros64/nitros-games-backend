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

import com.nitros64.nitros_games_backend.shared.domain.AbstractEntity;
import com.nitros64.nitros_games_backend.tooling.domain.LanguageTool;
import com.nitros64.nitros_games_backend.tooling.domain.ToolPlatform;
import com.nitros64.nitros_games_backend.tooling.domain.ToolProcessor;

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
    
    @JoinColumns({
        @JoinColumn(name = "fk_idtool", referencedColumnName = "fk_idtool", insertable = false, updatable = false),
        @JoinColumn(name = "fk_idprocessor", referencedColumnName = "fk_idprocessor", insertable = false, updatable = false)})
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private ToolProcessor toolProcessor;

    @Column(name = "fk_idprocessor", nullable = false)
    private Long processorId;
    
    @JoinColumns({
        @JoinColumn(name = "fk_idtool", referencedColumnName = "fk_idtool", insertable = false, updatable = false),
        @JoinColumn(name = "fk_idplatform", referencedColumnName = "fk_idplatform", insertable = false, updatable = false)})
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private ToolPlatform toolPlatform;

    @Column(name = "fk_idplatform", nullable = false)
    private Long platformId;

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
            ToolPlatform toolPlatform,
            ToolProcessor toolProcessor,
            Long platformId,
            Long processorId) {
        this.name = name;
        this.languageTool = languageTool;
        this.toolPlatform = toolPlatform;
        this.toolProcessor = toolProcessor;
        this.platformId = platformId;
        this.processorId = processorId;
    }
}
