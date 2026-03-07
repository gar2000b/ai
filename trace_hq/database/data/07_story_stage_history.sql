-- ---------------------------------------------------------------------------
-- Data: Insert story_stage_history from USER-STORIES.md stageHistory
-- Only "FromStage → ToStage" lines; notes and "Created" lines become single row into first stage.
-- Run from trace_hq:  ./scripts/mysql.sh < database/data/07_story_stage_history.sql
-- Requires: 04_stories.sql applied first.
-- ---------------------------------------------------------------------------

-- S001: Todo → Dev Planning; Dev Planning → Dev In Progress
INSERT INTO `story_stage_history` (`story_id`, `from_stage_name`, `to_stage_name`, `from_workflow_stage_id`, `to_workflow_stage_id`, `assignee_id`, `changed_by_agent_id`, `created_at`)
SELECT 'S001', 'Todo', 'Dev Planning', f.id, t.id, NULL, NULL, s.created_at FROM stories s
  JOIN workflow_stages f ON f.workflow_id = s.workflow_id AND f.stage_name = 'Todo'
  JOIN workflow_stages t ON t.workflow_id = s.workflow_id AND t.stage_name = 'Dev Planning'
  WHERE s.id = 'S001' LIMIT 1;
INSERT INTO `story_stage_history` (`story_id`, `from_stage_name`, `to_stage_name`, `from_workflow_stage_id`, `to_workflow_stage_id`, `assignee_id`, `changed_by_agent_id`, `created_at`)
SELECT 'S001', 'Dev Planning', 'Dev In Progress', f.id, t.id, NULL, NULL, s.created_at FROM stories s
  JOIN workflow_stages f ON f.workflow_id = s.workflow_id AND f.stage_name = 'Dev Planning'
  JOIN workflow_stages t ON t.workflow_id = s.workflow_id AND t.stage_name = 'Dev In Progress'
  WHERE s.id = 'S001' LIMIT 1;

-- S002: Todo → Planning; Planning → In Progress; In Progress → Review
INSERT INTO `story_stage_history` (`story_id`, `from_stage_name`, `to_stage_name`, `from_workflow_stage_id`, `to_workflow_stage_id`, `assignee_id`, `changed_by_agent_id`, `created_at`)
SELECT 'S002', 'Todo', 'Planning', f.id, t.id, NULL, NULL, s.created_at FROM stories s
  JOIN workflow_stages f ON f.workflow_id = s.workflow_id AND f.stage_name = 'Todo'
  JOIN workflow_stages t ON t.workflow_id = s.workflow_id AND t.stage_name = 'Planning' WHERE s.id = 'S002' LIMIT 1;
INSERT INTO `story_stage_history` (`story_id`, `from_stage_name`, `to_stage_name`, `from_workflow_stage_id`, `to_workflow_stage_id`, `assignee_id`, `changed_by_agent_id`, `created_at`)
SELECT 'S002', 'Planning', 'In Progress', f.id, t.id, NULL, NULL, s.created_at FROM stories s
  JOIN workflow_stages f ON f.workflow_id = s.workflow_id AND f.stage_name = 'Planning'
  JOIN workflow_stages t ON t.workflow_id = s.workflow_id AND t.stage_name = 'In Progress' WHERE s.id = 'S002' LIMIT 1;
INSERT INTO `story_stage_history` (`story_id`, `from_stage_name`, `to_stage_name`, `from_workflow_stage_id`, `to_workflow_stage_id`, `assignee_id`, `changed_by_agent_id`, `created_at`)
SELECT 'S002', 'In Progress', 'Review', f.id, t.id, NULL, NULL, s.created_at FROM stories s
  JOIN workflow_stages f ON f.workflow_id = s.workflow_id AND f.stage_name = 'In Progress'
  JOIN workflow_stages t ON t.workflow_id = s.workflow_id AND t.stage_name = 'Review' WHERE s.id = 'S002' LIMIT 1;

-- S003: Todo → Dev Planning; Dev Planning → Dev In Progress; Dev In Progress → Dev Review
INSERT INTO `story_stage_history` (`story_id`, `from_stage_name`, `to_stage_name`, `from_workflow_stage_id`, `to_workflow_stage_id`, `assignee_id`, `changed_by_agent_id`, `created_at`)
SELECT 'S003', 'Todo', 'Dev Planning', f.id, t.id, NULL, NULL, s.created_at FROM stories s
  JOIN workflow_stages f ON f.workflow_id = s.workflow_id AND f.stage_name = 'Todo'
  JOIN workflow_stages t ON t.workflow_id = s.workflow_id AND t.stage_name = 'Dev Planning' WHERE s.id = 'S003' LIMIT 1;
INSERT INTO `story_stage_history` (`story_id`, `from_stage_name`, `to_stage_name`, `from_workflow_stage_id`, `to_workflow_stage_id`, `assignee_id`, `changed_by_agent_id`, `created_at`)
SELECT 'S003', 'Dev Planning', 'Dev In Progress', f.id, t.id, NULL, NULL, s.created_at FROM stories s
  JOIN workflow_stages f ON f.workflow_id = s.workflow_id AND f.stage_name = 'Dev Planning'
  JOIN workflow_stages t ON t.workflow_id = s.workflow_id AND t.stage_name = 'Dev In Progress' WHERE s.id = 'S003' LIMIT 1;
INSERT INTO `story_stage_history` (`story_id`, `from_stage_name`, `to_stage_name`, `from_workflow_stage_id`, `to_workflow_stage_id`, `assignee_id`, `changed_by_agent_id`, `created_at`)
SELECT 'S003', 'Dev In Progress', 'Dev Review', f.id, t.id, NULL, NULL, s.created_at FROM stories s
  JOIN workflow_stages f ON f.workflow_id = s.workflow_id AND f.stage_name = 'Dev In Progress'
  JOIN workflow_stages t ON t.workflow_id = s.workflow_id AND t.stage_name = 'Dev Review' WHERE s.id = 'S003' LIMIT 1;

-- S004: Created in Todo
INSERT INTO `story_stage_history` (`story_id`, `from_stage_name`, `to_stage_name`, `from_workflow_stage_id`, `to_workflow_stage_id`, `assignee_id`, `changed_by_agent_id`, `created_at`)
SELECT 'S004', NULL, 'Todo', NULL, ws.id, NULL, NULL, s.created_at FROM stories s JOIN workflow_stages ws ON ws.workflow_id = s.workflow_id AND ws.stage_name = 'Todo' WHERE s.id = 'S004' LIMIT 1;

-- S005: Todo → Dev Planning
INSERT INTO `story_stage_history` (`story_id`, `from_stage_name`, `to_stage_name`, `from_workflow_stage_id`, `to_workflow_stage_id`, `assignee_id`, `changed_by_agent_id`, `created_at`)
SELECT 'S005', 'Todo', 'Dev Planning', f.id, t.id, NULL, NULL, s.created_at FROM stories s
  JOIN workflow_stages f ON f.workflow_id = s.workflow_id AND f.stage_name = 'Todo'
  JOIN workflow_stages t ON t.workflow_id = s.workflow_id AND t.stage_name = 'Dev Planning' WHERE s.id = 'S005' LIMIT 1;

-- S006: Todo → Planning; Planning → In Progress
INSERT INTO `story_stage_history` (`story_id`, `from_stage_name`, `to_stage_name`, `from_workflow_stage_id`, `to_workflow_stage_id`, `assignee_id`, `changed_by_agent_id`, `created_at`)
SELECT 'S006', 'Todo', 'Planning', f.id, t.id, NULL, NULL, s.created_at FROM stories s
  JOIN workflow_stages f ON f.workflow_id = s.workflow_id AND f.stage_name = 'Todo'
  JOIN workflow_stages t ON t.workflow_id = s.workflow_id AND t.stage_name = 'Planning' WHERE s.id = 'S006' LIMIT 1;
INSERT INTO `story_stage_history` (`story_id`, `from_stage_name`, `to_stage_name`, `from_workflow_stage_id`, `to_workflow_stage_id`, `assignee_id`, `changed_by_agent_id`, `created_at`)
SELECT 'S006', 'Planning', 'In Progress', f.id, t.id, NULL, NULL, s.created_at FROM stories s
  JOIN workflow_stages f ON f.workflow_id = s.workflow_id AND f.stage_name = 'Planning'
  JOIN workflow_stages t ON t.workflow_id = s.workflow_id AND t.stage_name = 'In Progress' WHERE s.id = 'S006' LIMIT 1;

-- S007: Todo → Planning; Planning → In Progress; In Progress → Review
INSERT INTO `story_stage_history` (`story_id`, `from_stage_name`, `to_stage_name`, `from_workflow_stage_id`, `to_workflow_stage_id`, `assignee_id`, `changed_by_agent_id`, `created_at`)
SELECT 'S007', 'Todo', 'Planning', f.id, t.id, NULL, NULL, s.created_at FROM stories s
  JOIN workflow_stages f ON f.workflow_id = s.workflow_id AND f.stage_name = 'Todo'
  JOIN workflow_stages t ON t.workflow_id = s.workflow_id AND t.stage_name = 'Planning' WHERE s.id = 'S007' LIMIT 1;
INSERT INTO `story_stage_history` (`story_id`, `from_stage_name`, `to_stage_name`, `from_workflow_stage_id`, `to_workflow_stage_id`, `assignee_id`, `changed_by_agent_id`, `created_at`)
SELECT 'S007', 'Planning', 'In Progress', f.id, t.id, NULL, NULL, s.created_at FROM stories s
  JOIN workflow_stages f ON f.workflow_id = s.workflow_id AND f.stage_name = 'Planning'
  JOIN workflow_stages t ON t.workflow_id = s.workflow_id AND t.stage_name = 'In Progress' WHERE s.id = 'S007' LIMIT 1;
INSERT INTO `story_stage_history` (`story_id`, `from_stage_name`, `to_stage_name`, `from_workflow_stage_id`, `to_workflow_stage_id`, `assignee_id`, `changed_by_agent_id`, `created_at`)
SELECT 'S007', 'In Progress', 'Review', f.id, t.id, NULL, NULL, s.created_at FROM stories s
  JOIN workflow_stages f ON f.workflow_id = s.workflow_id AND f.stage_name = 'In Progress'
  JOIN workflow_stages t ON t.workflow_id = s.workflow_id AND t.stage_name = 'Review' WHERE s.id = 'S007' LIMIT 1;

-- S008: Todo → Planning; Planning → In Progress
INSERT INTO `story_stage_history` (`story_id`, `from_stage_name`, `to_stage_name`, `from_workflow_stage_id`, `to_workflow_stage_id`, `assignee_id`, `changed_by_agent_id`, `created_at`)
SELECT 'S008', 'Todo', 'Planning', f.id, t.id, NULL, NULL, s.created_at FROM stories s
  JOIN workflow_stages f ON f.workflow_id = s.workflow_id AND f.stage_name = 'Todo'
  JOIN workflow_stages t ON t.workflow_id = s.workflow_id AND t.stage_name = 'Planning' WHERE s.id = 'S008' LIMIT 1;
INSERT INTO `story_stage_history` (`story_id`, `from_stage_name`, `to_stage_name`, `from_workflow_stage_id`, `to_workflow_stage_id`, `assignee_id`, `changed_by_agent_id`, `created_at`)
SELECT 'S008', 'Planning', 'In Progress', f.id, t.id, NULL, NULL, s.created_at FROM stories s
  JOIN workflow_stages f ON f.workflow_id = s.workflow_id AND f.stage_name = 'Planning'
  JOIN workflow_stages t ON t.workflow_id = s.workflow_id AND t.stage_name = 'In Progress' WHERE s.id = 'S008' LIMIT 1;

-- S009: Todo → Dev Planning; Dev Planning → Dev In Progress; Dev In Progress → Dev Review; Dev Review → Done
INSERT INTO `story_stage_history` (`story_id`, `from_stage_name`, `to_stage_name`, `from_workflow_stage_id`, `to_workflow_stage_id`, `assignee_id`, `changed_by_agent_id`, `created_at`)
SELECT 'S009', 'Todo', 'Dev Planning', f.id, t.id, NULL, NULL, s.created_at FROM stories s
  JOIN workflow_stages f ON f.workflow_id = s.workflow_id AND f.stage_name = 'Todo'
  JOIN workflow_stages t ON t.workflow_id = s.workflow_id AND t.stage_name = 'Dev Planning' WHERE s.id = 'S009' LIMIT 1;
INSERT INTO `story_stage_history` (`story_id`, `from_stage_name`, `to_stage_name`, `from_workflow_stage_id`, `to_workflow_stage_id`, `assignee_id`, `changed_by_agent_id`, `created_at`)
SELECT 'S009', 'Dev Planning', 'Dev In Progress', f.id, t.id, NULL, NULL, s.created_at FROM stories s
  JOIN workflow_stages f ON f.workflow_id = s.workflow_id AND f.stage_name = 'Dev Planning'
  JOIN workflow_stages t ON t.workflow_id = s.workflow_id AND t.stage_name = 'Dev In Progress' WHERE s.id = 'S009' LIMIT 1;
INSERT INTO `story_stage_history` (`story_id`, `from_stage_name`, `to_stage_name`, `from_workflow_stage_id`, `to_workflow_stage_id`, `assignee_id`, `changed_by_agent_id`, `created_at`)
SELECT 'S009', 'Dev In Progress', 'Dev Review', f.id, t.id, NULL, NULL, s.created_at FROM stories s
  JOIN workflow_stages f ON f.workflow_id = s.workflow_id AND f.stage_name = 'Dev In Progress'
  JOIN workflow_stages t ON t.workflow_id = s.workflow_id AND t.stage_name = 'Dev Review' WHERE s.id = 'S009' LIMIT 1;
INSERT INTO `story_stage_history` (`story_id`, `from_stage_name`, `to_stage_name`, `from_workflow_stage_id`, `to_workflow_stage_id`, `assignee_id`, `changed_by_agent_id`, `created_at`)
SELECT 'S009', 'Dev Review', 'Done', f.id, t.id, NULL, NULL, s.created_at FROM stories s
  JOIN workflow_stages f ON f.workflow_id = s.workflow_id AND f.stage_name = 'Dev Review'
  JOIN workflow_stages t ON t.workflow_id = s.workflow_id AND t.stage_name = 'Done' WHERE s.id = 'S009' LIMIT 1;

-- S010: Created (in Todo)
INSERT INTO `story_stage_history` (`story_id`, `from_stage_name`, `to_stage_name`, `from_workflow_stage_id`, `to_workflow_stage_id`, `assignee_id`, `changed_by_agent_id`, `created_at`)
SELECT 'S010', NULL, 'Todo', NULL, ws.id, NULL, NULL, s.created_at FROM stories s JOIN workflow_stages ws ON ws.workflow_id = s.workflow_id AND ws.stage_name = 'Todo' WHERE s.id = 'S010' LIMIT 1;

-- S011: Todo → Test Planning
INSERT INTO `story_stage_history` (`story_id`, `from_stage_name`, `to_stage_name`, `from_workflow_stage_id`, `to_workflow_stage_id`, `assignee_id`, `changed_by_agent_id`, `created_at`)
SELECT 'S011', 'Todo', 'Test Planning', f.id, t.id, NULL, NULL, s.created_at FROM stories s
  JOIN workflow_stages f ON f.workflow_id = s.workflow_id AND f.stage_name = 'Todo'
  JOIN workflow_stages t ON t.workflow_id = s.workflow_id AND t.stage_name = 'Test Planning' WHERE s.id = 'S011' LIMIT 1;

-- S012: Todo → Test Planning; Test Planning → Test In Progress
INSERT INTO `story_stage_history` (`story_id`, `from_stage_name`, `to_stage_name`, `from_workflow_stage_id`, `to_workflow_stage_id`, `assignee_id`, `changed_by_agent_id`, `created_at`)
SELECT 'S012', 'Todo', 'Test Planning', f.id, t.id, NULL, NULL, s.created_at FROM stories s
  JOIN workflow_stages f ON f.workflow_id = s.workflow_id AND f.stage_name = 'Todo'
  JOIN workflow_stages t ON t.workflow_id = s.workflow_id AND t.stage_name = 'Test Planning' WHERE s.id = 'S012' LIMIT 1;
INSERT INTO `story_stage_history` (`story_id`, `from_stage_name`, `to_stage_name`, `from_workflow_stage_id`, `to_workflow_stage_id`, `assignee_id`, `changed_by_agent_id`, `created_at`)
SELECT 'S012', 'Test Planning', 'Test In Progress', f.id, t.id, NULL, NULL, s.created_at FROM stories s
  JOIN workflow_stages f ON f.workflow_id = s.workflow_id AND f.stage_name = 'Test Planning'
  JOIN workflow_stages t ON t.workflow_id = s.workflow_id AND t.stage_name = 'Test In Progress' WHERE s.id = 'S012' LIMIT 1;

-- S013: Todo → Test Planning; Test Planning → Test In Progress; Test In Progress → Test Review
INSERT INTO `story_stage_history` (`story_id`, `from_stage_name`, `to_stage_name`, `from_workflow_stage_id`, `to_workflow_stage_id`, `assignee_id`, `changed_by_agent_id`, `created_at`)
SELECT 'S013', 'Todo', 'Test Planning', f.id, t.id, NULL, NULL, s.created_at FROM stories s
  JOIN workflow_stages f ON f.workflow_id = s.workflow_id AND f.stage_name = 'Todo'
  JOIN workflow_stages t ON t.workflow_id = s.workflow_id AND t.stage_name = 'Test Planning' WHERE s.id = 'S013' LIMIT 1;
INSERT INTO `story_stage_history` (`story_id`, `from_stage_name`, `to_stage_name`, `from_workflow_stage_id`, `to_workflow_stage_id`, `assignee_id`, `changed_by_agent_id`, `created_at`)
SELECT 'S013', 'Test Planning', 'Test In Progress', f.id, t.id, NULL, NULL, s.created_at FROM stories s
  JOIN workflow_stages f ON f.workflow_id = s.workflow_id AND f.stage_name = 'Test Planning'
  JOIN workflow_stages t ON t.workflow_id = s.workflow_id AND t.stage_name = 'Test In Progress' WHERE s.id = 'S013' LIMIT 1;
INSERT INTO `story_stage_history` (`story_id`, `from_stage_name`, `to_stage_name`, `from_workflow_stage_id`, `to_workflow_stage_id`, `assignee_id`, `changed_by_agent_id`, `created_at`)
SELECT 'S013', 'Test In Progress', 'Test Review', f.id, t.id, NULL, NULL, s.created_at FROM stories s
  JOIN workflow_stages f ON f.workflow_id = s.workflow_id AND f.stage_name = 'Test In Progress'
  JOIN workflow_stages t ON t.workflow_id = s.workflow_id AND t.stage_name = 'Test Review' WHERE s.id = 'S013' LIMIT 1;

-- S014: Todo → Int Test Planning
INSERT INTO `story_stage_history` (`story_id`, `from_stage_name`, `to_stage_name`, `from_workflow_stage_id`, `to_workflow_stage_id`, `assignee_id`, `changed_by_agent_id`, `created_at`)
SELECT 'S014', 'Todo', 'Int Test Planning', f.id, t.id, NULL, NULL, s.created_at FROM stories s
  JOIN workflow_stages f ON f.workflow_id = s.workflow_id AND f.stage_name = 'Todo'
  JOIN workflow_stages t ON t.workflow_id = s.workflow_id AND t.stage_name = 'Int Test Planning' WHERE s.id = 'S014' LIMIT 1;

-- S015: Todo → Int Test Planning; Int Test Planning → Int Test In Progress
INSERT INTO `story_stage_history` (`story_id`, `from_stage_name`, `to_stage_name`, `from_workflow_stage_id`, `to_workflow_stage_id`, `assignee_id`, `changed_by_agent_id`, `created_at`)
SELECT 'S015', 'Todo', 'Int Test Planning', f.id, t.id, NULL, NULL, s.created_at FROM stories s
  JOIN workflow_stages f ON f.workflow_id = s.workflow_id AND f.stage_name = 'Todo'
  JOIN workflow_stages t ON t.workflow_id = s.workflow_id AND t.stage_name = 'Int Test Planning' WHERE s.id = 'S015' LIMIT 1;
INSERT INTO `story_stage_history` (`story_id`, `from_stage_name`, `to_stage_name`, `from_workflow_stage_id`, `to_workflow_stage_id`, `assignee_id`, `changed_by_agent_id`, `created_at`)
SELECT 'S015', 'Int Test Planning', 'Int Test In Progress', f.id, t.id, NULL, NULL, s.created_at FROM stories s
  JOIN workflow_stages f ON f.workflow_id = s.workflow_id AND f.stage_name = 'Int Test Planning'
  JOIN workflow_stages t ON t.workflow_id = s.workflow_id AND t.stage_name = 'Int Test In Progress' WHERE s.id = 'S015' LIMIT 1;

-- S016: Todo → Int Test Planning; Int Test Planning → Int Test In Progress; Int Test In Progress → Int Test Review
INSERT INTO `story_stage_history` (`story_id`, `from_stage_name`, `to_stage_name`, `from_workflow_stage_id`, `to_workflow_stage_id`, `assignee_id`, `changed_by_agent_id`, `created_at`)
SELECT 'S016', 'Todo', 'Int Test Planning', f.id, t.id, NULL, NULL, s.created_at FROM stories s
  JOIN workflow_stages f ON f.workflow_id = s.workflow_id AND f.stage_name = 'Todo'
  JOIN workflow_stages t ON t.workflow_id = s.workflow_id AND t.stage_name = 'Int Test Planning' WHERE s.id = 'S016' LIMIT 1;
INSERT INTO `story_stage_history` (`story_id`, `from_stage_name`, `to_stage_name`, `from_workflow_stage_id`, `to_workflow_stage_id`, `assignee_id`, `changed_by_agent_id`, `created_at`)
SELECT 'S016', 'Int Test Planning', 'Int Test In Progress', f.id, t.id, NULL, NULL, s.created_at FROM stories s
  JOIN workflow_stages f ON f.workflow_id = s.workflow_id AND f.stage_name = 'Int Test Planning'
  JOIN workflow_stages t ON t.workflow_id = s.workflow_id AND t.stage_name = 'Int Test In Progress' WHERE s.id = 'S016' LIMIT 1;
INSERT INTO `story_stage_history` (`story_id`, `from_stage_name`, `to_stage_name`, `from_workflow_stage_id`, `to_workflow_stage_id`, `assignee_id`, `changed_by_agent_id`, `created_at`)
SELECT 'S016', 'Int Test In Progress', 'Int Test Review', f.id, t.id, NULL, NULL, s.created_at FROM stories s
  JOIN workflow_stages f ON f.workflow_id = s.workflow_id AND f.stage_name = 'Int Test In Progress'
  JOIN workflow_stages t ON t.workflow_id = s.workflow_id AND t.stage_name = 'Int Test Review' WHERE s.id = 'S016' LIMIT 1;

-- S017: Created in Todo
INSERT INTO `story_stage_history` (`story_id`, `from_stage_name`, `to_stage_name`, `from_workflow_stage_id`, `to_workflow_stage_id`, `assignee_id`, `changed_by_agent_id`, `created_at`)
SELECT 'S017', NULL, 'Todo', NULL, ws.id, NULL, NULL, s.created_at FROM stories s JOIN workflow_stages ws ON ws.workflow_id = s.workflow_id AND ws.stage_name = 'Todo' WHERE s.id = 'S017' LIMIT 1;

-- S018: Todo → Planning
INSERT INTO `story_stage_history` (`story_id`, `from_stage_name`, `to_stage_name`, `from_workflow_stage_id`, `to_workflow_stage_id`, `assignee_id`, `changed_by_agent_id`, `created_at`)
SELECT 'S018', 'Todo', 'Planning', f.id, t.id, NULL, NULL, s.created_at FROM stories s
  JOIN workflow_stages f ON f.workflow_id = s.workflow_id AND f.stage_name = 'Todo'
  JOIN workflow_stages t ON t.workflow_id = s.workflow_id AND t.stage_name = 'Planning' WHERE s.id = 'S018' LIMIT 1;

-- S019: Todo → Planning; Planning → In Progress; In Progress → Review
INSERT INTO `story_stage_history` (`story_id`, `from_stage_name`, `to_stage_name`, `from_workflow_stage_id`, `to_workflow_stage_id`, `assignee_id`, `changed_by_agent_id`, `created_at`)
SELECT 'S019', 'Todo', 'Planning', f.id, t.id, NULL, NULL, s.created_at FROM stories s
  JOIN workflow_stages f ON f.workflow_id = s.workflow_id AND f.stage_name = 'Todo'
  JOIN workflow_stages t ON t.workflow_id = s.workflow_id AND t.stage_name = 'Planning' WHERE s.id = 'S019' LIMIT 1;
INSERT INTO `story_stage_history` (`story_id`, `from_stage_name`, `to_stage_name`, `from_workflow_stage_id`, `to_workflow_stage_id`, `assignee_id`, `changed_by_agent_id`, `created_at`)
SELECT 'S019', 'Planning', 'In Progress', f.id, t.id, NULL, NULL, s.created_at FROM stories s
  JOIN workflow_stages f ON f.workflow_id = s.workflow_id AND f.stage_name = 'Planning'
  JOIN workflow_stages t ON t.workflow_id = s.workflow_id AND t.stage_name = 'In Progress' WHERE s.id = 'S019' LIMIT 1;
INSERT INTO `story_stage_history` (`story_id`, `from_stage_name`, `to_stage_name`, `from_workflow_stage_id`, `to_workflow_stage_id`, `assignee_id`, `changed_by_agent_id`, `created_at`)
SELECT 'S019', 'In Progress', 'Review', f.id, t.id, NULL, NULL, s.created_at FROM stories s
  JOIN workflow_stages f ON f.workflow_id = s.workflow_id AND f.stage_name = 'In Progress'
  JOIN workflow_stages t ON t.workflow_id = s.workflow_id AND t.stage_name = 'Review' WHERE s.id = 'S019' LIMIT 1;

-- S020: Todo → Planning; Planning → In Progress; In Progress → Review; Review → Done
INSERT INTO `story_stage_history` (`story_id`, `from_stage_name`, `to_stage_name`, `from_workflow_stage_id`, `to_workflow_stage_id`, `assignee_id`, `changed_by_agent_id`, `created_at`)
SELECT 'S020', 'Todo', 'Planning', f.id, t.id, NULL, NULL, s.created_at FROM stories s
  JOIN workflow_stages f ON f.workflow_id = s.workflow_id AND f.stage_name = 'Todo'
  JOIN workflow_stages t ON t.workflow_id = s.workflow_id AND t.stage_name = 'Planning' WHERE s.id = 'S020' LIMIT 1;
INSERT INTO `story_stage_history` (`story_id`, `from_stage_name`, `to_stage_name`, `from_workflow_stage_id`, `to_workflow_stage_id`, `assignee_id`, `changed_by_agent_id`, `created_at`)
SELECT 'S020', 'Planning', 'In Progress', f.id, t.id, NULL, NULL, s.created_at FROM stories s
  JOIN workflow_stages f ON f.workflow_id = s.workflow_id AND f.stage_name = 'Planning'
  JOIN workflow_stages t ON t.workflow_id = s.workflow_id AND t.stage_name = 'In Progress' WHERE s.id = 'S020' LIMIT 1;
INSERT INTO `story_stage_history` (`story_id`, `from_stage_name`, `to_stage_name`, `from_workflow_stage_id`, `to_workflow_stage_id`, `assignee_id`, `changed_by_agent_id`, `created_at`)
SELECT 'S020', 'In Progress', 'Review', f.id, t.id, NULL, NULL, s.created_at FROM stories s
  JOIN workflow_stages f ON f.workflow_id = s.workflow_id AND f.stage_name = 'In Progress'
  JOIN workflow_stages t ON t.workflow_id = s.workflow_id AND t.stage_name = 'Review' WHERE s.id = 'S020' LIMIT 1;
INSERT INTO `story_stage_history` (`story_id`, `from_stage_name`, `to_stage_name`, `from_workflow_stage_id`, `to_workflow_stage_id`, `assignee_id`, `changed_by_agent_id`, `created_at`)
SELECT 'S020', 'Review', 'Done', f.id, t.id, NULL, NULL, s.created_at FROM stories s
  JOIN workflow_stages f ON f.workflow_id = s.workflow_id AND f.stage_name = 'Review'
  JOIN workflow_stages t ON t.workflow_id = s.workflow_id AND t.stage_name = 'Done' WHERE s.id = 'S020' LIMIT 1;

-- S022: Todo → Planning
INSERT INTO `story_stage_history` (`story_id`, `from_stage_name`, `to_stage_name`, `from_workflow_stage_id`, `to_workflow_stage_id`, `assignee_id`, `changed_by_agent_id`, `created_at`)
SELECT 'S022', 'Todo', 'Planning', f.id, t.id, NULL, NULL, s.created_at FROM stories s
  JOIN workflow_stages f ON f.workflow_id = s.workflow_id AND f.stage_name = 'Todo'
  JOIN workflow_stages t ON t.workflow_id = s.workflow_id AND t.stage_name = 'Planning' WHERE s.id = 'S022' LIMIT 1;

-- S023: Todo → Planning; Planning → In Progress
INSERT INTO `story_stage_history` (`story_id`, `from_stage_name`, `to_stage_name`, `from_workflow_stage_id`, `to_workflow_stage_id`, `assignee_id`, `changed_by_agent_id`, `created_at`)
SELECT 'S023', 'Todo', 'Planning', f.id, t.id, NULL, NULL, s.created_at FROM stories s
  JOIN workflow_stages f ON f.workflow_id = s.workflow_id AND f.stage_name = 'Todo'
  JOIN workflow_stages t ON t.workflow_id = s.workflow_id AND t.stage_name = 'Planning' WHERE s.id = 'S023' LIMIT 1;
INSERT INTO `story_stage_history` (`story_id`, `from_stage_name`, `to_stage_name`, `from_workflow_stage_id`, `to_workflow_stage_id`, `assignee_id`, `changed_by_agent_id`, `created_at`)
SELECT 'S023', 'Planning', 'In Progress', f.id, t.id, NULL, NULL, s.created_at FROM stories s
  JOIN workflow_stages f ON f.workflow_id = s.workflow_id AND f.stage_name = 'Planning'
  JOIN workflow_stages t ON t.workflow_id = s.workflow_id AND t.stage_name = 'In Progress' WHERE s.id = 'S023' LIMIT 1;

-- S024: Todo → Planning; Planning → In Progress; In Progress → Review; Review → Done
INSERT INTO `story_stage_history` (`story_id`, `from_stage_name`, `to_stage_name`, `from_workflow_stage_id`, `to_workflow_stage_id`, `assignee_id`, `changed_by_agent_id`, `created_at`)
SELECT 'S024', 'Todo', 'Planning', f.id, t.id, NULL, NULL, s.created_at FROM stories s
  JOIN workflow_stages f ON f.workflow_id = s.workflow_id AND f.stage_name = 'Todo'
  JOIN workflow_stages t ON t.workflow_id = s.workflow_id AND t.stage_name = 'Planning' WHERE s.id = 'S024' LIMIT 1;
INSERT INTO `story_stage_history` (`story_id`, `from_stage_name`, `to_stage_name`, `from_workflow_stage_id`, `to_workflow_stage_id`, `assignee_id`, `changed_by_agent_id`, `created_at`)
SELECT 'S024', 'Planning', 'In Progress', f.id, t.id, NULL, NULL, s.created_at FROM stories s
  JOIN workflow_stages f ON f.workflow_id = s.workflow_id AND f.stage_name = 'Planning'
  JOIN workflow_stages t ON t.workflow_id = s.workflow_id AND t.stage_name = 'In Progress' WHERE s.id = 'S024' LIMIT 1;
INSERT INTO `story_stage_history` (`story_id`, `from_stage_name`, `to_stage_name`, `from_workflow_stage_id`, `to_workflow_stage_id`, `assignee_id`, `changed_by_agent_id`, `created_at`)
SELECT 'S024', 'In Progress', 'Review', f.id, t.id, NULL, NULL, s.created_at FROM stories s
  JOIN workflow_stages f ON f.workflow_id = s.workflow_id AND f.stage_name = 'In Progress'
  JOIN workflow_stages t ON t.workflow_id = s.workflow_id AND t.stage_name = 'Review' WHERE s.id = 'S024' LIMIT 1;
INSERT INTO `story_stage_history` (`story_id`, `from_stage_name`, `to_stage_name`, `from_workflow_stage_id`, `to_workflow_stage_id`, `assignee_id`, `changed_by_agent_id`, `created_at`)
SELECT 'S024', 'Review', 'Done', f.id, t.id, NULL, NULL, s.created_at FROM stories s
  JOIN workflow_stages f ON f.workflow_id = s.workflow_id AND f.stage_name = 'Review'
  JOIN workflow_stages t ON t.workflow_id = s.workflow_id AND t.stage_name = 'Done' WHERE s.id = 'S024' LIMIT 1;

-- S025: Todo → Planning
INSERT INTO `story_stage_history` (`story_id`, `from_stage_name`, `to_stage_name`, `from_workflow_stage_id`, `to_workflow_stage_id`, `assignee_id`, `changed_by_agent_id`, `created_at`)
SELECT 'S025', 'Todo', 'Planning', f.id, t.id, NULL, NULL, s.created_at FROM stories s
  JOIN workflow_stages f ON f.workflow_id = s.workflow_id AND f.stage_name = 'Todo'
  JOIN workflow_stages t ON t.workflow_id = s.workflow_id AND t.stage_name = 'Planning' WHERE s.id = 'S025' LIMIT 1;

-- S026: Todo → Planning; Planning → In Progress; In Progress → Review; Review → Done
INSERT INTO `story_stage_history` (`story_id`, `from_stage_name`, `to_stage_name`, `from_workflow_stage_id`, `to_workflow_stage_id`, `assignee_id`, `changed_by_agent_id`, `created_at`)
SELECT 'S026', 'Todo', 'Planning', f.id, t.id, NULL, NULL, s.created_at FROM stories s
  JOIN workflow_stages f ON f.workflow_id = s.workflow_id AND f.stage_name = 'Todo'
  JOIN workflow_stages t ON t.workflow_id = s.workflow_id AND t.stage_name = 'Planning' WHERE s.id = 'S026' LIMIT 1;
INSERT INTO `story_stage_history` (`story_id`, `from_stage_name`, `to_stage_name`, `from_workflow_stage_id`, `to_workflow_stage_id`, `assignee_id`, `changed_by_agent_id`, `created_at`)
SELECT 'S026', 'Planning', 'In Progress', f.id, t.id, NULL, NULL, s.created_at FROM stories s
  JOIN workflow_stages f ON f.workflow_id = s.workflow_id AND f.stage_name = 'Planning'
  JOIN workflow_stages t ON t.workflow_id = s.workflow_id AND t.stage_name = 'In Progress' WHERE s.id = 'S026' LIMIT 1;
INSERT INTO `story_stage_history` (`story_id`, `from_stage_name`, `to_stage_name`, `from_workflow_stage_id`, `to_workflow_stage_id`, `assignee_id`, `changed_by_agent_id`, `created_at`)
SELECT 'S026', 'In Progress', 'Review', f.id, t.id, NULL, NULL, s.created_at FROM stories s
  JOIN workflow_stages f ON f.workflow_id = s.workflow_id AND f.stage_name = 'In Progress'
  JOIN workflow_stages t ON t.workflow_id = s.workflow_id AND t.stage_name = 'Review' WHERE s.id = 'S026' LIMIT 1;
INSERT INTO `story_stage_history` (`story_id`, `from_stage_name`, `to_stage_name`, `from_workflow_stage_id`, `to_workflow_stage_id`, `assignee_id`, `changed_by_agent_id`, `created_at`)
SELECT 'S026', 'Review', 'Done', f.id, t.id, NULL, NULL, s.created_at FROM stories s
  JOIN workflow_stages f ON f.workflow_id = s.workflow_id AND f.stage_name = 'Review'
  JOIN workflow_stages t ON t.workflow_id = s.workflow_id AND t.stage_name = 'Done' WHERE s.id = 'S026' LIMIT 1;

-- S027: Created in Todo
INSERT INTO `story_stage_history` (`story_id`, `from_stage_name`, `to_stage_name`, `from_workflow_stage_id`, `to_workflow_stage_id`, `assignee_id`, `changed_by_agent_id`, `created_at`)
SELECT 'S027', NULL, 'Todo', NULL, ws.id, NULL, NULL, s.created_at FROM stories s JOIN workflow_stages ws ON ws.workflow_id = s.workflow_id AND ws.stage_name = 'Todo' WHERE s.id = 'S027' LIMIT 1;
