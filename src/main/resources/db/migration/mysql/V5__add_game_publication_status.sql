ALTER TABLE gamedata
    ADD COLUMN publication_status VARCHAR(16) NOT NULL DEFAULT 'DRAFT';

ALTER TABLE gamedata
    ADD CONSTRAINT chk_gamedata_publication_status
        CHECK (publication_status IN ('DRAFT', 'PUBLISHED'));

CREATE INDEX idx_gamedata_publication_id ON gamedata (publication_status, id);
