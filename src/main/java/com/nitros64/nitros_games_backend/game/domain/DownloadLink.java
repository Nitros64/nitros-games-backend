package com.nitros64.nitros_games_backend.game.domain;

import com.nitros64.nitros_games_backend.shared.domain.Base;
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
import lombok.Setter;

@Entity
@Table(name = "downloadLink")
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class DownloadLink extends Base {

    private static final long serialVersionUID = 1L;

    @Column(nullable = false, unique = true)
    private String link;
    
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_gameversion")
    private GameVersion gameVersion;
    
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_host_image")
    private ServerHostImage serverImage;
}
