-- ---------------------------------------------------------------------------
-- Roles and agents (system role model, assignees)
-- Ref: OPEN-WORKFLOWS-PROJECT.md — agent types; USER-STORIES.md — assignee (dev-lisa, owner, etc.)
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS `roles` (
  `id`   INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `code` VARCHAR(32) NOT NULL COMMENT 'owner | dev | unit-test | integration-test | performance-test | devops',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_roles_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `agents` (
  `id`       INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name`     VARCHAR(128) NOT NULL COMMENT 'e.g. dev-lisa, owner, performance-test-derek',
  `role_id`  INT UNSIGNED NOT NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT (CURRENT_TIMESTAMP(3)),
  `updated_at` DATETIME(3) NOT NULL DEFAULT (CURRENT_TIMESTAMP(3)) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_agents_name` (`name`),
  KEY `ix_agents_role_id` (`role_id`),
  CONSTRAINT `fk_agents_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
