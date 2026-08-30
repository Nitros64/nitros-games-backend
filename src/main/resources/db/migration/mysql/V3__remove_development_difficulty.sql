ALTER TABLE gamedata
    DROP FOREIGN KEY fk_gamedata_dev_difficulty;

ALTER TABLE gamedata
    DROP INDEX idx_gamedata_dev_difficulty;

ALTER TABLE gamedata
    DROP COLUMN dev_difficulty_id;

DROP TABLE dev_difficulty;