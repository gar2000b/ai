# TRACE H.Q.

**TRACE H.Q.** is the overarching system, project, and tool. Additional functionality will be added over time. For now, development is focused on the first major capability: **Workflows**.

---

## Current focus: Workflows (multi-agent kanban)

The **Workflows** section is a multi-agent kanban-style system. Multiple agents (dev, unit-test, integration-test, performance-test, devops) advance and work on **user stories** as they move through structured pipelines. An **owner** role governs review gates and has full authority to fast-track, rewind, reassign, or correct workflow state.

### In a nutshell

- **Workflows** = delivery pipelines, each represented as a board. A project contains one or more workflows (e.g. Development, Performance Testing, DevOps, Manual).
- **User stories** = units of work with a stable identity. Each story belongs to at most one workflow and carries execution state: current stage, assignee, dependencies, blocking, branch/PR/artifact tracking, and an audit trail.
- **Agents** = role-based workers (dev, unit-test, integration-test, performance-test, devops) who move stories forward one stage at a time. They cannot skip stages or bypass **Review**.
- **Owner** = governance authority. The owner approves at every Review stage, merges, and can intervene at any stage in any workflow.

Stories are the **single source of truth**. Workflows define allowed stage sequences and transition rules. Dependencies and blocking constrain progression unless the owner overrides.

---

## Requirements (design artifacts)

Detailed behaviour and data model are defined in markdown under `markdown/requirements/foundational/`:

| Document | Purpose |
|----------|---------|
| **foundational/OPEN-WORKFLOWS-PROJECT.md** | Workflow model, agent types, stage patterns, transition rules, and the four workflow definitions (Development, Performance, DevOps, Manual). |
| **foundational/USER-STORY.md** | User story structure: identity, dependencies, blocking, execution state, review governance, audit log. Design principles. |
| **foundational/USER-STORIES.md** | Example story set (S001–S010) illustrating the model across workflows and story types. |

These are design references, not machine-parsed specs. They guide tooling and evolution.

---

## Repo structure (high level)

```
trace_hq/
├── README.md                 # This file
├── markdown/
│   ├── requirements/        # Requirements (see above: foundational/)
│   ├── database/            # Database-related docs
│   └── plan/               # Planning and design docs
├── scripts/                 # Utility scripts (e.g. DB)
└── ...                      # Future: app code, config, etc.
```

---

## Next steps

- Refine this README as the product and repo evolve.
- Use the requirements markdown as the source for building the first version of the Workflows (multi-agent kanban) experience.

---

*TRACE H.Q. — Workflows first, more to come.*
