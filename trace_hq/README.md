# TRACE H.Q.

**TRACE H.Q.** is the overarching system, project, and tool. The first major capability is **Workflows** — a multi-agent kanban-style web application.

---

## Current focus: Workflows (multi-agent kanban)

The **Workflows** section is a multi-agent kanban-style system. Multiple agents (dev, unit-test, integration-test, performance-test, devops) advance and work on **user stories** as they move through structured pipelines. An **owner** role governs review gates and has full authority to fast-track, rewind, reassign, or correct workflow state.

### In a nutshell

- **Workflows** = delivery pipelines, each represented as a board. A project contains one or more workflows (e.g. Development, Performance Testing, DevOps, Manual).
- **User stories** = units of work with a stable identity. Each story belongs to at most one workflow and carries execution state: current stage, assignee, dependencies, blocking, branch/PR/artifact tracking, and an audit trail.
- **Agents** = role-based workers who move stories forward one stage at a time. The **owner** is the governance authority and can move any story to any stage.

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

   The app serves both the API and the web UI on one port. Open **http://localhost:3000** in a browser to use TRACE H.Q. (Port can be overridden with the `PORT` environment variable.)

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

---

*TRACE H.Q. — Workflows first, more to come.*
