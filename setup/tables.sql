CREATE TABLE story
(
    id            int(11) NOT NULL AUTO_INCREMENT,
    playerUuid    binary(16) NOT NULL,
    characterType varchar(48) NOT NULL,
    characterName varchar(20) NOT NULL,
    startTime     TIMESTAMP   NOT NULL DEFAULT 0, /* to prevent https://dev.mysql.com/doc/refman/8.0/en/timestamp-initialization.html */
    endTime       TIMESTAMP NULL DEFAULT NULL,
    survivalTime  INT UNSIGNED NOT NULL DEFAULT 0,
    deathCause    varchar(48) NULL DEFAULT NULL,
    deathLocWorld varchar(48) NULL DEFAULT NULL,
    deathLocX     DOUBLE NULL DEFAULT NULL,
    deathLocY     DOUBLE NULL DEFAULT NULL,
    deathLocZ     DOUBLE NULL DEFAULT NULL,
    PRIMARY KEY (`id`)
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/* todo add a constrain to make sure there is max 1 active story per user */


