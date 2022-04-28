CREATE TABLE story
(
    id            int(11) NOT NULL AUTO_INCREMENT,
    playerUuid    varchar(48) COLLATE utf8mb4_unicode_ci NOT NULL,
    characterType varchar(48)                            NOT NULL,
    characterName varchar(48)                            NOT NULL,
    startTime     TIMESTAMP                              NOT NULL,
    endTime       TIMESTAMP,
    PRIMARY KEY (`id`),
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/* todo add a constrain to make sure there is max 1 active story per user */


