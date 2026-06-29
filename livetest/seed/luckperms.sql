-- LuckPerms 5.x SQL schema (the two tables the luckperms.yml template targets), seeded with one player
-- keyed by the OLD offline UUID from docs/quickstart-example.md. After the migration these rows must be
-- keyed by the NEW premium UUID instead. Dashed-string UUID column, matching the template's default codec.

CREATE TABLE luckperms_players (
    uuid        VARCHAR(36) NOT NULL PRIMARY KEY,
    username    VARCHAR(16) NOT NULL,
    primary_group VARCHAR(36) NOT NULL
);

CREATE TABLE luckperms_user_permissions (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    uuid        VARCHAR(36) NOT NULL,
    permission  VARCHAR(200) NOT NULL,
    value       BOOLEAN NOT NULL,
    server      VARCHAR(36) NOT NULL DEFAULT 'global',
    world       VARCHAR(36) NOT NULL DEFAULT 'global',
    expiry      BIGINT NOT NULL DEFAULT 0,
    contexts    VARCHAR(200) NOT NULL DEFAULT '{}'
);

-- Steve, offline UUID 069a79f4-44e9-4726-a5be-fca90e38aaf5, with a rank and a permission.
INSERT INTO luckperms_players (uuid, username, primary_group)
VALUES ('069a79f4-44e9-4726-a5be-fca90e38aaf5', 'Steve', 'vip');

INSERT INTO luckperms_user_permissions (uuid, permission, value)
VALUES ('069a79f4-44e9-4726-a5be-fca90e38aaf5', 'group.vip', 1),
       ('069a79f4-44e9-4726-a5be-fca90e38aaf5', 'essentials.home', 1);
