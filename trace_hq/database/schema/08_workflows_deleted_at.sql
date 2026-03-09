-- ---------------------------------------------------------------------------
-- Migration: logical delete for workflows (soft delete; row kept, hidden from UI)
-- Run after 02_workflows.sql (and 02a if using display_order).
-- ---------------------------------------------------------------------------

ALTER TABLE `workflows`
  ADD COLUMN `deleted_at` DATETIME(3) NULL DEFAULT NULL
  COMMENT 'When set, workflow is logically deleted and excluded from lists and API'
  AFTER `updated_at`;
