-- ---------------------------------------------------------------------------
-- Projects (Open Workflows Project container)
-- Ref: OPEN-WORKFLOWS-PROJECT.md — "Contains 1..n independent workflows"
-- ---------------------------------------------------------------------------
-- Use database from env (DB_NAME); default trace-hq.
-- Run from project root; credentials via scripts/mysql.sh or .env.
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS `projects` (
  `id`          INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name`        VARCHAR(255) NOT NULL,
  `created_at`  DATETIME(3) NOT NULL DEFAULT (CURRENT_TIMESTAMP(3)),
  `updated_at`  DATETIME(3) NOT NULL DEFAULT (CURRENT_TIMESTAMP(3)) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
