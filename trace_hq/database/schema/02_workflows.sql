-- ---------------------------------------------------------------------------
-- Workflows and workflow stages
-- Ref: OPEN-WORKFLOWS-PROJECT.md — workflows as delivery pipelines, stage patterns
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS `workflows` (
  `id`          INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `project_id`  INT UNSIGNED NOT NULL,
  `code`        VARCHAR(32) NOT NULL COMMENT 'development | performance | devops | manual',
  `name`        VARCHAR(255) NOT NULL,
  `description` TEXT,
  `created_at`  DATETIME(3) NOT NULL DEFAULT (CURRENT_TIMESTAMP(3)),
  `updated_at`  DATETIME(3) NOT NULL DEFAULT (CURRENT_TIMESTAMP(3)) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_workflows_project_code` (`project_id`, `code`),
  KEY `ix_workflows_project_id` (`project_id`),
  CONSTRAINT `fk_workflows_project` FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `workflow_stages` (
  `id`           INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `workflow_id`  INT UNSIGNED NOT NULL,
  `stage_order`  SMALLINT UNSIGNED NOT NULL COMMENT '1-based position in pipeline',
  `stage_name`   VARCHAR(64) NOT NULL COMMENT 'Todo | Planning | In Progress | Review | Done',
  `stage_role`   VARCHAR(32) NOT NULL COMMENT 'owner | dev | unit-test | integration-test | performance-test | devops',
  `created_at`   DATETIME(3) NOT NULL DEFAULT (CURRENT_TIMESTAMP(3)),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_workflow_stages_order` (`workflow_id`, `stage_order`),
  KEY `ix_workflow_stages_workflow_id` (`workflow_id`),
  CONSTRAINT `fk_workflow_stages_workflow` FOREIGN KEY (`workflow_id`) REFERENCES `workflows` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
