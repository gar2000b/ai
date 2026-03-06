-- ---------------------------------------------------------------------------
-- Data: Insert four workflows and their stages for project mep-sentinel (id=1)
-- Ref: OPEN-WORKFLOWS-PROJECT.md — Development, Performance Testing, DevOps, Manual
-- Run from trace_hq:  ./scripts/mysql.sh < database/data/02_workflows_and_stages.sql
-- ---------------------------------------------------------------------------

-- Workflow 1: Development
INSERT INTO `workflows` (`project_id`, `code`, `name`, `description`)
VALUES (1, 'development', 'Development', 'Deliver features from implementation through integration validation.');

INSERT INTO `workflow_stages` (`workflow_id`, `stage_order`, `stage_name`, `stage_role`)
VALUES
  (LAST_INSERT_ID(), 1,  'Todo',              'owner'),
  (LAST_INSERT_ID(), 2,  'Dev Planning',      'dev'),
  (LAST_INSERT_ID(), 3,  'Dev In Progress',   'dev'),
  (LAST_INSERT_ID(), 4,  'Dev Review',        'owner'),
  (LAST_INSERT_ID(), 5,  'Test Planning',     'unit-test'),
  (LAST_INSERT_ID(), 6,  'Test In Progress',  'unit-test'),
  (LAST_INSERT_ID(), 7,  'Test Review',       'owner'),
  (LAST_INSERT_ID(), 8,  'Int Test Planning', 'integration-test'),
  (LAST_INSERT_ID(), 9,  'Int Test In Progress', 'integration-test'),
  (LAST_INSERT_ID(), 10, 'Int Test Review',   'owner'),
  (LAST_INSERT_ID(), 11, 'Done',              'owner');

-- Workflow 2: Performance Testing
INSERT INTO `workflows` (`project_id`, `code`, `name`, `description`)
VALUES (1, 'performance', 'Performance Testing', 'Validate scalability and stress behaviour after integration stability.');

INSERT INTO `workflow_stages` (`workflow_id`, `stage_order`, `stage_name`, `stage_role`)
VALUES
  (LAST_INSERT_ID(), 1, 'Todo',        'owner'),
  (LAST_INSERT_ID(), 2, 'Planning',    'performance-test'),
  (LAST_INSERT_ID(), 3, 'In Progress', 'performance-test'),
  (LAST_INSERT_ID(), 4, 'Review',      'owner'),
  (LAST_INSERT_ID(), 5, 'Done',       'owner');

-- Workflow 3: DevOps
INSERT INTO `workflows` (`project_id`, `code`, `name`, `description`)
VALUES (1, 'devops', 'DevOps', 'Deploy, promote, and operationalize validated changes.');

INSERT INTO `workflow_stages` (`workflow_id`, `stage_order`, `stage_name`, `stage_role`)
VALUES
  (LAST_INSERT_ID(), 1, 'Todo',        'owner'),
  (LAST_INSERT_ID(), 2, 'Planning',    'devops'),
  (LAST_INSERT_ID(), 3, 'In Progress', 'devops'),
  (LAST_INSERT_ID(), 4, 'Review',      'owner'),
  (LAST_INSERT_ID(), 5, 'Done',       'owner');

-- Workflow 4: Manual
INSERT INTO `workflows` (`project_id`, `code`, `name`, `description`)
VALUES (1, 'manual', 'Manual', 'Process all user stories that require manual human work only.');

INSERT INTO `workflow_stages` (`workflow_id`, `stage_order`, `stage_name`, `stage_role`)
VALUES
  (LAST_INSERT_ID(), 1, 'Todo',        'owner'),
  (LAST_INSERT_ID(), 2, 'Planning',    'owner'),
  (LAST_INSERT_ID(), 3, 'In Progress', 'owner'),
  (LAST_INSERT_ID(), 4, 'Review',      'owner'),
  (LAST_INSERT_ID(), 5, 'Done',       'owner');
