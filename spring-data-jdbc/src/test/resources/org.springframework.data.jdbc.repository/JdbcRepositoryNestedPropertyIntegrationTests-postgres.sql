DROP TABLE dummy_entity;
DROP TABLE ROOT;
DROP TABLE INTERMEDIATE;
DROP TABLE LEAF;
DROP TABLE WITH_DELIMITED_COLUMN;

DROP TABLE IF EXISTS dummy_entity;
DROP TABLE IF EXISTS related_entity;
DROP TABLE IF EXISTS intermediate_entity;

CREATE TABLE dummy_entity
(
    id BIGSERIAL PRIMARY KEY
);

CREATE TABLE intermediate_entity
(
    id                   BIGSERIAL PRIMARY KEY,
    content              VARCHAR(255) NOT NULL,
    dummy_entity_id      BIGINT       NOT NULL,
    CONSTRAINT fk_dummy_entity
        FOREIGN KEY (dummy_entity_id)
            REFERENCES dummy_entity (id)
            ON DELETE CASCADE
);

CREATE TABLE related_entity
(
    id              BIGSERIAL PRIMARY KEY,
    content         VARCHAR(255) NOT NULL,
    intermediate_entity_id BIGINT       NOT NULL,
    CONSTRAINT fk_dummy_entity
        FOREIGN KEY (intermediate_entity_id)
            REFERENCES intermediate_entity (id)
            ON DELETE CASCADE
);
