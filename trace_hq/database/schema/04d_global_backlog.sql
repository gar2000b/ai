-- ---------------------------------------------------------------------------
-- Migration: Global backlog — stories in backlog are not tied to a project.
-- project_id becomes nullable; when workflow_id IS NULL, project_id is NULL.
-- Run from trace_hq: ./scripts/mysql.sh < database/schema/04d_global_backlog.sql
-- ---------------------------------------------------------------------------

-- Allow project_id to be NULL (backlog stories have no project until added to a workflow)
ALTER TABLE `stories`
  MODIFY COLUMN `project_id` INT UNSIGNED NULL
  COMMENT 'Set when story is in a workflow (workflow.project_id); NULL when in global backlog';

-- Move existing backlog stories to global backlog (clear project_id)
UPDATE `stories` SET `project_id` = NULL WHERE `workflow_id` IS NULL;

-- Index for backlog list: order by backlog_order when workflow_id IS NULL
ALTER TABLE `stories` DROP INDEX `ix_stories_backlog_order`;
ALTER TABLE `stories` ADD INDEX `ix_stories_backlog_order` (`workflow_id`, `backlog_order`);
