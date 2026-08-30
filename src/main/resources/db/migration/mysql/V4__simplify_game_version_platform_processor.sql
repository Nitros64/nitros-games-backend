ALTER TABLE game_version
    DROP FOREIGN KEY fk_game_version_platform_tool;

ALTER TABLE game_version
    DROP FOREIGN KEY fk_game_version_processor_tool;

ALTER TABLE game_version
    DROP INDEX fk_game_version_platform_tool;

ALTER TABLE game_version
    DROP INDEX fk_game_version_processor_tool;

ALTER TABLE game_version
    ADD INDEX idx_game_version_platform (fk_idplatform),
    ADD INDEX idx_game_version_processor (fk_idprocessor);

ALTER TABLE game_version
    ADD CONSTRAINT fk_game_version_platform
        FOREIGN KEY (fk_idplatform)
        REFERENCES platform(id),
    ADD CONSTRAINT fk_game_version_processor
        FOREIGN KEY (fk_idprocessor)
        REFERENCES processor(id);

DROP TABLE tool_platform;
DROP TABLE tool_processor;