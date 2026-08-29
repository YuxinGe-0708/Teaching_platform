-- Migration bookkeeping. This file is intentionally safe to run repeatedly.
-- The initial business schema remains in db/init/01_schema.sql.
CREATE TABLE IF NOT EXISTS `schema_migrations` (
    `version` VARCHAR(100) NOT NULL PRIMARY KEY,
    `applied_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
