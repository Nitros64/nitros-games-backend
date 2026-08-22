package com.nitros64.nitros_games_backend.model.entity;

import com.nitros64.nitros_games_backend.catalog.domain.DevelopmentDifficulty;
import com.nitros64.nitros_games_backend.catalog.domain.GameGenre;
import com.nitros64.nitros_games_backend.shared.domain.Base;

import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.nitros64.nitros_games_backend.model.relation_entity.GameVersion;

@Entity
@Table(name = "gamedata")
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class GameData extends Base {
    
    private static final long serialVersionUID = 1L; 
    
    @NotEmpty(message = "no puede estar vacio")
    @Size(min = 4, max = 30, message="el tamaño tiene que estar entre 4 y 30")
    @Column(nullable = false, unique = false)
    private String name;
    
    @NotEmpty(message = "no puede estar vacio")
    @Size(min = 4, max = 30, message="el tamaño tiene que estar entre 4 y 30")
    @Column(nullable = false)
    private String descripcion;
    
    @Column(nullable = false)
    private boolean jam;
    
    @Column(nullable = false)
    private int devNumbers;//cantidad de desarrolladores en el proyecto

    //LISTA COMPLETA SOBRE LAS DIFERENTES VERSIONES DEL VIDEOJUEGOS
    @OneToMany(mappedBy = "gamedata", fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Set<GameVersion> versionList = new HashSet<>();


    //LISTA COMPLETA SOBRE LOS DIFERENTES GENEROS A LOS QUE PERTENECE DEL VIDEOJUEGOS
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "mygames_genres", 
        joinColumns = @JoinColumn(name = "mygame_id", insertable = false, updatable = false), 
        inverseJoinColumns = @JoinColumn(name = "genre_id", insertable = false, updatable = false))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Set<GameGenre> mygameGenres = new HashSet<>();
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dev_difficulty_id", nullable = false)
    private DevelopmentDifficulty devDificulty;

//    public GameData() {
//    }
//
    public GameData(String nombre, String descripcion, boolean jam, int devNumber) {
        this.name = nombre;
        this.descripcion = descripcion;
        this.jam = jam;
        this.devNumbers = devNumber;
    }   

    public GameData(String nombre, String descripcion, boolean jam, int devNumber, DevelopmentDifficulty devDificulty) {
        this.name = nombre;
        this.descripcion = descripcion;
        this.jam = jam;
        this.devNumbers = devNumber;
        this.devDificulty = devDificulty;
    }    
    
    public void addVersion(GameVersion version) {
        this.versionList.add(version);
        version.setGamedata(this);
    }
    
    public void addGenre(GameGenre gamegenre){
        mygameGenres.add(gamegenre);
    }
}
