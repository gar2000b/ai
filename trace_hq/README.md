# TRACE H.Q.

**TRACE H.Q.** is the overarching system, project, and tool. The first major capability is **Workflows** — a multi-agent kanban-style web application.

---

## Current focus: Workflows (multi-agent kanban)

The **Workflows** section is a multi-agent kanban-style system. Multiple agents (dev, unit-test, integration-test, performance-test, devops) advance and work on **user stories** as they move through structured pipelines. An **owner** role governs review gates and has full authority to fast-track, rewind, reassign, or correct workflow state.

### In a nutshell

- **Workflows** = delivery pipelines, each represented as a board. A project contains one or more workflows (e.g. Development, Performance Testing, DevOps, Manual). A **“Create Project”** button (to the right of the project dropdown) opens a **modal** to create a new empty project by name. From the board, stories can be **moved back to the backlog** via the “Move to…” dropdown (first option: Backlog).
- **Backlog** = global list of stories not yet in any workflow (not tied to a project). Stories can be moved from the backlog onto **any project’s workflow** via “Add to workflow” (dropdown grouped by project). Backlog order is **reordered by drag-and-drop** and persisted.
- **User stories** = units of work with a stable identity. A story is either in a workflow (on a project board; then it belongs to that project) or in the global backlog (no project). Stories in a workflow carry execution state: current stage, assignee, dependencies, blocking, branch/PR/artifact tracking, and an audit trail.
- **Agents** = role-based workers who move stories forward one stage at a time. The **owner** is the governance authority and can move any story to any stage.
- **Keyboard:** From Workflows or Backlog, **C** opens the create-story modal (placement: Backlog or any workflow). With a story card selected, **E** opens the edit-story modal. On Workflows, **P** opens the create-project modal and **W** opens the create-workflow modal. Whilst any of these modals is open, **C**, **E**, **P**, and **W** are ignored so you can type those letters in form fields.

---

## Prerequisites

- **Node.js** 18+
- **MySQL** with the Trace HQ schema and seed data applied (see `markdown/database/DATABASE.md` and `database/schema/`, `database/data/`).

---

## Setup and run

1. **Install dependencies**

   ```bash
   cd trace_hq
   npm install
   ```

2. **Configure the database**

   Copy or create a `.env` file in the `trace_hq` directory with:

   ```
   DB_HOST=localhost
   DB_USER=trace-hq-admin
   DB_PASSWORD=your_password
   DB_NAME=trace-hq
   ```

   Never commit real credentials. Keep `.env` in `.gitignore`.

3. **Apply schema and seed data** (if not already done)

   From the `trace_hq` directory, apply the DDL in `database/schema/` in order, then run the scripts in `database/data/` in order (see `markdown/database/DATABASE.md`).

4. **Start the application**

   ```bash
   npm start
   ```

   The app serves both the API and the web UI on one port. On startup it waits for the database to be ready (with retries) before accepting connections, so the first page load is reliable. Open **http://localhost:3000** in a browser to use TRACE H.Q. (Port can be overridden with the `PORT` environment variable.)

5. **After pulling changes that add schema migrations**

   If the codebase adds a **new migration** (e.g. `04c_backlog_order.sql`, `04d_global_backlog.sql`), run each **once per DB** from the `trace_hq` directory:

   ```bash
   ./scripts/mysql.sh < database/schema/04c_backlog_order.sql
   ./scripts/mysql.sh < database/schema/04d_global_backlog.sql
   ```

   Then **restart the app** if it is already running. No need to run migrations for code-only or frontend-only changes.

---

## Repo structure (high level)

```
trace_hq/
├── README.md
├── package.json
├── server.js              # Entry point: API + static
├── config/
│   └── db.js              # MySQL pool (reads .env)
├── api/
│   └── index.js           # API routes
├── public/                # Static UI (HTML, CSS, JS)
├── database/
│   ├── schema/            # DDL
│   └── data/              # Seed scripts
├── markdown/
│   ├── requirements/      # App + foundational requirements
│   ├── database/           # Database docs
│   └── plan/              # Implementation plan
└── scripts/
    └── mysql.sh           # CLI DB access (no password on command line)
```

---

## Requirements (design artifacts)

| Document | Purpose |
|----------|---------|
| **markdown/requirements/app/REQUIREMENTS.md** | Application requirements for the first draft. |
| **markdown/requirements/foundational/OPEN-WORKFLOWS-PROJECT.md** | Workflow model, agent types, stage patterns, transition rules. |
| **markdown/requirements/foundational/USER-STORY.md** | User story structure and design principles. |
| **markdown/requirements/foundational/USER-STORIES.md** | Example story set (S001–S027). |
| **markdown/database/DATABASE.md** | Database connection and schema/data reference. |
| **markdown/plan/IMPLEMENTATION-PLAN.md** | Detailed implementation plan (executable). |
| **markdown/plan/BACKLOG-IMPLEMENTATION-PLAN.md** | Backlog feature: database, API, UI. |

---

*TRACE H.Q. — Workflows first, more to come.*
