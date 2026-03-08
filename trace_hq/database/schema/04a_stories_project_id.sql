-- ---------------------------------------------------------------------------
-- Migration: Add project_id to stories (for backlog support)
-- Run this on existing DBs that have stories without project_id.
-- New installs: 04_stories.sql already includes project_id; skip this or run safely (no-op if column exists).
-- Run from trace_hq: ./scripts/mysql.sh < database/schema/04a_stories_project_id.sql
-- ---------------------------------------------------------------------------

-- Add column (nullable first)
ALTER TABLE `stories`
  ADD COLUMN `project_id` INT UNSIGNED NULL COMMENT 'Project this story belongs to; determines backlog when workflow_id NULL' AFTER `priority`;

-- Backfill from workflow's project
UPDATE `stories` s
  JOIN `workflows` w ON s.workflow_id = w.id
SET s.project_id = w.project_id
WHERE s.workflow_id IS NOT NULL;

-- Default any remaining NULLs to project 1
UPDATE `stories` SET project_id = 1 WHERE project_id IS NULL;

-- Make NOT NULL and add FK + index
ALTER TABLE `stories`
  MODIFY `project_id` INT UNSIGNED NOT NULL,
  ADD INDEX `ix_stories_project_id` (`project_id`),
  ADD CONSTRAINT `fk_stories_project` FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE;
