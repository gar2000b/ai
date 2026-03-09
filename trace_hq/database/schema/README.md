# Database schema (Trace HQ)

SQL DDL for the Open Workflows / user-stories model. **Do not run these against a live DB until reviewed.** Apply in numerical order (dependencies are ordered).

## ER diagram

**[schema-er.mmd](schema-er.mmd)** — Entity-Relationship diagram of all tables and foreign-key relationships. Open in any Mermaid-supported viewer (e.g. GitHub, VS Code with a Mermaid extension, or [mermaid.live](https://mermaid.live)) to see how projects, workflows, stages, roles, agents, stories, dependencies, and audit tables connect.

## DDL files

| File | Contents |
|------|----------|
| `01_projects.sql` | `projects` — workflow project container |
| `02_workflows.sql` | `workflows`, `workflow_stages` — pipelines and stage definitions |
| `02a_workflow_display_order.sql` | **Migration:** add `display_order` to `workflows` for board order (run after 02; requires MySQL 8.0+) |
| `03_roles_agents.sql` | `roles`, `agents` — system roles and assignable agents |
| `04_stories.sql` | `stories` — user stories (single source of truth; includes `project_id` for backlog, `backlog_order` for backlog order) |
| `04a_stories_project_id.sql` | **Migration:** add `project_id` to existing `stories` table (run only on DBs created before `project_id` was added) |
| `04c_backlog_order.sql` | **Migration:** add `backlog_order` to `stories` for user-defined backlog ordering (run only on DBs created before `backlog_order` was added) |
| `04d_global_backlog.sql` | **Migration:** global backlog — `project_id` nullable; backlog = stories with `workflow_id` IS NULL (no project); reorder index on `(workflow_id, backlog_order)` |
| `05_story_dependencies.sql` | `story_dependencies`, `story_related` — dependencies and related-story links |
| `06_story_history_audit.sql` | `story_stage_history`, `story_audit_log` — append-only history and audit |
| `07_stories_deleted_at.sql` | **Migration:** add `deleted_at` to `stories` for logical (soft) delete |

**Source:** Requirements in `markdown/requirements/foundational/` (OPEN-WORKFLOWS-PROJECT.md, USER-STORY.md, USER-STORIES.md). Connection details: `markdown/database/DATABASE.md`.

**Note:** No seed data is included; tables are structure-only.
