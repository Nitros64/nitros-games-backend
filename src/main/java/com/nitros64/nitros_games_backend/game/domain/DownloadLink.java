package com.nitros64.nitros_games_backend.game.domain;

import com.nitros64.nitros_games_backend.shared.domain.Base;
import com.nitros64.nitros_games_backend.storage.domain.ServerHostImage;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "downloadLink")
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class DownloadLink extends Base{
    
    private static final long serialVersionUID = 1L; 
    
    //@NotEmpty(message = "no puede estar vacio")   
    @Size(min = 4, max = 100, message="el tamaño tiene que estar entre 4 y 100")
    @Column(nullable = false, unique = true)
    private String Link;
    
    @ManyToOne(optional = false, 
               //cascade=CascadeType.ALL,
               fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_gameversion")
    private GameVersion gameversion;
    
    @ManyToOne(optional = false, 
               //cascade=CascadeType.ALL,
               fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_host_image")
    private ServerHostImage serverImage;

//    public DownloadLink(String Link, GameVersion gameversion, ServerHostImage serverImage) {
//        this.Link = Link;
//        this.gameversion = gameversion;
//        this.serverImage = serverImage;
//    }
    
    public DownloadLink(String Link, ServerHostImage serverImage) {
        this.Link = Link;
        this.serverImage = serverImage;
    }
    
    
}
