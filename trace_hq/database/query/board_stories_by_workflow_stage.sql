-- ---------------------------------------------------------------------------
-- Query: Stories by workflow and stage (board view)
-- Returns Story, Title, Workflow, Agent (assignee), Stage in the same order
-- as the board: workflow (development → performance → devops → manual),
-- then stage order within each workflow.
-- Run from trace_hq:  ./scripts/mysql.sh < database/query/board_stories_by_workflow_stage.sql
-- ---------------------------------------------------------------------------

SELECT
  s.id          AS Story,
  s.title       AS Title,
  w.name        AS Workflow,
  COALESCE(a.name, 'unassigned') AS `Agent (assignee)`,
  ws.stage_name AS Stage
FROM stories s
JOIN workflows w           ON s.workflow_id = w.id
JOIN workflow_stages ws    ON s.workflow_stage_id = ws.id
LEFT JOIN agents a         ON s.assignee_id = a.id
ORDER BY w.id, ws.stage_order;
