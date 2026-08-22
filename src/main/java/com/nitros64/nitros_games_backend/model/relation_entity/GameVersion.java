package com.nitros64.nitros_games_backend.model.relation_entity;

import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.nitros64.nitros_games_backend.shared.domain.Base;
import com.nitros64.nitros_games_backend.model.entity.DownloadLink;
import com.nitros64.nitros_games_backend.model.entity.GameData;
import com.nitros64.nitros_games_backend.tooling.domain.LanguageTool;
import com.nitros64.nitros_games_backend.tooling.domain.ToolPlatform;
import com.nitros64.nitros_games_backend.tooling.domain.ToolProcessor;

@Entity
@Table(name = "GameVersion")
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class GameVersion extends Base {
    private static final long serialVersionUID = 1L; 
    
    @NotEmpty(message = "no puede estar vacio")
    @Size(min = 4, max = 30, message="el tamaño tiene que estar entre 4 y 30")
    @Column(nullable = false, unique = false)
    private String name;
    
    @ManyToOne (optional = false, 
                //cascade=CascadeType.ALL,
                fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_gamedata")
    private GameData gamedata;
    
    @JoinColumns({
        @JoinColumn(name = "fk_idlang", referencedColumnName = "program_lang_id"),
        @JoinColumn(name = "fk_idtool", referencedColumnName = "program_tool_id")})
    @ManyToOne(optional = false, 
            //cascade = CascadeType.ALL,
            fetch=FetchType.LAZY)
    private LanguageTool lang_tool;
    
    @JoinColumns({
        @JoinColumn(name = "fk_idtool", referencedColumnName = "fk_idtool", insertable = false, updatable = false),
        @JoinColumn(name = "fk_idprocessor", referencedColumnName = "fk_idprocessor", insertable = false, updatable = false )})
    @ManyToOne(optional = false, 
            //cascade = CascadeType.ALL,
            fetch=FetchType.LAZY)
    private ToolProcessor toolprocessor;
    
    @JoinColumns({
        @JoinColumn(name = "fk_idtool", referencedColumnName = "fk_idtool", insertable = false, updatable = false),
        @JoinColumn(name = "fk_idplatform", referencedColumnName = "fk_idplatform", insertable = false, updatable = false )})
    @ManyToOne(optional = false, 
            //cascade = CascadeType.ALL,
            fetch=FetchType.LAZY)
    private ToolPlatform toolplatform;


    //LISTA COMPLETA DE LOS DIFERENTES ENLACES DONDE SE ENCUENTRA EL JUEGO HOSTEADO
    @OneToMany(mappedBy = "gameversion", 
               cascade = CascadeType.ALL,
               fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Set<DownloadLink> downloadLinks = new HashSet<>();
    
    public GameVersion(String name, LanguageTool lang_tool, ToolProcessor toolprocessor, ToolPlatform toolplatform) {
        this.name = name;
        this.lang_tool = lang_tool;
        this.toolprocessor = toolprocessor;
        this.toolplatform = toolplatform;
    }   
    
    public void addDownloadLink(DownloadLink dl){
        this.downloadLinks.add(dl);
        dl.setGameversion(this);
    }
    
    public void removeDownloadLink(DownloadLink dl){
        this.downloadLinks.remove(dl);
        dl.setGameversion(null);
    } 
    
}
