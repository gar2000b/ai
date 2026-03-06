-- ---------------------------------------------------------------------------
-- Story stage history and audit log (append-only)
-- Ref: USER-STORY.md — stageHistory; notes (claims, handoffs, review outcomes, etc.)
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS `story_stage_history` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `story_id`        VARCHAR(16) NOT NULL,
  `from_stage_name` VARCHAR(64) NULL COMMENT 'canonical stage name at transition time',
  `to_stage_name`   VARCHAR(64) NOT NULL,
  `from_workflow_stage_id` INT UNSIGNED NULL,
  `to_workflow_stage_id`  INT UNSIGNED NULL,
  `assignee_id`     INT UNSIGNED NULL,
  `changed_by_agent_id`  INT UNSIGNED NULL,
  `created_at`      DATETIME(3) NOT NULL DEFAULT (CURRENT_TIMESTAMP(3)),
  PRIMARY KEY (`id`),
  KEY `ix_story_stage_history_story_id` (`story_id`),
  KEY `ix_story_stage_history_created_at` (`created_at`),
  CONSTRAINT `fk_story_stage_history_story` FOREIGN KEY (`story_id`) REFERENCES `stories` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_story_stage_history_to_stage` FOREIGN KEY (`to_workflow_stage_id`) REFERENCES `workflow_stages` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_story_stage_history_from_stage` FOREIGN KEY (`from_workflow_stage_id`) REFERENCES `workflow_stages` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_story_stage_history_assignee` FOREIGN KEY (`assignee_id`) REFERENCES `agents` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_story_stage_history_changed_by` FOREIGN KEY (`changed_by_agent_id`) REFERENCES `agents` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `story_audit_log` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `story_id`   VARCHAR(16) NOT NULL,
  `event_type` VARCHAR(64) NULL COMMENT 'claim, handoff, review_approved, rejection, owner_override, workflow_transfer, etc.',
  `note`       TEXT NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT (CURRENT_TIMESTAMP(3)),
  PRIMARY KEY (`id`),
  KEY `ix_story_audit_log_story_id` (`story_id`),
  KEY `ix_story_audit_log_created_at` (`created_at`),
  CONSTRAINT `fk_story_audit_log_story` FOREIGN KEY (`story_id`) REFERENCES `stories` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
