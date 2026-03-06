# Database schema (Trace HQ)

SQL DDL for the Open Workflows / user-stories model. **Do not run these against a live DB until reviewed.** Apply in numerical order (dependencies are ordered).

| File | Contents |
|------|----------|
| `01_projects.sql` | `projects` — workflow project container |
| `02_workflows.sql` | `workflows`, `workflow_stages` — pipelines and stage definitions |
| `03_roles_agents.sql` | `roles`, `agents` — system roles and assignable agents |
| `04_stories.sql` | `stories` — user stories (single source of truth) |
| `05_story_dependencies.sql` | `story_dependencies`, `story_related` — dependencies and related-story links |
| `06_story_history_audit.sql` | `story_stage_history`, `story_audit_log` — append-only history and audit |

**Source:** Requirements in `markdown/requirements/` (OPEN-WORKFLOWS-PROJECT.md, USER-STORY.md, USER-STORIES.md). Connection details: `markdown/database/DATABASE.md`.

**Note:** No seed data is included; tables are structure-only.
