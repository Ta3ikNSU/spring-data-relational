CREATE TABLE dummy_entity
(
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY
);

CREATE TABLE related_entity
(
    id           BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    content      VARCHAR(255),
    dummy_entity BIGINT
);
