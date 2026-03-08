-- ---------------------------------------------------------------------------
-- Data: Insert user stories (S001–S027)
-- Ref: USER-STORIES.md
-- Run from trace_hq:  ./scripts/mysql.sh < database/data/04_stories.sql
-- Requires: 01, 02, 03 applied first.
-- ---------------------------------------------------------------------------

INSERT INTO `stories` (
  `id`, `title`, `description`, `type`, `priority`, `project_id`, `workflow_id`, `workflow_stage_id`,
  `created_by_agent_id`, `created_at`, `last_updated_at`, `blocked`, `blocked_reason`, `blocked_at`, `blocked_by`,
  `assignee_id`, `acceptance_criteria`, `implementation_notes`, `branch`, `review_reference`, `artifact`,
  `review_status`, `review_notes`, `rejection_count`
) VALUES
('S001', 'Add /health endpoint', 'Implement a basic health endpoint returning service status.', 'dev', 'medium', 1,
 (SELECT id FROM workflows WHERE project_id = 1 AND code = 'development'),
 (SELECT ws.id FROM workflow_stages ws JOIN workflows w ON ws.workflow_id = w.id WHERE w.project_id = 1 AND w.code = 'development' AND ws.stage_name = 'Dev In Progress'),
 (SELECT id FROM agents WHERE name = 'owner'), '2026-03-01 09:00:00', '2026-03-02 10:15:00', 0, NULL, NULL, NULL,
 (SELECT id FROM agents WHERE name = 'dev-lisa'), 'GET /health returns 200; Response JSON is { "ok": true }', 'Basic Express route, no auth required.', 'dev/S001-health', NULL, NULL, 'not-required', NULL, 0),

('S002', 'Write Getting Started guide', 'Create onboarding documentation for new developers.', 'writing', 'high', 1,
 (SELECT id FROM workflows WHERE project_id = 1 AND code = 'manual'),
 (SELECT ws.id FROM workflow_stages ws JOIN workflows w ON ws.workflow_id = w.id WHERE w.project_id = 1 AND w.code = 'manual' AND ws.stage_name = 'Review'),
 (SELECT id FROM agents WHERE name = 'owner'), '2026-03-01 09:05:00', '2026-03-02 08:30:00', 0, NULL, NULL, NULL,
 (SELECT id FROM agents WHERE name = 'owner'), 'Includes install instructions; Includes run instructions; Includes environment variables section', 'Draft written manually.', NULL, 'docs/GETTING_STARTED.md', 'docs/GETTING_STARTED.md', 'pending', NULL, 0),

('S003', 'Add config loader', 'Support loading configuration from env and JSON file.', 'dev', 'high', 1,
 (SELECT id FROM workflows WHERE project_id = 1 AND code = 'development'),
 (SELECT ws.id FROM workflow_stages ws JOIN workflows w ON ws.workflow_id = w.id WHERE w.project_id = 1 AND w.code = 'development' AND ws.stage_name = 'Dev Review'),
 (SELECT id FROM agents WHERE name = 'owner'), '2026-03-01 09:10:00', '2026-03-03 11:00:00', 0, NULL, NULL, NULL,
 (SELECT id FROM agents WHERE name = 'owner'), NULL, 'Schema validation added.', 'dev/S003-config', 'PR#18', NULL, 'pending', NULL, 0),

('S004', 'Add integration test for config loader', 'Validate config loader across env + file interaction.', 'dev', 'medium', 1,
 (SELECT id FROM workflows WHERE project_id = 1 AND code = 'development'),
 (SELECT ws.id FROM workflow_stages ws JOIN workflows w ON ws.workflow_id = w.id WHERE w.project_id = 1 AND w.code = 'development' AND ws.stage_name = 'Todo'),
 (SELECT id FROM agents WHERE name = 'owner'), '2026-03-01 09:20:00', '2026-03-03 12:00:00', 1, 'Waiting for S003 to reach Done.', '2026-03-03 12:00:00', 'dependency',
 NULL, 'Validates mixed env + file behavior; Covers invalid config cases', NULL, NULL, NULL, NULL, 'not-required', NULL, 0),

('S005', 'Add readiness endpoint', 'Add readiness endpoint dependent on health endpoint.', 'dev', 'low', 1,
 (SELECT id FROM workflows WHERE project_id = 1 AND code = 'development'),
 (SELECT ws.id FROM workflow_stages ws JOIN workflows w ON ws.workflow_id = w.id WHERE w.project_id = 1 AND w.code = 'development' AND ws.stage_name = 'Dev Planning'),
 (SELECT id FROM agents WHERE name = 'owner'), '2026-03-01 09:30:00', '2026-03-02 10:20:00', 0, NULL, NULL, NULL,
 (SELECT id FROM agents WHERE name = 'dev-bob'), NULL, NULL, 'dev/S005-readiness', NULL, NULL, 'not-required', NULL, 0),

('S006', 'Load test /health endpoint', 'Validate health endpoint under 10k RPS load.', 'performance', 'medium', 1,
 (SELECT id FROM workflows WHERE project_id = 1 AND code = 'performance'),
 (SELECT ws.id FROM workflow_stages ws JOIN workflows w ON ws.workflow_id = w.id WHERE w.project_id = 1 AND w.code = 'performance' AND ws.stage_name = 'In Progress'),
 (SELECT id FROM agents WHERE name = 'owner'), '2026-03-02 09:00:00', '2026-03-02 15:00:00', 0, NULL, NULL, NULL,
 (SELECT id FROM agents WHERE name = 'perf-derek'), 'Sustains 10k RPS; < 100ms average latency', 'Using k6 harness.', NULL, NULL, 'performance/report-S006.json', 'not-required', NULL, 0),

('S007', 'Setup staging deployment pipeline', 'Add GitHub Actions workflow for staging environment.', 'infrastructure', 'high', 1,
 (SELECT id FROM workflows WHERE project_id = 1 AND code = 'devops'),
 (SELECT ws.id FROM workflow_stages ws JOIN workflows w ON ws.workflow_id = w.id WHERE w.project_id = 1 AND w.code = 'devops' AND ws.stage_name = 'Review'),
 (SELECT id FROM agents WHERE name = 'owner'), '2026-03-02 09:15:00', '2026-03-02 14:00:00', 0, NULL, NULL, NULL,
 (SELECT id FROM agents WHERE name = 'owner'), 'Auto deploy on main branch; Secrets stored securely', 'Workflow YAML added.', 'devops/S007-staging', 'PR#21', NULL, 'pending', NULL, 0),

('S008', 'Research JWT expiry strategy', 'Evaluate token expiry and refresh approaches.', 'research', 'medium', 1,
 (SELECT id FROM workflows WHERE project_id = 1 AND code = 'manual'),
 (SELECT ws.id FROM workflow_stages ws JOIN workflows w ON ws.workflow_id = w.id WHERE w.project_id = 1 AND w.code = 'manual' AND ws.stage_name = 'In Progress'),
 (SELECT id FROM agents WHERE name = 'owner'), '2026-03-02 10:00:00', '2026-03-02 12:00:00', 0, NULL, NULL, NULL,
 (SELECT id FROM agents WHERE name = 'owner'), 'Compare 3 expiry strategies; Document pros/cons', NULL, NULL, NULL, 'docs/JWT-RESEARCH.md', 'not-required', NULL, 0),

('S009', 'Add logging middleware', 'Log method + path for all requests.', 'dev', 'medium', 1,
 (SELECT id FROM workflows WHERE project_id = 1 AND code = 'development'),
 (SELECT ws.id FROM workflow_stages ws JOIN workflows w ON ws.workflow_id = w.id WHERE w.project_id = 1 AND w.code = 'development' AND ws.stage_name = 'Done'),
 (SELECT id FROM agents WHERE name = 'owner'), '2026-03-02 11:00:00', '2026-03-03 09:00:00', 0, NULL, NULL, NULL,
 NULL, NULL, 'Uses winston logger.', 'dev/S009-logging', 'PR#17', NULL, 'approved', 'Looks good.', 0),

('S010', 'Create epic roadmap draft', 'Draft roadmap for next 3 months.', 'manual', 'critical', 1,
 (SELECT id FROM workflows WHERE project_id = 1 AND code = 'manual'),
 (SELECT ws.id FROM workflow_stages ws JOIN workflows w ON ws.workflow_id = w.id WHERE w.project_id = 1 AND w.code = 'manual' AND ws.stage_name = 'Todo'),
 (SELECT id FROM agents WHERE name = 'owner'), '2026-03-03 08:00:00', '2026-03-03 08:00:00', 0, NULL, NULL, NULL,
 NULL, 'Define 3 milestone themes; Align with business goals', NULL, NULL, NULL, NULL, 'not-required', NULL, 0),

('S011', 'Unit test health endpoint', 'Add isolated unit tests for /health route and response shape.', 'dev', 'medium', 1,
 (SELECT id FROM workflows WHERE project_id = 1 AND code = 'development'),
 (SELECT ws.id FROM workflow_stages ws JOIN workflows w ON ws.workflow_id = w.id WHERE w.project_id = 1 AND w.code = 'development' AND ws.stage_name = 'Test Planning'),
 (SELECT id FROM agents WHERE name = 'owner'), '2026-03-03 10:00:00', '2026-03-03 11:00:00', 0, NULL, NULL, NULL,
 (SELECT id FROM agents WHERE name = 'test-ava'), NULL, NULL, 'test/S011-health-unit', NULL, NULL, 'not-required', NULL, 0),

('S012', 'Unit tests for config loader', 'Isolated tests for config loader module.', 'dev', 'medium', 1,
 (SELECT id FROM workflows WHERE project_id = 1 AND code = 'development'),
 (SELECT ws.id FROM workflow_stages ws JOIN workflows w ON ws.workflow_id = w.id WHERE w.project_id = 1 AND w.code = 'development' AND ws.stage_name = 'Test In Progress'),
 (SELECT id FROM agents WHERE name = 'owner'), '2026-03-03 10:05:00', '2026-03-03 12:00:00', 0, NULL, NULL, NULL,
 (SELECT id FROM agents WHERE name = 'test-ian'), NULL, NULL, 'test/S012-config-unit', NULL, NULL, 'not-required', NULL, 0),

('S013', 'Review unit tests for config loader', 'Owner review of S012 unit test PR before merge.', 'dev', 'medium', 1,
 (SELECT id FROM workflows WHERE project_id = 1 AND code = 'development'),
 (SELECT ws.id FROM workflow_stages ws JOIN workflows w ON ws.workflow_id = w.id WHERE w.project_id = 1 AND w.code = 'development' AND ws.stage_name = 'Test Review'),
 (SELECT id FROM agents WHERE name = 'owner'), '2026-03-03 12:30:00', '2026-03-03 13:00:00', 0, NULL, NULL, NULL,
 (SELECT id FROM agents WHERE name = 'owner'), 'Coverage and style approved', NULL, 'test/S012-config-unit', 'PR#22', NULL, 'pending', NULL, 0),

('S014', 'Integration test plan for health + ready', 'Plan integration tests for health and readiness endpoints together.', 'dev', 'medium', 1,
 (SELECT id FROM workflows WHERE project_id = 1 AND code = 'development'),
 (SELECT ws.id FROM workflow_stages ws JOIN workflows w ON ws.workflow_id = w.id WHERE w.project_id = 1 AND w.code = 'development' AND ws.stage_name = 'Int Test Planning'),
 (SELECT id FROM agents WHERE name = 'owner'), '2026-03-03 11:00:00', '2026-03-03 14:00:00', 0, NULL, NULL, NULL,
 (SELECT id FROM agents WHERE name = 'int-dana'), NULL, NULL, NULL, NULL, NULL, 'not-required', NULL, 0),

('S015', 'Integration tests for health + ready', 'Implement integration tests for health and readiness in harness.', 'dev', 'medium', 1,
 (SELECT id FROM workflows WHERE project_id = 1 AND code = 'development'),
 (SELECT ws.id FROM workflow_stages ws JOIN workflows w ON ws.workflow_id = w.id WHERE w.project_id = 1 AND w.code = 'development' AND ws.stage_name = 'Int Test In Progress'),
 (SELECT id FROM agents WHERE name = 'owner'), '2026-03-03 11:10:00', '2026-03-03 15:00:00', 0, NULL, NULL, NULL,
 (SELECT id FROM agents WHERE name = 'int-marc'), NULL, NULL, 'int/S015-health-ready', NULL, NULL, 'not-required', NULL, 0),

('S016', 'Review integration tests (health + ready)', 'Owner review of integration test PR for health and ready.', 'dev', 'medium', 1,
 (SELECT id FROM workflows WHERE project_id = 1 AND code = 'development'),
 (SELECT ws.id FROM workflow_stages ws JOIN workflows w ON ws.workflow_id = w.id WHERE w.project_id = 1 AND w.code = 'development' AND ws.stage_name = 'Int Test Review'),
 (SELECT id FROM agents WHERE name = 'owner'), '2026-03-03 15:30:00', '2026-03-03 16:00:00', 0, NULL, NULL, NULL,
 (SELECT id FROM agents WHERE name = 'owner'), 'Harness usage and coverage approved', NULL, 'int/S015-health-ready', 'PR#23', NULL, 'pending', NULL, 0),

('S017', 'Load test config loader', 'Validate config loader performance under load.', 'performance', 'low', 1,
 (SELECT id FROM workflows WHERE project_id = 1 AND code = 'performance'),
 (SELECT ws.id FROM workflow_stages ws JOIN workflows w ON ws.workflow_id = w.id WHERE w.project_id = 1 AND w.code = 'performance' AND ws.stage_name = 'Todo'),
 (SELECT id FROM agents WHERE name = 'owner'), '2026-03-03 09:00:00', '2026-03-03 09:00:00', 0, NULL, NULL, NULL,
 NULL, 'No regression under 1k RPS', NULL, NULL, NULL, NULL, 'not-required', NULL, 0),

('S018', 'Plan load test for /ready', 'Define load and latency targets for readiness endpoint.', 'performance', 'medium', 1,
 (SELECT id FROM workflows WHERE project_id = 1 AND code = 'performance'),
 (SELECT ws.id FROM workflow_stages ws JOIN workflows w ON ws.workflow_id = w.id WHERE w.project_id = 1 AND w.code = 'performance' AND ws.stage_name = 'Planning'),
 (SELECT id FROM agents WHERE name = 'owner'), '2026-03-03 09:30:00', '2026-03-03 10:00:00', 0, NULL, NULL, NULL,
 (SELECT id FROM agents WHERE name = 'perf-maya'), 'RPS and p99 targets documented', NULL, NULL, NULL, NULL, 'not-required', NULL, 0),

('S019', 'Review health endpoint load test', 'Owner approval of S006 load test results and thresholds.', 'performance', 'medium', 1,
 (SELECT id FROM workflows WHERE project_id = 1 AND code = 'performance'),
 (SELECT ws.id FROM workflow_stages ws JOIN workflows w ON ws.workflow_id = w.id WHERE w.project_id = 1 AND w.code = 'performance' AND ws.stage_name = 'Review'),
 (SELECT id FROM agents WHERE name = 'owner'), '2026-03-03 14:00:00', '2026-03-03 14:30:00', 0, NULL, NULL, NULL,
 (SELECT id FROM agents WHERE name = 'owner'), '10k RPS and latency accepted', NULL, NULL, NULL, 'performance/report-S006.json', 'pending', NULL, 0),

('S020', 'Load test readiness endpoint', 'Load test /ready; completed and approved.', 'performance', 'low', 1,
 (SELECT id FROM workflows WHERE project_id = 1 AND code = 'performance'),
 (SELECT ws.id FROM workflow_stages ws JOIN workflows w ON ws.workflow_id = w.id WHERE w.project_id = 1 AND w.code = 'performance' AND ws.stage_name = 'Done'),
 (SELECT id FROM agents WHERE name = 'owner'), '2026-03-02 16:00:00', '2026-03-03 11:00:00', 0, NULL, NULL, NULL,
 (SELECT id FROM agents WHERE name = 'owner'), '5k RPS sustained; Report approved', 'k6 run completed.', NULL, NULL, 'performance/report-S020.json', 'approved', 'Accepted.', 0),

('S021', 'Production deployment pipeline', 'Add GitHub Actions workflow for production with approvals.', 'infrastructure', 'high', 1,
 (SELECT id FROM workflows WHERE project_id = 1 AND code = 'devops'),
 (SELECT ws.id FROM workflow_stages ws JOIN workflows w ON ws.workflow_id = w.id WHERE w.project_id = 1 AND w.code = 'devops' AND ws.stage_name = 'Todo'),
 (SELECT id FROM agents WHERE name = 'owner'), '2026-03-03 08:30:00', '2026-03-03 08:30:00', 0, NULL, NULL, NULL,
 NULL, 'Manual approval gate; Rollback documented', NULL, NULL, NULL, NULL, 'not-required', NULL, 0),

('S022', 'Plan production deployment', 'Design production deploy steps and approval flow.', 'infrastructure', 'high', 1,
 (SELECT id FROM workflows WHERE project_id = 1 AND code = 'devops'),
 (SELECT ws.id FROM workflow_stages ws JOIN workflows w ON ws.workflow_id = w.id WHERE w.project_id = 1 AND w.code = 'devops' AND ws.stage_name = 'Planning'),
 (SELECT id FROM agents WHERE name = 'owner'), '2026-03-03 09:00:00', '2026-03-03 09:30:00', 0, NULL, NULL, NULL,
 (SELECT id FROM agents WHERE name = 'devops-sam'), 'Steps and rollback documented', NULL, NULL, NULL, NULL, 'not-required', NULL, 0),

('S023', 'Implement production deploy workflow', 'Add production workflow YAML and approval job.', 'infrastructure', 'high', 1,
 (SELECT id FROM workflows WHERE project_id = 1 AND code = 'devops'),
 (SELECT ws.id FROM workflow_stages ws JOIN workflows w ON ws.workflow_id = w.id WHERE w.project_id = 1 AND w.code = 'devops' AND ws.stage_name = 'In Progress'),
 (SELECT id FROM agents WHERE name = 'owner'), '2026-03-02 17:00:00', '2026-03-03 10:00:00', 0, NULL, NULL, NULL,
 (SELECT id FROM agents WHERE name = 'devops-raj'), 'Workflow runs on tag; Approval required', NULL, 'devops/S023-production', NULL, NULL, 'not-required', NULL, 0),

('S024', 'Staging pipeline completed', 'Staging deployment pipeline implemented and approved (historical).', 'infrastructure', 'high', 1,
 (SELECT id FROM workflows WHERE project_id = 1 AND code = 'devops'),
 (SELECT ws.id FROM workflow_stages ws JOIN workflows w ON ws.workflow_id = w.id WHERE w.project_id = 1 AND w.code = 'devops' AND ws.stage_name = 'Done'),
 (SELECT id FROM agents WHERE name = 'owner'), '2026-03-01 14:00:00', '2026-03-02 16:00:00', 0, NULL, NULL, NULL,
 (SELECT id FROM agents WHERE name = 'owner'), 'Auto deploy on main; Secrets secured', 'Completed by devops-sam.', 'devops/S024-staging', 'PR#20', NULL, 'approved', 'Merged.', 0),

('S025', 'Plan Q2 roadmap', 'Outline Q2 themes and milestones for stakeholder review.', 'manual', 'high', 1,
 (SELECT id FROM workflows WHERE project_id = 1 AND code = 'manual'),
 (SELECT ws.id FROM workflow_stages ws JOIN workflows w ON ws.workflow_id = w.id WHERE w.project_id = 1 AND w.code = 'manual' AND ws.stage_name = 'Planning'),
 (SELECT id FROM agents WHERE name = 'owner'), '2026-03-03 08:00:00', '2026-03-03 08:30:00', 0, NULL, NULL, NULL,
 NULL, 'Three themes with dates; Aligned with business', NULL, NULL, NULL, NULL, 'not-required', NULL, 0),

('S026', 'Document API decisions', 'Record API versioning and error-format decisions; completed.', 'docs', 'medium', 1,
 (SELECT id FROM workflows WHERE project_id = 1 AND code = 'manual'),
 (SELECT ws.id FROM workflow_stages ws JOIN workflows w ON ws.workflow_id = w.id WHERE w.project_id = 1 AND w.code = 'manual' AND ws.stage_name = 'Done'),
 (SELECT id FROM agents WHERE name = 'owner'), '2026-03-02 11:00:00', '2026-03-03 09:00:00', 0, NULL, NULL, NULL,
 (SELECT id FROM agents WHERE name = 'owner'), 'ADR or doc updated; Decisions traceable', 'docs/API-DECISIONS.md.', NULL, NULL, 'docs/API-DECISIONS.md', 'approved', NULL, 0),

('S027', 'Triage support backlog', 'Review and prioritise open support items for next sprint.', 'manual', 'low', 1,
 (SELECT id FROM workflows WHERE project_id = 1 AND code = 'manual'),
 (SELECT ws.id FROM workflow_stages ws JOIN workflows w ON ws.workflow_id = w.id WHERE w.project_id = 1 AND w.code = 'manual' AND ws.stage_name = 'Todo'),
 (SELECT id FROM agents WHERE name = 'owner'), '2026-03-03 12:00:00', '2026-03-03 12:00:00', 0, NULL, NULL, NULL,
 (SELECT id FROM agents WHERE name = 'owner'), 'Items labelled and prioritised; Blockers called out', NULL, NULL, NULL, NULL, 'not-required', NULL, 0);
