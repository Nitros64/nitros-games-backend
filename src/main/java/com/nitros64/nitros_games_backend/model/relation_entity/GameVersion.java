package com.nitros64.nitros_games_backend.model.relation_entity;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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

import com.nitros64.nitros_games_backend.model.entity.Base;
import com.nitros64.nitros_games_backend.model.entity.DownloadLink;
import com.nitros64.nitros_games_backend.model.entity.GameData;

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
    private Set<DownloadLink> downloadLinks = new HashSet();

//    public GameVersion() { }
//    
//    public GameVersion(String name) {
//        this.name = name;
//    }
//    
//    public GameVersion(String name, GameData versions) {
//        this.name = name;
//        //this.gamedata = gamedata;
//    }
//
//    public GameVersion(String name, LanguageTool ptool) {
//        this.name = name;
//        this.lang_tool = ptool;
//    }
//
//    public GameVersion(String name, LanguageTool ptool, ToolProcessor toolprocessor) {
//        this.name = name;
//        this.lang_tool = ptool;
//        this.toolprocessor = toolprocessor;
//    }
//
    public GameVersion(String name, LanguageTool lang_tool, ToolProcessor toolprocessor, ToolPlatform toolplatform) {
        this.name = name;
        this.lang_tool = lang_tool;
        this.toolprocessor = toolprocessor;
        this.toolplatform = toolplatform;
    }
//
//    public GameVersion(String name, GameData versions, LanguageTool ptool) {
//        this.name = name;
//        //this.gamedata = gamedata;
//        this.lang_tool = ptool;
//    }   
    
    public void addDownloadLink(DownloadLink dl){
        this.downloadLinks.add(dl);
        dl.setGameversion(this);
    }
    
    public void removeDownloadLink(DownloadLink dl){
        this.downloadLinks.remove(dl);
        dl.setGameversion(null);
    } 
    
}