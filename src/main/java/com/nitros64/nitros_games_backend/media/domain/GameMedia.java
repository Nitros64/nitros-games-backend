package com.nitros64.nitros_games_backend.media.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.nitros64.nitros_games_backend.game.domain.GameData;
import com.nitros64.nitros_games_backend.shared.domain.AbstractEntity;

@Entity
@Table(name = "game_media", uniqueConstraints = {
        @UniqueConstraint(name = "uk_game_media_slot", columnNames = {"game_id", "media_type", "sort_order"}),
        @UniqueConstraint(name = "uk_game_media_key", columnNames = "storage_key")
})
@Getter
@NoArgsConstructor
public class GameMedia extends AbstractEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private GameData game;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "media_type", nullable = false, length = 12)
    private GameMediaType type;

    @Column(name = "sort_order", nullable = false)
    private int position;

    @Column(name = "storage_key", nullable = false, length = 80)
    private String storageKey;

    @Column(name = "content_type", nullable = false, length = 32)
    private String contentType;

    @Column(name = "byte_size", nullable = false)
    private long byteSize;

    @Column(nullable = false)
    private int width;

    @Column(nullable = false)
    private int height;

    @Column(name = "alt_text", nullable = false, length = 200)
    private String altText = "";

    @Column(nullable = false, length = 500)
    private String caption = "";

    public GameMedia(GameData game, GameMediaType type, int position) {
        this.game = game;
        this.type = type;
        this.position = position;
    }

    public void replaceImage(String storageKey, String contentType, long byteSize, int width, int height) {
        this.storageKey = storageKey;
        this.contentType = contentType;
        this.byteSize = byteSize;
        this.width = width;
        this.height = height;
    }

    public void updateMetadata(String altText, String caption) {
        this.altText = altText;
        this.caption = caption;
    }

    public void moveTo(int position) {
        this.position = position;
    }
}
