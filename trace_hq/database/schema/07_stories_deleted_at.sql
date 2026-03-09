-- ---------------------------------------------------------------------------
-- Migration: logical delete for stories (soft delete; row kept, hidden from UI)
-- Run after 04_stories.sql (and 04d if using global backlog).
-- ---------------------------------------------------------------------------

ALTER TABLE `stories`
  ADD COLUMN `deleted_at` DATETIME(3) NULL DEFAULT NULL
  COMMENT 'When set, story is logically deleted and excluded from lists and API'
  AFTER `last_updated_at`;
