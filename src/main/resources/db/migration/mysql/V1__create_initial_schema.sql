CREATE TABLE dev_difficulty (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(30) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_dev_difficulty_name UNIQUE (name)
) ENGINE=InnoDB;

CREATE TABLE game_genres (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(30) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_game_genres_name UNIQUE (name)
) ENGINE=InnoDB;

CREATE TABLE platform (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(30) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_platform_name UNIQUE (name)
) ENGINE=InnoDB;

CREATE TABLE processor (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(10) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_processor_name UNIQUE (name)
) ENGINE=InnoDB;

CREATE TABLE program_lang (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(12) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_program_lang_name UNIQUE (name)
) ENGINE=InnoDB;

CREATE TABLE programtool_type (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(30) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_programtool_type_name UNIQUE (name)
) ENGINE=InnoDB;

CREATE TABLE server_hostimage (
    id BIGINT NOT NULL AUTO_INCREMENT,
    imagepath VARCHAR(255) NOT NULL,
    name VARCHAR(30) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_server_hostimage_imagepath UNIQUE (imagepath),
    CONSTRAINT uk_server_hostimage_name UNIQUE (name)
) ENGINE=InnoDB;

CREATE TABLE gamedata (
    id BIGINT NOT NULL AUTO_INCREMENT,
    descripcion VARCHAR(30) NOT NULL,
    dev_numbers INT NOT NULL,
    jam BIT NOT NULL,
    name VARCHAR(30) NOT NULL,
    dev_difficulty_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_gamedata_dev_difficulty UNIQUE (dev_difficulty_id),
    CONSTRAINT fk_gamedata_dev_difficulty FOREIGN KEY (dev_difficulty_id)
        REFERENCES dev_difficulty (id)
) ENGINE=InnoDB;

CREATE TABLE program_tool (
    id BIGINT NOT NULL AUTO_INCREMENT,
    imagefile_path VARCHAR(30) NOT NULL,
    name VARCHAR(30) NOT NULL,
    web_page VARCHAR(255) NOT NULL,
    fk_gametooltype BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_program_tool_imagefile_path UNIQUE (imagefile_path),
    CONSTRAINT uk_program_tool_name UNIQUE (name),
    CONSTRAINT uk_program_tool_web_page UNIQUE (web_page),
    CONSTRAINT fk_program_tool_type FOREIGN KEY (fk_gametooltype)
        REFERENCES programtool_type (id)
) ENGINE=InnoDB;

CREATE TABLE mygames_genres (
    mygame_id BIGINT NOT NULL,
    genre_id BIGINT NOT NULL,
    PRIMARY KEY (mygame_id, genre_id),
    CONSTRAINT fk_mygames_genres_game FOREIGN KEY (mygame_id)
        REFERENCES gamedata (id) ON DELETE CASCADE,
    CONSTRAINT fk_mygames_genres_genre FOREIGN KEY (genre_id)
        REFERENCES game_genres (id)
) ENGINE=InnoDB;

CREATE TABLE tool_lang (
    program_lang_id BIGINT NOT NULL,
    program_tool_id BIGINT NOT NULL,
    PRIMARY KEY (program_lang_id, program_tool_id),
    CONSTRAINT fk_tool_lang_language FOREIGN KEY (program_lang_id)
        REFERENCES program_lang (id) ON DELETE CASCADE,
    CONSTRAINT fk_tool_lang_tool FOREIGN KEY (program_tool_id)
        REFERENCES program_tool (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE tool_platform (
    fk_idplatform BIGINT NOT NULL,
    fk_idtool BIGINT NOT NULL,
    PRIMARY KEY (fk_idplatform, fk_idtool),
    CONSTRAINT fk_tool_platform_platform FOREIGN KEY (fk_idplatform)
        REFERENCES platform (id),
    CONSTRAINT fk_tool_platform_tool FOREIGN KEY (fk_idtool)
        REFERENCES program_tool (id)
) ENGINE=InnoDB;

CREATE TABLE tool_processor (
    fk_idprocessor BIGINT NOT NULL,
    fk_idtool BIGINT NOT NULL,
    PRIMARY KEY (fk_idprocessor, fk_idtool),
    CONSTRAINT fk_tool_processor_processor FOREIGN KEY (fk_idprocessor)
        REFERENCES processor (id),
    CONSTRAINT fk_tool_processor_tool FOREIGN KEY (fk_idtool)
        REFERENCES program_tool (id)
) ENGINE=InnoDB;

CREATE TABLE game_version (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(30) NOT NULL,
    fk_gamedata BIGINT NOT NULL,
    fk_idlang BIGINT NOT NULL,
    fk_idtool BIGINT NOT NULL,
    fk_idplatform BIGINT NOT NULL,
    fk_idprocessor BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_game_version_game FOREIGN KEY (fk_gamedata)
        REFERENCES gamedata (id) ON DELETE CASCADE,
    CONSTRAINT fk_game_version_language_tool FOREIGN KEY (fk_idlang, fk_idtool)
        REFERENCES tool_lang (program_lang_id, program_tool_id),
    CONSTRAINT fk_game_version_platform_tool FOREIGN KEY (fk_idplatform, fk_idtool)
        REFERENCES tool_platform (fk_idplatform, fk_idtool),
    CONSTRAINT fk_game_version_processor_tool FOREIGN KEY (fk_idprocessor, fk_idtool)
        REFERENCES tool_processor (fk_idprocessor, fk_idtool)
) ENGINE=InnoDB;

CREATE TABLE download_link (
    id BIGINT NOT NULL AUTO_INCREMENT,
    link VARCHAR(100) NOT NULL,
    fk_gameversion BIGINT NOT NULL,
    fk_host_image BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_download_link_link UNIQUE (link),
    CONSTRAINT fk_download_link_game_version FOREIGN KEY (fk_gameversion)
        REFERENCES game_version (id) ON DELETE CASCADE,
    CONSTRAINT fk_download_link_host_image FOREIGN KEY (fk_host_image)
        REFERENCES server_hostimage (id)
) ENGINE=InnoDB;
