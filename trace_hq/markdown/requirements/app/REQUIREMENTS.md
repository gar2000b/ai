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
- **Side menu:** The app has a side menu with **Home**, **Workflows**, **Backlog**, and **Settings**.
- **Selecting Workflows:** Choosing "Workflows" shows the **last viewed Project** (not a per-workflow selection). The app remembers which project was last viewed and opens that.
- **Project selector:** The project dropdown is in the **top-right** of the Workflows view (in the board header), so it is easily accessible and does not clutter the side menu. It should look integrated with the board (e.g. same visual language as the rest of the UI).
- **Project:** A **Project** corresponds to one row in the database **`projects`** table (e.g. mep-sentinel). It is the top-level container.
- **Project Workflows:** A project has **1..n Project Workflows** (database **`workflows`** table). Each workflow is a role-routed, stage-based delivery pipeline; workflow names and stage counts are defined in the database (e.g. Development, Performance Testing, DevOps, Manual).
- **One page, stacked workflows:** For the selected project, the app shows **one scrollable page** (the **project board**) on which **all of that project’s workflows are displayed one above the other** in workflow order (by `display_order` when the migration has been applied, otherwise by id). Each workflow section can have a different number of stages. Think of multiple independent kanban “walls” placed on the same page, stacked vertically. The user does not switch between separate board pages; they scroll one page that contains every workflow for that project.
- **Terminology:**
  - **Project board** = this single page for a project: the view that contains all workflows for that project. It is the “board” you see when you open Workflows for a project.
  - **Workflow section** = each vertical block on that page: one kanban-style area per workflow (stages as columns, stories as cards). So one project board is made of multiple workflow sections (one per project workflow), stacked in workflow order (display_order when present, else id).
- **Creating a new workflow** adds a new workflow to the project (a new row in `workflows` and its stages); the project board then shows one more workflow section (in workflow order). **Creating a new project:** A **“Create Project”** button appears to the right of the project dropdown on the Workflows page; pressing **P** on the Workflows view also opens the same modal. A **"Create Workflow"** button appears between Create Project and Create story; pressing **W** opens the create-workflow modal (Workflows section only; workflow is attached to the currently selected project). The modal collects workflow **code** (unique per project), **name**, optional **description**, and **stages** (name and stage_role per stage; add/remove rows; at least one stage required). A **"Create story"** button appears to the right of Create Workflow and opens the same create-story modal as pressing **C** (see Design and UX). The project-creation modal prompts for the new project name; the user enters the name and clicks **Create** or **Cancel**. The new project is created with no workflows initially; when selected, the board is empty until workflows are added.
- **Editing a workflow:** Each workflow section on the board has an **Edit** link to the right of the workflow name. Clicking it opens a modal that is the same as (or very similar to) the Create Workflow modal, but in **edit** mode: workflow **code** is read-only; **name**, **description**, and **stages** can be updated. The user can add, remove, or reorder stages and change stage names and roles. **Save** and **Cancel** buttons apply or discard changes; on save, the workflow and its stages are updated and the board refreshes.
- **Reordering workflows:** Each workflow section header has **up** and **down** arrow buttons (like the stage reorder buttons in the Edit Workflow modal), placed beside the Edit link. The user can move a workflow up or down within the project board. Order is persisted in `workflows.display_order` (migration **02a_workflow_display_order.sql**). If that migration has not been run, the board still loads (workflows ordered by id) and the reorder buttons show a message directing the user to run the migration.
- **Keyboard shortcuts and modals:** Whilst any of the create-story (C), edit-story (E), create-project (P), create-workflow (W), or edit-workflow modals are open, **C**, **E**, **P**, and **W** key events are ignored so users can type those letters in form fields without triggering the shortcuts.

### Backlog

- **Backlog** is a **global** list of user stories that are **not assigned to any workflow** (not tied to any project). They appear only in the Backlog view, not on any project board.
- **Side menu:** The app has a **Backlog** item (between Workflows and Settings). Choosing it opens the Backlog view (no project selector).
- **Backlog view:** Title “Backlog” and short subtitle. Stories are displayed in a **grid** (top-left to bottom-right). Each card shows story id, title, type, priority, assignee, and any blocked/dependency info.
- **Backlog order:** The user can **reorder** backlog stories by **drag-and-drop**. Order is persisted in the database (`stories.backlog_order`) and survives refresh.
- **Add to workflow:** Each backlog story card has an **“Add to workflow…”** dropdown. Options are **grouped by project** (e.g. “mep-sentinel” with workflows Development, Performance Testing, DevOps, Manual; then “another-project” with its workflows). Only **workflows** are selectable (project names are group labels). The user can send a story to **any workflow on any project**. On selection, the story is added to that workflow’s first stage and to that project; it is removed from the backlog.
- **Data model:** When `workflow_id` is NULL, the story is in the **global backlog** and **`project_id`** is NULL. **`backlog_order`** (INT UNSIGNED NULL) defines display order. When a story is added to a workflow, `project_id` is set to that workflow’s project and `workflow_id` / `workflow_stage_id` are set.

---

## Tech stack

- **Backend:** Node.js.
- **Frontend:** Vanilla JavaScript (no framework). Static assets (HTML, CSS, JS, images) and client-side behaviour implemented with plain JS.
- **Database integration:** The backend **connects to the existing Trace HQ MySQL database** and uses it as the source and store for all application data (projects, workflows, stages, stories, agents, dependencies, stage history, etc.). The app **loads database credentials from the project root `.env` file** (`trace_hq/.env`): `DB_HOST`, `DB_USER`, `DB_PASSWORD`, `DB_NAME`. Never log or expose the password; never pass it on the command line. Full connection details, connection string, and security rules are in [markdown/database/DATABASE.md](../../database/DATABASE.md). Schema and seed data live in `database/schema/` and `database/data/` (see [References](#references-for-implementation-plan)). On startup, the server **warms up the database** (e.g. a simple query with retries) before accepting HTTP connections, so the first request does not fail when MySQL is slow to start.

---

## Hosting and delivery

- The **frontend** (static content, JS, CSS, etc.) is **served by the backend application**. There is no separate static host or CDN for the app UI; the Node.js app serves both API and static files to keep the setup simple.

---

## Design and UX

- The app must **look, feel and operate like a modern kanban tool** (e.g. Jira, Linear, Trello). Boards with clear columns (stages), cards (stories), **drag-and-drop** and a **dropdown** to move cards, clean layout, and familiar patterns so the human owner can use it without explanation. Prioritise a professional, modern feel.
- **Moving stories:** The user can move a story in two ways: (1) **Drag-and-drop** — drag a story card and drop it anywhere within the target stage column (including on top of another story); the story is moved to that stage according to the same transition rules. (2) **Dropdown** — use the “Move to…” dropdown on the card. The **first option after “Move to…” is “Backlog”**: selecting it moves the story back to the global backlog (it is removed from the board). The remaining options are the other stages in the same workflow. Both use the same APIs (PATCH stage for stages; PATCH workflow with `workflow_id`/`workflow_stage_id` null for backlog); the human owner can move to any stage or to backlog.
- **Story card selection:** In both **Workflows** (project board) and **Backlog**, a story card can be **selected** by clicking on it (not on the move/add-to-workflow dropdown). The selected card shows a persistent blue outline (accent border and subtle ring). Only one card can be selected at a time; clicking another card selects that one and deselects the previous. **Clicking the same card again deselects it**, removing the outline. Selection is for use by future enhancements (e.g. detail panel, bulk actions).
- **Edit story:** When a story card is **selected**, pressing the **E** key opens an **edit story** modal. The modal allows editing all story fields (identity, assignment, blocking, dependencies, related, execution/review, notes). **Save** and **Cancel** buttons; the modal does **not** close when clicking outside (only Cancel, X, or Escape).
- **Delete story:** When a story card is **selected** (in Workflows or Backlog), pressing the **D** key opens a **delete story** confirmation modal. The modal asks “Are you sure you wish to delete the story?” and shows the story id and title. **Delete** and **Cancel** buttons; the story is **logically deleted** (hidden from the app; the row remains in the database with `deleted_at` set). No physical delete. Whilst the delete modal or any other create/edit/project/workflow modal is open, **C**, **E**, **P**, **W**, and **D** key events are ignored so users can type those letters in form fields.
- **Create story:** From **Workflows** or **Backlog**, the user can open the **create story** modal by pressing the **C** key or, on the Workflows view only, by clicking the **"Create story"** button (to the right of the Create Project button). The modal is similar to the edit modal. At the **top**, a **placement** dropdown selects where to put the new story: the **first option is "Backlog"**; the remaining options are the same structure as the "Add to workflow…" dropdown (grouped by project, workflows as selectable options). The rest of the form mirrors the edit story fields (title, description, type, priority, assignee, blocking, dependencies, related, execution, notes). **Create** and **Cancel** buttons.
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
