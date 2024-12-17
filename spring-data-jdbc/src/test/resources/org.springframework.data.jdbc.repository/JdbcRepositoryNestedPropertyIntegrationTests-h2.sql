CREATE TABLE dummy_entity
(
    id BIGINT GENERATED ALWAYS AS IDENTITY (START WITH 1) PRIMARY KEY,
    PRIMARY KEY (id)
);

CREATE TABLE related_entity
(
    id           BIGINT GENERATED ALWAYS AS IDENTITY (START WITH 1 ) PRIMARY KEY,
    content      VARCHAR(255),
    dummy_entity BIGINT
);
