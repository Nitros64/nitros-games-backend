CREATE TABLE game_media (
    id BIGINT NOT NULL AUTO_INCREMENT,
    game_id BIGINT NOT NULL,
    media_type VARCHAR(12) NOT NULL,
    sort_order INT NOT NULL,
    storage_key VARCHAR(80) NOT NULL,
    content_type VARCHAR(32) NOT NULL,
    byte_size BIGINT NOT NULL,
    width INT NOT NULL,
    height INT NOT NULL,
    alt_text VARCHAR(200) NOT NULL DEFAULT '',
    caption VARCHAR(500) NOT NULL DEFAULT '',
    PRIMARY KEY (id),
    CONSTRAINT uk_game_media_slot UNIQUE (game_id, media_type, sort_order),
    CONSTRAINT uk_game_media_key UNIQUE (storage_key),
    CONSTRAINT fk_game_media_game FOREIGN KEY (game_id) REFERENCES gamedata (id) ON DELETE CASCADE,
    CONSTRAINT ck_game_media_slot CHECK (
        (media_type = 'COVER' AND sort_order = 0)
        OR (media_type = 'SCREENSHOT' AND sort_order > 0)
    ),
    CONSTRAINT ck_game_media_image CHECK (byte_size > 0 AND width > 0 AND height > 0)
) ENGINE=InnoDB;
