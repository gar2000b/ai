-- ---------------------------------------------------------------------------
-- Migration: logical delete for projects (soft delete; row kept, hidden from UI)
-- Run after 01_projects.sql.
-- ---------------------------------------------------------------------------

ALTER TABLE `projects`
  ADD COLUMN `deleted_at` DATETIME(3) NULL DEFAULT NULL
  COMMENT 'When set, project is logically deleted and excluded from lists and API'
  AFTER `updated_at`;
