# Backlog — Implementation plan

This document outlines how to add a **Backlog** to TRACE H.Q.: a project-scoped list of user stories that are not yet assigned to any workflow. It covers database changes, API, UI, and doc updates. Review this plan before implementation.

---

## 1. Definition

- **Backlog** = the set of user stories that **belong to a project** but are **not assigned to any workflow** (i.e. not on any board column).
- A story is either:
  - **In a workflow** → it appears on the project board in one of that workflow’s stages, or
  - **In the project’s backlog** → it appears only in the Backlog view for that project.
- So every story must be associated with a **project**. Today stories are linked only via `workflow_id` (workflow → project); we need a direct **story → project** link so that when `workflow_id` is NULL we still know which project’s backlog the story is in.

---

## 2. Database changes

### 2.1 Add `project_id` to `stories`

- **Change:** Add column `project_id` to `stories`.
  - Type: `INT UNSIGNED NOT NULL`.
  - FK to `projects(id)` (e.g. `ON DELETE RESTRICT`, `ON UPDATE CASCADE`).
  - Comment: “Project this story belongs to; determines which backlog it appears in when workflow_id is NULL.”
- **Why:** Today we can’t answer “which project is this story in?” when `workflow_id` is NULL. With `project_id`, “backlog for project X” is simply: `WHERE project_id = X AND workflow_id IS NULL`.
- **Invariant:** When `workflow_id` is set, the app should ensure `workflow.project_id = story.project_id` (enforce in API or with a CHECK/trigger). So a story’s project never changes when we move it between workflow and backlog.

### 2.2 Migration path

1. **Add column as nullable first** (so existing rows don’t break):
   - `ALTER TABLE stories ADD COLUMN project_id INT UNSIGNED NULL COMMENT '...' AFTER priority;`
   - `ALTER TABLE stories ADD CONSTRAINT fk_stories_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE RESTRICT ON UPDATE CASCADE;`
2. **Backfill** existing stories from their workflow’s project:
   - `UPDATE stories s JOIN workflows w ON s.workflow_id = w.id SET s.project_id = w.project_id WHERE s.workflow_id IS NOT NULL;`
3. **Handle any rows with NULL project_id** (e.g. stories that already had `workflow_id` NULL): assign to a default project (e.g. project id 1) or leave as NULL and fix manually. If all current seed data has `workflow_id` set, this step is empty.
4. **Make column NOT NULL** and add index:
   - `UPDATE stories SET project_id = 1 WHERE project_id IS NULL;` (or chosen default)
   - `ALTER TABLE stories MODIFY project_id INT UNSIGNED NOT NULL;`
   - `CREATE INDEX ix_stories_project_id ON stories(project_id);`
5. **Optional:** Add a CHECK or application-level check that when `workflow_id` is set, `(SELECT project_id FROM workflows WHERE id = stories.workflow_id) = stories.project_id`.

### 2.3 Schema file

- Add a **new DDL file** (e.g. `database/schema/04a_stories_project_id.sql` or a dedicated migration) that adds `project_id` and the index, so future clean installs have it. Document in `database/schema/README.md`. For an existing DB, run the migration steps above (or a single migration script) instead of re-creating tables.

### 2.4 Seed data

- **`database/data/04_stories.sql`** (and any other story inserts): include `project_id` in `INSERT` (e.g. project 1 for mep-sentinel). New stories created via app will set `project_id` when added to backlog or to a workflow.
- Optionally add **one or two backlog stories** in a new seed file (e.g. `04b_backlog_stories.sql`): stories with `project_id = 1` and `workflow_id` / `workflow_stage_id` NULL so the Backlog view has something to show.

### 2.5 ER diagram

- Update `database/schema/schema-er.mmd`: add `project_id` to `stories` and the relationship `projects ||--o{ stories : "has"`.

---

## 3. API

### 3.1 GET /api/projects/:projectId/backlog

- **Returns:** List of stories for that project where `workflow_id IS NULL` (and `project_id = projectId` once we have the column).
- **Shape:** Same as board stories where useful: id, title, description, type, priority, project_id, assignee_id, assignee_name, blocked, blocked_reason, dependencies, related, created_at, etc. No workflow/stage fields (they’re null). Order by id or created_at (left-to-right, top-to-bottom in UI).
- **Auth:** Same as rest of app (no login; acting user is owner).

### 3.2 PATCH /api/stories/:storyId/workflow (add to workflow)

- **Purpose:** Move a story **from backlog into a workflow** (and thus onto the board).
- **Body:** `{ "workflow_id": <id>, "workflow_stage_id": <id> }`. Typically set `workflow_stage_id` to the chosen workflow’s first stage (e.g. Todo).
- **Rules:**
  - Story must exist and be in backlog (`workflow_id` currently NULL) or we allow reassigning workflow (owner can do anything).
  - Workflow must belong to the same project as the story (`workflow.project_id = story.project_id`).
  - Optionally clear assignee when adding to workflow (or leave as is).
- **On success:** Update `stories.workflow_id`, `stories.workflow_stage_id`; insert into `story_stage_history` and optionally `story_audit_log`. Return updated story.
- **Use in UI:** “Add to workflow” dropdown on a backlog card: pick workflow (and optionally stage); call this PATCH; then refresh backlog and/or navigate to Workflows.

### 3.3 Optional: Move story to backlog (from board)

- **PATCH /api/stories/:storyId/workflow** with body `{ "workflow_id": null, "workflow_stage_id": null }` (or a dedicated “move to backlog” endpoint).
- **Rules:** Only owner (current user). Update story; record in history/audit.
- **Use in UI:** On the board, e.g. “Move to backlog” in the card’s move dropdown or a separate action. Can be a follow-up if not in first cut.

---

## 4. Frontend (main application)

### 4.1 Side menu

- **Order:** Home → **Workflows** → **Backlog** → Settings.
- Add a nav item: “Backlog” with `href="#/backlog"` and `data-route="backlog"`.

### 4.2 Backlog view

- **Route:** `#/backlog`.
- **Layout:**
  - **Header:** Same as Workflows: title “Backlog” (or “Project backlog”) on the left, **project dropdown on the top right**. Reuse the same “last viewed project” / project selector pattern so the backlog is always for the selected project.
  - **Content:** A single scrollable area showing **all backlog stories** for the selected project in a **grid**: left to right, top to bottom (e.g. CSS Grid or flex-wrap). Each item is a **backlog story card** (similar look to board cards but without a “stage” or “Move to [stage]” — instead “Add to workflow”).
- **Empty state:** If there are no backlog stories, show a short message (e.g. “No stories in backlog for this project.”).

### 4.3 Backlog story card

- **Show:** Story id, title, type, priority, assignee (or “Unassigned”), blocked/dependencies if any.
- **Action:** “Add to workflow” control (e.g. dropdown or button that opens a small menu): list the **project’s workflows** (Development, Performance Testing, DevOps, Manual, etc.). On choose: call **PATCH /api/stories/:storyId/workflow** with that workflow’s id and its first stage id (Todo). Then refresh the backlog list (and optionally show a toast “Story added to [workflow name]”). If we implement “move to backlog” from the board, the same card component could be reused there with a different action.

### 4.4 Routing and data

- **app.js:** On `#/backlog`, show the Backlog view; load project list and set project dropdown; load backlog for selected project (GET /api/projects/:projectId/backlog); render grid. When project changes, reload backlog for that project. Reuse last-viewed project from localStorage so switching between Workflows and Backlog keeps the same project.
- **New JS (e.g. `public/js/backlog.js`):** Functions to fetch backlog, render the grid, render a backlog card, and handle “Add to workflow”. Optionally share card styling with the board (e.g. same CSS class for card, different inner actions).

### 4.5 Styles

- Reuse existing card and layout styles where possible. Add a `.backlog-grid` (or similar) for the left-to-right, top-to-bottom layout; ensure it’s responsive (e.g. min-width on cards, wrap).

---

## 5. Requirements and plan docs

### 5.1 REQUIREMENTS.md (app)

- Add a **“Backlog”** subsection under “Workflows UI and project board” (or a new “Backlog” section):
  - Backlog is a **project-scoped** list of user stories that are **not assigned to any workflow**.
  - Side menu includes **Backlog** (between Workflows and Settings).
  - Backlog view shows the **selected project’s** backlog; project selector in the top right (same as Workflows).
  - Stories are displayed **left to right, top to bottom** in a grid of cards.
  - Each backlog story card has an **“Add to workflow”** action to move the story into a chosen workflow (and onto the board).
  - Data model: stories have a **project**; when `workflow_id` is NULL, the story appears only in that project’s backlog.

### 5.2 IMPLEMENTATION-PLAN.md (main plan)

- Add a short **“Backlog”** section (or reference this document): link to this BACKLOG-IMPLEMENTATION-PLAN.md and summarise: database (project_id on stories), API (GET backlog, PATCH add to workflow), UI (menu item, backlog view, project selector, grid of cards, Add to workflow).

### 5.3 USER-STORY.md (foundational)

- Clarify that a story **belongs to a project** and is either **in a workflow** (on the board) or **in the project’s backlog** (workflow “-” / not assigned). No need to change the rest of the artifact.

### 5.4 DATABASE.md

- Mention the new `stories.project_id` column and that backlog = stories for a project with no workflow. Update any table list if present.

---

## 6. Implementation order (summary)

1. **Database:** Add `project_id` to `stories` (migration + backfill); update schema file and ER diagram; update seed data; optionally add a couple of backlog story rows.
2. **API:** Implement GET /api/projects/:projectId/backlog and PATCH /api/stories/:storyId/workflow (add to workflow); ensure project_id is set when moving to workflow and validated.
3. **Frontend:** Add Backlog to the side menu; add #/backlog view with header and project dropdown; implement backlog grid and backlog story card with “Add to workflow”; wire routing and last-viewed project.
4. **Docs:** Update REQUIREMENTS.md, IMPLEMENTATION-PLAN.md, USER-STORY.md, DATABASE.md, and schema README as above.
5. **Optional follow-up:** “Move to backlog” from the board (PATCH story to set workflow_id/workflow_stage_id to NULL); show “Move to backlog” in board card actions.

---

## 7. Out of scope for this plan

- **Creating new stories** from the UI (no “Create story” in backlog yet).
- **Filtering/search** within backlog (can be added later).

---

## 8. Backlog reordering (implemented)

Users can **reorder backlog stories** via **drag-and-drop** in the Backlog view. Order is persisted in **`stories.backlog_order`** and returned by GET backlog in that order.

- **Database:** Column **`backlog_order`** INT UNSIGNED NULL on `stories` (only used when `workflow_id` IS NULL). Migration: **`database/schema/04c_backlog_order.sql`**. New installs: **`04_stories.sql`** includes the column.
- **API:** **PUT /api/projects/:projectId/backlog/order** — body `{ "orderedStoryIds": ["S028", "S030", "S029", ...] }`. Updates `backlog_order` for each story (0, 10, 20, …). All ids must be in that project’s backlog.
- **UI:** Backlog cards are draggable; drop on another card to reorder. On drop, the app sends the new order to the API and refreshes the backlog.
- **Docs:** REQUIREMENTS.md (Backlog), DATABASE.md, IMPLEMENTATION-PLAN.md, this file.
