-- ---------------------------------------------------------------------------
-- Migration: Add backlog_order to stories (for user-defined backlog ordering)
-- Run on existing DBs. New installs: 04_stories.sql includes backlog_order; skip or run safely.
-- Run from trace_hq: ./scripts/mysql.sh < database/schema/04c_backlog_order.sql
-- ---------------------------------------------------------------------------

-- Add column (nullable; only used when workflow_id IS NULL)
ALTER TABLE `stories`
  ADD COLUMN `backlog_order` INT UNSIGNED NULL
  COMMENT 'Display order in project backlog; lower = earlier. Only used when workflow_id IS NULL.'
  AFTER `workflow_stage_id`;

-- Backfill existing backlog stories: assign same value so they sort by id until user reorders
UPDATE `stories` SET `backlog_order` = 0 WHERE `workflow_id` IS NULL;
ALTER TABLE `stories`
  ADD INDEX `ix_stories_backlog_order` (`project_id`, `backlog_order`);
