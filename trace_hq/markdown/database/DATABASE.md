# Database connection (Trace HQ)

This document describes how to connect to the Trace HQ MySQL database so agents and tooling can interact with it.

## Connection details

- **Engine:** MySQL
- **Database name:** `trace-hq`
- **User:** Stored in `.env` as `DB_USER` (e.g. `trace-hq-admin`)
- **Password:** Stored in `.env` as `DB_PASSWORD` (never commit real values)
- **Host:** Stored in `.env` as `DB_HOST` (default `localhost`)

Sensitive values live in the project root `.env` file: `trace_hq/.env`. **Never put the password on the command line** (process lists can expose it). Use the script or a config file instead.

## CLI connection (secure)

From the `trace_hq` directory, use the helper script so credentials are read from `.env` and the password is passed via a temporary config file (never on the command line):

```bash
# From trace_hq directory
./scripts/mysql.sh -e "SHOW DATABASES;"
./scripts/mysql.sh   # interactive session
```

The script loads `trace_hq/.env`, writes a temporary MySQL `[client]` config file with `user`, `password`, and `host`, runs `mysql --defaults-extra-file=...`, then deletes the temp file. The password never appears in process listings.

## For agents / tooling

1. **Read credentials from:** `trace_hq/.env` (or the repo root if your runner loads it from there).
2. **Variables to use:**
   - `DB_HOST` – MySQL host (e.g. `localhost`)
   - `DB_USER` – MySQL user (e.g. `trace-hq-admin`)
   - `DB_PASSWORD` – MySQL password
   - `DB_NAME` – Database name (e.g. `trace-hq`)
3. **Connection string (typical):**  
   `mysql://${DB_USER}:${DB_PASSWORD}@${DB_HOST}/${DB_NAME}`  
   (Use this in code only; never log it or pass the password on a shell command line.)
4. **Security:** Never log or expose `DB_PASSWORD`; never commit real credentials; keep `.env` in `.gitignore`. For CLI, use `scripts/mysql.sh` so the password is not on the command line.

## Schema

Table definitions (DDL only, no data) live in **`trace_hq/database/schema/`** as numbered `.sql` files. Apply in order (`01_projects.sql` through `06_story_history_audit.sql`). See `database/schema/README.md` for a short index and requirements references. An **Entity-Relationship diagram** of all tables is in `database/schema/schema-er.mmd` (Mermaid); open it in a Mermaid viewer to see table relationships. Do not execute against the database until schemas have been reviewed.

- **Backlog support:** The `stories` table includes **`project_id`** (FK to `projects`, **nullable**). When a story is in a workflow, `project_id` is set to that workflow’s project. When `workflow_id` is NULL, the story is in the **global backlog** and `project_id` is NULL. If your database was created before `project_id` was added, run **`database/schema/04a_stories_project_id.sql`** once.
- **Global backlog:** Run **`database/schema/04d_global_backlog.sql`** to make `project_id` nullable and treat backlog as global (no project filter). Existing backlog stories get `project_id` set to NULL.
- **Backlog ordering:** The `stories` table includes **`backlog_order`** (INT UNSIGNED NULL). When a story is in the backlog (`workflow_id` IS NULL), this field controls display order (lower = earlier). Migration **`database/schema/04c_backlog_order.sql`** adds it if missing.

## Populating the schema

Data scripts live in **`trace_hq/database/data/`**. Run each script from the **`trace_hq`** directory. Apply steps in order when they depend on each other (e.g. project before workflows).

```bash
# From trace_hq directory — run a data script
./scripts/mysql.sh < database/data/01_project_mep_sentinel.sql
```

| Step | Description | Script |
|------|-------------|--------|
| 1 | Add a project | [database/data/01_project_mep_sentinel.sql](../../database/data/01_project_mep_sentinel.sql) — inserts project `mep-sentinel` |
| 2 | Add workflows and stages | [database/data/02_workflows_and_stages.sql](../../database/data/02_workflows_and_stages.sql) — inserts Development, Performance Testing, DevOps, Manual + their stages |
| 3 | Add roles and agents | [database/data/03_roles_and_agents.sql](../../database/data/03_roles_and_agents.sql) — inserts 6 agent types (roles) + one agent per role |
| 4 | Add user stories | [database/data/04_stories.sql](../../database/data/04_stories.sql) — inserts stories S001–S027 (requires 01, 02, 03) |
| 5 | Add story dependencies | [database/data/05_story_dependencies.sql](../../database/data/05_story_dependencies.sql) — blocking dependencies (requires 04) |
| 6 | Add story related links | [database/data/06_story_related.sql](../../database/data/06_story_related.sql) — non-blocking related-story links (requires 04) |
| 7 | Add story stage history | [database/data/07_story_stage_history.sql](../../database/data/07_story_stage_history.sql) — stage transitions from USER-STORIES.md (requires 04) |
| 8 | Add backlog stories (optional) | [database/data/04b_backlog_stories.sql](../../database/data/04b_backlog_stories.sql) — inserts S028–S042 (backlog-only; requires 04 and, if applicable, 04a migration) |

Steps 5, 6, and 7 can be run in any order after step 4. Step 8 adds sample backlog stories; run after step 4 (and after running `04a_stories_project_id.sql` if your schema predates `project_id`). **story_audit_log** is not seeded; the running system populates it.

## Quick reference

| Item        | Env var     | Example        |
|------------|-------------|----------------|
| Host       | `DB_HOST`   | `localhost`    |
| User       | `DB_USER`   | `trace-hq-admin` |
| Password   | `DB_PASSWORD` | (from .env)  |
| Database   | `DB_NAME`   | `trace-hq`     |
