package com.nitros64.nitros_games_backend.game.domain;

import com.nitros64.nitros_games_backend.shared.domain.AbstractEntity;
import com.nitros64.nitros_games_backend.storage.domain.ServerHostImage;

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
@Table(name = "download_link")
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class DownloadLink extends AbstractEntity {

    private static final long serialVersionUID = 1L;

    @Column(nullable = false, unique = true)
    private String link;
    
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_gameversion")
    private GameVersion gameVersion;
    
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_host_image")
    private ServerHostImage serverImage;

    public void attachToVersion(GameVersion gameVersion) {
        this.gameVersion = gameVersion;
    }

    public void updateDetails(String link, ServerHostImage serverImage) {
        this.link = link;
        this.serverImage = serverImage;
    }
}
