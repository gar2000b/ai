# TRACE H.Q. — Application requirements

This document defines detailed requirements for the TRACE H.Q. application. It builds on the **foundational requirements** (workflows, user stories, data model), which are already reflected in the database schema and seed data. The database is in place; this document focuses on the app we will build on top of it.

This file will be translated into a detailed implementation plan. Anything relevant for building the app should be captured here. Foundational docs and the database remain the authoritative reference for behaviour and data; we do not duplicate them in full.

**Project root:** The **`trace_hq`** directory is the **root directory for this project**. All implementation (backend, frontend, config, scripts, and any new app code) must start from and live under `trace_hq/`. Paths in this document are relative to that root unless stated otherwise.

---

## References (for implementation plan)

When building the detailed plan or the app, use these as the source of truth:

- **Foundational requirements:** [markdown/requirements/foundational/](../foundational/)
  - [**OPEN-WORKFLOWS-PROJECT.md**](../foundational/OPEN-WORKFLOWS-PROJECT.md) — Workflow model, agent types, stage patterns, transition rules, workflow definitions (and examples), agent roster.
  - [**USER-STORY.md**](../foundational/USER-STORY.md) — User story structure: identity, dependencies, blocking, execution state, review governance, audit. Design principles.
  - [**USER-STORIES.md**](../foundational/USER-STORIES.md) — Example story set (S001–S027) illustrating the model.
- **Database:** Schema in [database/schema/](../../../database/schema/) (DDL), seed data in [database/data/](../../../database/data/), connection and usage in [markdown/database/DATABASE.md](../../database/DATABASE.md). ER diagram: [database/schema/schema-er.mmd](../../../database/schema/schema-er.mmd).

---

## Product

- **Name:** TRACE H.Q.
- **Nature:** SaaS-like web application for the Workflows (multi-agent kanban) system and related capabilities.
- **Current focus:** First major capability is **Workflows** (multi-agent kanban). Additional functionality will be added over time.

---

## Domain summary (from foundational)

Summary of concepts the app must reflect; full behaviour and rules are in the [foundational docs above](#references-for-implementation-plan).

- **Workflows** = delivery pipelines, each represented as a **workflow section** on the project board. A project contains one or more workflows. Each workflow has its own sequence of stages (number and names of stages vary per workflow; defined in the database). Example: Development (11 stages), Performance Testing, DevOps, Manual — but there can be any number of workflows with varying stage counts. Workflows are independent but may trigger one another.
- **User stories** = units of work; single source of truth. Each story belongs to **at most one workflow** and has execution state: current stage, assignee, dependencies, blocking, branch/PR/artifact tracking, audit trail. Story identity (e.g. S001), type, priority, review status, etc. are defined in [USER-STORY.md](../foundational/USER-STORY.md).
- **Agents** = role-based workers: **owner** plus **dev**, **unit-test**, **integration-test**, **performance-test**, **devops**. There is an agent roster (e.g. owner, dev-bob, dev-lisa, test-ava, …). Non-owner agents move stories **one stage forward** only; they cannot skip stages or bypass Review. **Owner** governs all Review stages, can fast-track, rewind, reassign, or correct workflow state at any time.
- **Stage rules:** Review stages are governance gates (owner approval). Standard pattern: Todo → Planning (role) → In Progress (role) → Review (owner) → Done. Dependencies and blocking constrain progression unless the owner overrides.

### Who can move a story to which stage (transition rules)

The app must enforce who can move which story where. The database stores each stage’s **stage_role** (`workflow_stages.stage_role`): the role responsible for work in that stage (owner, dev, unit-test, integration-test, performance-test, devops). Use this for permissions.

- **Owner (any agent with role owner):**
  - May move **any** story from **any** stage to **any** other stage (forward or backward).
  - May fast-track, rewind, skip stages, reassign, and override blocked or dependency constraints.
  - Only the owner may move a story **out of** a Review stage (e.g. approve → Done, or reject → rewind).

- **Non-owner (dev, unit-test, integration-test, performance-test, devops):**
  - May only move a story **from the story’s current stage to the immediate next stage** (by `workflow_stages.stage_order`) in that workflow.
  - May **only** do that when the story’s **current stage’s `stage_role`** matches the acting agent’s role. Example: story in “Dev In Progress” has `stage_role` = dev → only an agent with role dev (or owner) can advance it to the next stage (e.g. “Dev Review”).
  - May **not** skip stages, move backward, or move a story **out of** a Review stage; only the owner can approve/reject from Review.

So: for a given story and “move to next stage”, the app checks: (1) if acting agent is owner → allow (and allow any other transition too). (2) If not owner → allow only if the story’s current stage has `stage_role` = acting agent’s role and the target is exactly the next stage; and do not allow moving out of a Review stage. Dependencies and blocked state can block progression unless the acting agent is owner (owner can override). Full details: [OPEN-WORKFLOWS-PROJECT.md § TRANSITION MODEL](../foundational/OPEN-WORKFLOWS-PROJECT.md#transition-model).

### Current user (no login yet)

For the moment the app has **no login or authentication**. The person using TRACE H.Q. (the user navigating the app) is **assumed to be the owner**. So for all actions (moving stories, approving at Review, etc.) the **acting agent** is the owner — e.g. the agent record with role owner in the database. The app does not need to ask who is acting; treat the current user as owner for permission checks and for recording who performed an action (e.g. in stage history or audit). Login and multi-user identity can be added later.

### Agentic integration (not specified yet)

Integration with **agentic systems** (e.g. OpenClaw or other autonomous agents) has **not** been specified as of yet. The immediate goal is to get **basic functionality working with the human owner** — board, stories, moving cards, look and feel — and to sort that out first. All of the **agent rules** above (owner vs non-owner roles, stage_role, who can move where) remain in place for when agentic integration is added later; they are not removed.

---

## Workflows UI and project board

- **Default:** When TRACE H.Q. runs, the user sees the **home page** by default.
- **Side menu:** The app has a side menu with an item **"Workflows"**.
- **Selecting Workflows:** Choosing "Workflows" shows the **last viewed Project** (not a per-workflow selection). The app remembers which project was last viewed and opens that.
- **Project:** A **Project** corresponds to one row in the database **`projects`** table (e.g. mep-sentinel). It is the top-level container.
- **Project Workflows:** A project has **1..n Project Workflows** (database **`workflows`** table). Each workflow is a role-routed, stage-based delivery pipeline; workflow names and stage counts are defined in the database (e.g. Development, Performance Testing, DevOps, Manual).
- **One page, stacked workflows:** For the selected project, the app shows **one scrollable page** (the **project board**) on which **all of that project’s workflows are displayed one above the other** in workflow id order. Each workflow section can have a different number of stages. Think of multiple independent kanban “walls” placed on the same page, stacked vertically. The user does not switch between separate board pages; they scroll one page that contains every workflow for that project.
- **Terminology:**
  - **Project board** = this single page for a project: the view that contains all workflows for that project. It is the “board” you see when you open Workflows for a project.
  - **Workflow section** = each vertical block on that page: one kanban-style area per workflow (stages as columns, stories as cards). So one project board is made of multiple workflow sections (one per project workflow), stacked in id order.
- **Creating a new workflow** adds a new workflow to the project (a new row in `workflows` and its stages); the project board then shows one more workflow section (in id order). Creating a **new project** adds a new project; when the user switches to it (or views it for the first time), that project’s board is shown as above.

---

## Tech stack

- **Backend:** Node.js.
- **Frontend:** Vanilla JavaScript (no framework). Static assets (HTML, CSS, JS, images) and client-side behaviour implemented with plain JS.
- **Database integration:** The backend **connects to the existing Trace HQ MySQL database** and uses it as the source and store for all application data (projects, workflows, stages, stories, agents, dependencies, stage history, etc.). The app **loads database credentials from the project root `.env` file** (`trace_hq/.env`): `DB_HOST`, `DB_USER`, `DB_PASSWORD`, `DB_NAME`. Never log or expose the password; never pass it on the command line. Full connection details, connection string, and security rules are in [markdown/database/DATABASE.md](../../database/DATABASE.md). Schema and seed data live in `database/schema/` and `database/data/` (see [References](#references-for-implementation-plan)).

---

## Hosting and delivery

- The **frontend** (static content, JS, CSS, etc.) is **served by the backend application**. There is no separate static host or CDN for the app UI; the Node.js app serves both API and static files to keep the setup simple.

---

## Design and UX

- The app must **look, feel and operate like a modern kanban tool** (e.g. Jira, Linear, Trello). Boards with clear columns (stages), cards (stories), drag-and-drop or obvious actions to move cards, clean layout, and familiar patterns so the human owner can use it without explanation. Prioritise a professional, modern feel.
- **Settings:** A **settings** entry (e.g. in the side menu or header) opens a settings area. Include at least:
  - **Theme:** Three options — **Light** (default), **Dark**, **Medium** (a mid/dim theme between light and dark). The chosen theme is persisted (e.g. in `localStorage`) so it survives refresh. Default to **Light** for first load.
- Other settings (e.g. board density, date format) can be added later; theme and a recognisable settings surface are in scope for the first pass.

---

## Out of scope (for now)

Explicitly **not** in scope for the current phase. Can be added in follow-up.

- **Login and authentication** — no multi-user identity; the single user is assumed to be the owner.
- **Integration with agentic systems** (e.g. OpenClaw) — not specified or built yet.
- **Mobile native apps** — web only.
- **Offline support** — app assumes a live connection to the backend.
- **Real-time collaboration** — no multi-user simultaneous editing or live presence.
- **Advanced reporting / analytics** — beyond basic board view and story data.
- **Email or push notifications** — none.
- **Public or third-party API** — no external API for other tools (internal app + DB only).

---

## (Further sections to be added)

- Application features and user flows
- Pages and navigation
- API and data access
- Security and authentication
- Deployment and environment

*To be expanded in follow-up.*
