# TRACE H.Q. — Implementation Plan (first draft)

This plan implements the first draft of the TRACE H.Q. application as specified in the [Application Requirements](../requirements/app/REQUIREMENTS.md). It is designed to be **executable**: an agent or developer can work through the phases and steps below to build the project.

**Project root:** All paths are relative to **`trace_hq/`** unless stated otherwise.

---

## 1. References (source of truth)

When implementing, use these as authoritative references:

| Area | Location |
|------|----------|
| **App requirements** | [markdown/requirements/app/REQUIREMENTS.md](../requirements/app/REQUIREMENTS.md) |
| **Foundational requirements** | [markdown/requirements/foundational/](../requirements/foundational/) — OPEN-WORKFLOWS-PROJECT.md, USER-STORY.md, USER-STORIES.md |
| **Database** | [markdown/database/DATABASE.md](../database/DATABASE.md) — connection, credentials, schema and data scripts |
| **Schema DDL** | `database/schema/` — 01_projects.sql through 06_story_history_audit.sql |
| **Schema ER diagram** | `database/schema/schema-er.mmd` |
| **Seed data** | `database/data/` — 01 through 07 (apply in order after schema) |

---

## 2. Product and scope summary

- **Product:** TRACE H.Q. — SaaS-like web app for the Workflows (multi-agent kanban) system.
- **First capability:** Workflows UI — project board showing all workflows for a project, stacked on one scrollable page; stories as cards; move cards between stages with correct transition rules.
- **Tech:** Backend Node.js; frontend vanilla JavaScript (no framework); MySQL via `trace_hq/.env` (DB_HOST, DB_USER, DB_PASSWORD, DB_NAME).
- **Current user:** No login; the single user is treated as the **owner** for all permission checks and audit.
- **Out of scope (this phase):** Login, agentic integration, mobile apps, offline, real-time collaboration, reporting, notifications, public API.

---

## 3. Target directory structure

After implementation, the project root `trace_hq/` should contain at least:

```
trace_hq/
├── .env                    # DB credentials (existing; never commit real values)
├── package.json            # Node app + scripts
├── server.js               # or index.js — entry point: start server, mount API + static
├── config/
│   └── db.js               # load .env, create MySQL pool (no password on CLI/logs)
├── api/                    # Backend API routes
│   └── (see Phase 4)
├── public/                 # Static assets served by backend
│   ├── index.html          # Shell / single-page entry
│   ├── css/
│   │   └── app.css         # Global + theme variables (light/dark/medium)
│   ├── js/
│   │   ├── app.js          # Init, routing, theme apply
│   │   ├── board.js        # Project board: fetch workflows/stories, render sections
│   │   ├── story-card.js   # Story card UI, move actions
│   │   └── settings.js     # Settings UI, theme picker, persistence
│   └── (images if needed)
├── database/
│   ├── schema/             # (existing) DDL
│   └── data/               # (existing) seed scripts
├── markdown/               # (existing) requirements, plan, database docs
└── scripts/
    └── mysql.sh            # (existing) CLI DB access
```

Exact file names can vary (e.g. `src/` instead of `api/`, or multiple route files); the important points are: (1) one Node entry point, (2) DB config from `.env`, (3) API under one mount (e.g. `/api`), (4) static files served from one directory (e.g. `public/`).

---

## 4. Phase 1 — Project and backend bootstrap

### 1.1 Initialize Node project

- Create `trace_hq/package.json` with:
  - `"main"` or start script pointing to the server entry (e.g. `server.js`).
  - Dependencies: `express`, `mysql2` (or equivalent MySQL client).
  - Scripts: `"start": "node server.js"`, optionally `"dev": "node server.js"` or with a watcher.
- Ensure `.env` is in `.gitignore` (and that real credentials are never committed).

### 1.2 Database connection

- Create `config/db.js` (or equivalent):
  - Load `DB_HOST`, `DB_USER`, `DB_PASSWORD`, `DB_NAME` from `process.env` (e.g. via `dotenv` from `.env` in project root).
  - Create and export a MySQL connection pool. Do not log or expose the password; do not pass it on the command line.
  - Use the connection only for server-side DB access. See [markdown/database/DATABASE.md](../database/DATABASE.md) for connection details.

### 1.3 Server entry point

- Create `server.js` (or `index.js`):
  - Use Express (or minimal Node HTTP + router).
  - Mount API routes under a base path (e.g. `/api`).
  - Serve static files from `public/` (or chosen folder) for the app UI (HTML, CSS, JS, images). No separate static host; the Node app serves both API and static content.
  - **DB warmup:** Before calling `listen`, warm up the database (e.g. run a simple query such as `SELECT 1` with retries and a short delay between attempts). Only then listen on a configurable port (e.g. `PORT` from env, default 3000). This avoids "Failed to load stories" on the first request when MySQL is slow to start. The connection pool (in `config/db.js`) should use a `connectTimeout` (e.g. 15s) for slow MySQL startup.
  - Health check: e.g. `GET /api/health` returning 200 and a simple payload so the app can be verified without the DB.

---

## 5. Phase 2 — Database and data readiness

- **Assumption:** Schema and seed data are already applied (as per DATABASE.md). If not:
  - Apply DDL in order: `database/schema/01_projects.sql` … `06_story_history_audit.sql`.
  - Run seed scripts in order: `database/data/01_project_mep_sentinel.sql` through `07_story_stage_history.sql` (using `scripts/mysql.sh` from `trace_hq/`).
- The implementation plan does not change the schema; it consumes the existing tables (projects, workflows, workflow_stages, roles, agents, stories, story_dependencies, story_related, story_stage_history, story_audit_log).

---

## 6. Phase 3 — API design and data access

All API responses should be JSON. Use consistent error responses (e.g. 4xx/5xx with a message or code).

### 3.1 Projects

- **GET /api/projects**
  - Return list of projects (id, name, created_at, updated_at) from `projects` table.
  - Order by id or name as appropriate.

### 3.2 Workflows and stages (per project)

- **GET /api/projects/:projectId/workflows**
  - Return workflows for the given project from `workflows` (id, project_id, code, name, description, created_at, updated_at).
  - For each workflow, include its **stages**: from `workflow_stages` ordered by `stage_order` (id, workflow_id, stage_order, stage_name, stage_role, created_at).
  - This supports the project board: one page with all workflows and their stage columns.

### 3.3 Stories (for project board)

- **GET /api/projects/:projectId/stories**
  - Return all stories that belong to any workflow of this project (stories.workflow_id IN (workflow ids of project)).
  - Include: id, title, description, type, priority, workflow_id, workflow_stage_id, assignee_id, blocked, blocked_reason, review_status, and any other fields needed for cards and move logic.
  - Join or resolve: workflow name, current stage name, assignee name (from agents), and **stage_role** of the current stage (from workflow_stages) so the client/backend can enforce who can move the story.
  - Include **dependencies**: list of story ids that this story depends on (from story_dependencies where story_id = stories.id). Optionally include resolved status (e.g. whether dependency is Done) for UI (blocked indicator).
  - Include **related** story ids if useful (from story_related). Non-blocking; for display only in this phase.

### 3.4 Agents (for assignee display and transition rules)

- **GET /api/agents**
  - Return list of agents (id, name, role code from roles) so the UI can show assignee names and the backend can resolve “acting agent = owner” (e.g. single owner agent in DB). Current user is treated as owner; no login yet.

### 3.5 Move story (with transition rules)

- **PATCH /api/stories/:storyId/stage** (or POST with body)
  - Body: `{ "workflow_stage_id": <target stage id> }` (or equivalent).
  - **Transition rules (enforced server-side):**
    - **Acting agent = owner:** Allow moving the story from any stage to any other stage (forward or backward). Owner may fast-track, rewind, skip stages, override blocked/dependency constraints.
    - **Acting agent ≠ owner:** Allow only if (1) the story’s **current** stage’s `stage_role` (from workflow_stages) matches the acting agent’s role, and (2) the target stage is the **immediate next** stage by `workflow_stages.stage_order` in the same workflow. Do not allow moving out of a Review stage (only owner can approve/reject from Review).
  - For this phase, **acting agent is always the owner** (no login): resolve the owner agent from DB (e.g. role = owner) and use that as `changed_by_agent_id`.
  - On success:
    - Update `stories.workflow_stage_id` (and optionally assignee if needed).
    - Insert one row into `story_stage_history` (story_id, from/to stage names and ids, assignee_id, changed_by_agent_id, created_at).
    - Optionally append to `story_audit_log` (e.g. event_type `stage_transition`, note with from/to).
  - Return the updated story (and possibly updated stage history) so the UI can refresh the card.
  - On validation failure (e.g. invalid transition): return 400 with a clear message.

### 3.6 Create story

- **GET /api/stories/next-id** — Returns `{ nextId: "S043" }` (or next available S###) for the create-story modal.
- **POST /api/stories** — Create a new story. Body: `title` (required), `type`, `priority`, `placement` (either `"backlog"` or `{ workflow_id, workflow_stage_id }`), plus optional fields (description, assignee_id, blocked, blocked_reason, blocked_by, acceptance_criteria, implementation_notes, branch, review_reference, artifact, review_status, review_notes, rejection_count, dependencies[], related[]). ID is auto-generated (next S###) unless provided. On success: 201 and created story.
- **UI:** From Workflows or Backlog, pressing **C** opens the create-story modal. Placement dropdown at top: first option **Backlog**, then optgroups by project with workflows (same as “Add to workflow…”). Form fields mirror edit-story modal. Create and Cancel buttons.

### 3.7 Create project

- **POST /api/projects** — Create a new project. Body: `{ "name": "<project name>" }`. Returns 201 and the new project (id, name, created_at, updated_at). The project has no workflows initially.
- **UI:** On the Workflows page, a **“Create Project”** button appears to the right of the project dropdown; pressing **P** on the Workflows view also opens the same modal. The modal has a single field “Project name” and **Create** / **Cancel** buttons. On Create, the project is created and the board refreshes with the new project selected (empty until workflows are added). Modal closes on Create, Cancel, X, or Escape; does not close when clicking outside. **Whilst any of the create-story (C), edit-story (E), or create-project (P) modals are open, C, E, and P key events are ignored** so users can type those letters in form fields.

### 3.8 Create workflow

- **GET /api/roles** — Returns list of roles (id, code) for the stage_role dropdown in the create-workflow modal (owner, dev, unit-test, integration-test, performance-test, devops).
- **POST /api/projects/:projectId/workflows** — Create a new workflow and its stages. Body: `code` (required, unique per project), `name` (required), `description` (optional), `stages` (array of `{ stage_name, stage_role }`, at least one required; stage_role must be one of the valid role codes). Inserts into `workflows` then `workflow_stages` in order (stage_order 1-based). Returns 201 and the created workflow with its stages.
- **UI:** On the Workflows page only, a **"Create Workflow"** button (between Create Project and Create Story) and **W** key open the create-workflow modal. The workflow is created for the **currently selected project**; if none is selected, the app prompts to select a project first. Modal: code, name, description; a configurable list of stages (name + stage_role per row; add/remove rows). Create and Cancel buttons. On success, the board refreshes and shows the new workflow section.

---

## 7. Phase 4 — Frontend: shell, routing, and static delivery

### 4.1 Single-page shell

- **public/index.html**
  - Root HTML shell: doctype, meta, title “TRACE H.Q.”, link to main CSS, root div (e.g. `#app`), script(s) to load app JS.
  - Include a **side menu** with at least:
    - **Workflows** — navigates to the project board (see below).
    - **Settings** — opens settings (theme, etc.).
  - Optional: Home link that shows a simple home view (default view when the app runs).
  - **Workflows view:** Contains a **board header** (top of the view) with a title on the left and the **project dropdown on the top right** for easy access. The project selector is not in the side menu. The header also includes **"Create Project"** (**P**), **"Create Workflow"** (**W**), and **"Create story"** (**C**) buttons in that order.

### 4.2 Client-side “routing”

- **public/js/app.js** (or equivalent)
  - On load: read URL hash or path (e.g. `#/workflows`, `#/settings`) and render the corresponding view. Default route: home.
  - **Last viewed project:** Persist last viewed project id (e.g. in localStorage). When the user chooses “Workflows”, open the project board for that project (or the first project if none saved).
  - When the user selects a different project (e.g. from the **project dropdown (top-right of the Workflows view)**), update “last viewed project” and re-render the board.

### 4.3 Theme (settings)

- **Settings**
  - Provide a settings surface (e.g. in side menu or header). At minimum:
    - **Theme:** Three options — **Light** (default), **Dark**, **Medium** (mid/dim between light and dark).
  - Persist selected theme in `localStorage` (e.g. key `tracehq_theme`, values `light` | `dark` | `medium`). On every load, read and apply the theme so it survives refresh.
  - **public/css/app.css**: Define CSS variables for colors/backgrounds per theme (e.g. `--bg`, `--text`, `--card-bg`) and apply a class or data attribute on `<html>` or `<body>` (e.g. `data-theme="light"`). Default to Light on first load.

---

## 8. Phase 5 — Project board (Workflows UI)

### 5.1 One page, stacked workflow sections

- **Board header:** The Workflows view has a header at the top: title (e.g. "Project board") on the left, **project dropdown on the top right**, then **Create Project**, **Create Workflow**, and **Create story** buttons. **Create Workflow** (W) opens a modal to add a workflow (code, name, description, stages) to the selected project. **Create story** opens the same create-story modal as pressing **C**. The dropdown is always visible and easily accessible when viewing the board.
- When the user is on “Workflows” and a project is selected:
  - **GET /api/projects/:projectId/workflows** and **GET /api/projects/:projectId/stories** (or a combined endpoint if preferred).
  - Render **one scrollable page** (the project board) that contains **all** workflows for that project, stacked vertically in workflow id order. Each workflow is one **workflow section**: a kanban-style block with:
    - A heading (workflow name).
    - **Columns** = stages (ordered by stage_order).
    - **Cards** = stories in that workflow, placed in the column corresponding to their current `workflow_stage_id`.

### 5.2 Look and feel

- Modern kanban-style UI: clear columns (stages), clear cards (stories), familiar patterns (e.g. Jira/Linear/Trello-like). Professional, clean layout. No need to replicate every detail; prioritize clarity and correctness of data and transitions.

### 5.3 Story cards

- Each card shows at least: story id (e.g. S001), title, assignee (name or “Unassigned”), blocked indicator if blocked, and optionally priority/type. Click or action to **move** the story (see below).

### 5.4 Moving stories

- **Two ways to move:** (1) **Drag-and-drop** — story cards are draggable; the user drops a card anywhere within a stage column (including on top of another story). The drop target is the column: any release inside that column moves the story to that stage. (2) **Dropdown** — “Move to…” on each card. The **first option after “Move to…” is “Backlog”**: choosing it moves the story to the global backlog (PATCH `/api/stories/:storyId/workflow` with `workflow_id` and `workflow_stage_id` null). The other options are the workflow’s stages; selecting one calls **PATCH /api/stories/:storyId/stage** with the chosen `workflow_stage_id`.
- **Drop behaviour:** Releasing the drag anywhere within the chosen stage column (empty space or on another card) updates the story to that stage according to the same transition rules. No-op if dropped on the same stage. Backend enforces transition rules (owner can do anything; for this phase user is always owner). On success, refresh the board so the story appears in the new column.
- **Target list:** For owner, show all stages of the story’s workflow as valid targets. (Future: for non-owner, only the immediate next stage would be allowed; backend already enforces this.)
- **Visual feedback:** Cards show a blue outline on hover; while dragging, the card can show reduced opacity; the stage column under the cursor can show a drop-target highlight (e.g. accent border or background).

### 5.5 Dependencies and blocked state

- Show on the card (or tooltip) if the story is **blocked** and/or has **dependencies** (e.g. “Blocked by S003”). Data is already returned by GET project stories; no extra API required for first draft. Owner can still move the story (override) via the same move endpoint.

---

## 9. Phase 6 — Polish and verification

### 6.1 Error handling

- API: Return appropriate status codes and JSON error messages. Frontend: show a simple message or toast when a request fails (e.g. 400 invalid transition, 500 server error).

### 6.2 DB warmup on startup

- On server start, warm up the database (e.g. run a simple query with retries) before calling `listen`, so the first HTTP request does not trigger a cold connection and fail (e.g. "Failed to load stories") when MySQL is slow to start. The pool in `config/db.js` should use a `connectTimeout` (e.g. 15s) for slow MySQL startup.

### 6.3 No password in logs or CLI

- Confirm that `DB_PASSWORD` and full connection strings are never logged or passed on the command line. Use `config/db.js` and `.env` only.

### 6.4 Readme and run instructions

- Add or update **trace_hq/README.md** with:
  - Prerequisites: Node.js, MySQL, schema and seed data applied.
  - How to install dependencies (`npm install`), configure `.env`, and run the app (`npm start` or `node server.js`).
  - That the app serves the UI and API on one port; open the given URL (e.g. http://localhost:3000) to use TRACE H.Q.

---

## 10. Execution order (summary)

1. **Phase 1** — Create package.json, config/db.js, server.js; serve static and mount /api.
2. **Phase 2** — Confirm DB schema and seed data are present (no code change).
3. **Phase 3** — Implement API routes: projects, project workflows+stages, project stories (with deps/related), agents, PATCH story stage with transition rules.
4. **Phase 4** — Build index.html, side menu, client routing, theme persistence, CSS variables for Light/Dark/Medium.
5. **Phase 5** — Build project board: fetch workflows + stories, render stacked workflow sections, story cards, move action calling API and refreshing UI.
6. **Phase 6** — Error handling, security check (no password exposure), README.

---

## 11. Optional and future extensions

- **Create project / create workflow** — Add POST endpoints and UI (project list, “Add workflow” on project) when needed.
- **Story detail panel** — Side panel or modal with full story fields (description, acceptance criteria, stage history, audit log) fed from existing or new GET story-by-id endpoint.
- **Drag-and-drop** — Replace “Move to” dropdown with DnD for cards between columns; still call the same PATCH API and enforce rules on server.
- **Login and multi-user** — Replace “current user = owner” with real identity; pass acting agent to move endpoint and enforce owner vs non-owner rules.
- **Agentic integration** — Keep transition rules and stage_role; integrate with external agents later without changing core model.

This implementation plan is the first draft; it can be updated as requirements or design evolve. When you ask to “run” this plan, execute the phases above in order to produce the first working version of TRACE H.Q.

---

## 12. Backlog

The **Backlog** is a **global** list of stories not yet assigned to any workflow (not tied to a project). Full implementation details are in **[BACKLOG-IMPLEMENTATION-PLAN.md](BACKLOG-IMPLEMENTATION-PLAN.md)**. Summary:

- **Database:** `stories.project_id` is **nullable**. When `workflow_id` is NULL, the story is in the **global backlog** and `project_id` is NULL. When added to a workflow, `project_id` is set to that workflow’s project. **`stories.backlog_order`** (INT UNSIGNED NULL) stores display order. Migrations: `04a_stories_project_id.sql`, `04c_backlog_order.sql`, `04d_global_backlog.sql`; seed `04b_backlog_stories.sql` (backlog stories with `project_id` NULL).
- **API:** GET `/api/backlog` (all backlog stories, ordered by `backlog_order`, then id); PUT `/api/backlog/order` (body `{ orderedStoryIds: [] }`); GET `/api/projects-with-workflows` (for “Add to workflow” dropdown: projects with nested workflows); PATCH `/api/stories/:storyId/workflow` (add to **any** workflow, or **move to backlog** when body is `{ workflow_id: null, workflow_stage_id: null }`; sets or clears `project_id` accordingly).
- **UI:** Side menu **Backlog**; Backlog view with **no project selector**. Grid of backlog cards; **draggable** to reorder. Each card has **“Add to workflow…”** dropdown **grouped by project** (e.g. mep-sentinel → Development, Performance Testing, …; another-project → …). Only workflows are selectable; user can send a story to any project’s workflow. On the **Workflows** board, each story card’s **“Move to…”** dropdown has **“Backlog”** as the first option to move the story back to the global backlog.
