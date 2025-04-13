DROP TABLE dummy_entity;
DROP TABLE ROOT;
DROP TABLE INTERMEDIATE;
DROP TABLE LEAF;
DROP TABLE WITH_DELIMITED_COLUMN;

-- Таблица для агрегата (корневой сущности)
CREATE TABLE dummy_entity
(
    id BIGSERIAL PRIMARY KEY
);

-- Таблица для дочерней сущности с внешним ключом на dummy_entity.id.
CREATE TABLE related_entity
(
    id              BIGSERIAL PRIMARY KEY,
    content         VARCHAR(255) NOT NULL,
    dummy_entity_id BIGINT       NOT NULL,
    CONSTRAINT fk_dummy_entity
        FOREIGN KEY (dummy_entity_id)
            REFERENCES dummy_entity (id)
            ON DELETE CASCADE
);
