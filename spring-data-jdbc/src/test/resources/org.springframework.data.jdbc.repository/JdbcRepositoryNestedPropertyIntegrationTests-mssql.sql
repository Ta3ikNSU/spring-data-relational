CREATE TABLE dummy_entity
(
    id BIGINT IDENTITY PRIMARY KEY
);

CREATE TABLE related_entity
(
    id           BIGINT IDENTITY PRIMARY KEY,
    content      VARCHAR(255),
    dummy_entity BIGINT
);
