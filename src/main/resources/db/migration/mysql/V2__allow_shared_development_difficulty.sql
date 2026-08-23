ALTER TABLE gamedata
    DROP FOREIGN KEY fk_gamedata_dev_difficulty;

ALTER TABLE gamedata
    DROP INDEX uk_gamedata_dev_difficulty,
    ADD INDEX idx_gamedata_dev_difficulty (dev_difficulty_id);

ALTER TABLE gamedata
    ADD CONSTRAINT fk_gamedata_dev_difficulty
        FOREIGN KEY (dev_difficulty_id) REFERENCES dev_difficulty (id);
