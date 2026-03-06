-- ---------------------------------------------------------------------------
-- Data: Insert roles (agent types) and agent roster (personas)
-- Ref: OPEN-WORKFLOWS-PROJECT.md — AGENT TYPES + AGENT ROSTER (two per role except owner)
-- Run from trace_hq:  ./scripts/mysql.sh < database/data/03_roles_and_agents.sql
-- ---------------------------------------------------------------------------

-- Agent types (roles)
INSERT INTO `roles` (`code`)
VALUES
  ('owner'),
  ('dev'),
  ('unit-test'),
  ('integration-test'),
  ('performance-test'),
  ('devops');

-- Agent roster: owner (1), two per role for dev, unit-test, integration-test, performance-test, devops
INSERT INTO `agents` (`name`, `role_id`)
SELECT 'owner',       `id` FROM `roles` WHERE `code` = 'owner'
UNION ALL
SELECT 'dev-bob',     `id` FROM `roles` WHERE `code` = 'dev'
UNION ALL
SELECT 'dev-lisa',    `id` FROM `roles` WHERE `code` = 'dev'
UNION ALL
SELECT 'test-ava',    `id` FROM `roles` WHERE `code` = 'unit-test'
UNION ALL
SELECT 'test-ian',    `id` FROM `roles` WHERE `code` = 'unit-test'
UNION ALL
SELECT 'int-dana',    `id` FROM `roles` WHERE `code` = 'integration-test'
UNION ALL
SELECT 'int-marc',    `id` FROM `roles` WHERE `code` = 'integration-test'
UNION ALL
SELECT 'perf-maya',   `id` FROM `roles` WHERE `code` = 'performance-test'
UNION ALL
SELECT 'perf-derek',  `id` FROM `roles` WHERE `code` = 'performance-test'
UNION ALL
SELECT 'devops-sam',  `id` FROM `roles` WHERE `code` = 'devops'
UNION ALL
SELECT 'devops-raj',  `id` FROM `roles` WHERE `code` = 'devops';
