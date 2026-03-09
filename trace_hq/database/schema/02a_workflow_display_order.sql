-- ---------------------------------------------------------------------------
-- Migration: add display_order to workflows for user-defined ordering on board
-- Run only on DBs created before display_order was added. Requires MySQL 8.0+.
-- ---------------------------------------------------------------------------

ALTER TABLE `workflows`
  ADD COLUMN `display_order` INT UNSIGNED NULL COMMENT 'User-defined order within project (1-based)' AFTER `description`;

-- Backfill: set order by current id per project (1-based within each project).
-- Uses a derived table so the same table is not read and updated in one statement.
UPDATE workflows w
JOIN (
  SELECT id, ROW_NUMBER() OVER (PARTITION BY project_id ORDER BY id) AS rn
  FROM workflows
) t ON w.id = t.id
SET w.display_order = t.rn;

ALTER TABLE `workflows`
  MODIFY COLUMN `display_order` INT UNSIGNED NOT NULL DEFAULT 1;

CREATE INDEX `ix_workflows_display_order` ON `workflows` (`project_id`, `display_order`);
