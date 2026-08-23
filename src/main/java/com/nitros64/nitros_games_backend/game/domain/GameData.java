package com.nitros64.nitros_games_backend.game.domain;

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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "gamedata")
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class GameData extends Base {
    
    private static final long serialVersionUID = 1L;

    @Column(nullable = false)
    private String name;
    
    @Column(name = "descripcion", nullable = false)
    private String description;

    @Column(nullable = false)
    private boolean jam;

    @Column(name = "dev_numbers", nullable = false)
    private int developerCount;

    @OneToMany(mappedBy = "game", fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Set<GameVersion> versions = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "mygames_genres",
            joinColumns = @JoinColumn(name = "mygame_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id"))
    private Set<GameGenre> genres = new HashSet<>();
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dev_difficulty_id", nullable = false)
    private DevelopmentDifficulty developmentDifficulty;

    public void addVersion(GameVersion version) {
        this.versions.add(version);
        version.setGame(this);
    }

    public void replaceGenres(Set<GameGenre> genres) {
        this.genres.clear();
        this.genres.addAll(genres);
    }
}
