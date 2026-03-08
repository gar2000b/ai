# user-story.md

## USER STORY DESIGN ARTIFACT

This document defines the conceptual structure of a User Story within the Open Workflows Project system.

This is NOT intended for machine parsing.
It is a design reference to guide tooling and system evolution.

A User Story:

- Represents a unit of work
- **Belongs to a project** (so it can appear in that project’s backlog when not in a workflow)
- Maintains identity independent of workflow definitions
- Is linked to at most one workflow (or none; then it is in the project’s **backlog**)
- Stores its own execution state
- May depend on other stories
- May be blocked
- Is auditable and traceable

Stories are the single source of truth.
Workflows define allowed stage sequences and transition rules.
Reverse dependency relationships are derived by the system.

---

## CORE IDENTITY

- **id:**  
  S### (globally unique within project)

- **title:**  
  short, descriptive summary

- **description:**  
  longer explanation of intent and context

- **type:**  
  dev | writing | docs | research | infrastructure | performance | manual

- **priority:**  
  low | medium | high | critical

- **workflow:**  
  development | performance | devops | manual | -

  Represents the workflow this story belongs to.  
  "-" means not yet assigned to a workflow; the story then appears only in the **project’s backlog** until it is added to a workflow.

- **createdBy:**  
  owner

- **createdAt:**  
  timestamp

- **lastUpdatedAt:**  
  timestamp

---

## DEPENDENCIES

- **dependencies:**  
  list of story IDs that must reach Done before this story may progress beyond Todo.

  Example:

  - S003
  - S014

**Notes:**

- Dependency relationships are stored in one direction only.
- Reverse relationships ("dependents") are derived by the system.
- The tool should maintain an indexed lookup for efficient querying.
- A story should not move forward if dependencies are incomplete (unless overridden by owner).

---

## BLOCKING STATE

- **blocked:**  
  true | false

- **blockedReason:**  
  explanation of why work cannot proceed

- **blockedAt:**  
  timestamp (optional)

- **blockedBy:**  
  dependency | environment | owner | external | unknown

**Notes:**

- When blocked == true, the story should not progress stages.
- Owner may override blocked status.
- Blocking events should be logged.

---

## WORKFLOW EXECUTION STATE (SOURCE OF TRUTH)

- **currentStage:**  
  Todo | Planning | In Progress | Review | Done

  This is the canonical stage name.  
  The valid stage order is defined by the assigned workflow.

- **stageRole:**  
  dev | unit-test | integration-test | performance-test | devops | owner

  Represents the role responsible for executing work in the current stage.

  Example:

  - currentStage: Planning  
  - stageRole: dev

- **assignee:**  
  unassigned | &lt;agent-name&gt;

- **stageHistory:**  
  append-only record of:

  - stage transitions
  - role changes
  - assignment changes
  - workflow transfers

---

## EXECUTION DETAILS

- **acceptanceCriteria:**  
  measurable, testable outcomes

- **implementationNotes:**  
  technical notes, context, decisions

- **relatedStories:**  
  list of associated story ids (non-blocking relationship)

---

## CODE & ARTIFACT TRACKING

- **branch:**  
  git branch name (if applicable)

- **reviewReference:**  
  PR link, review id, or artifact reference

- **artifact:**  
  documentation path or deliverable location

---

## GOVERNANCE & REVIEW

- **reviewStatus:**  
  not-required | pending | approved | rejected

- **reviewNotes:**  
  summary of owner feedback

- **rejectionCount:**  
  integer

---

## AUDIT LOG

- **notes:**  
  append-only chronological log of:

  - claims
  - planning decisions
  - handoffs
  - review outcomes
  - rejections
  - owner interventions
  - workflow transfers
  - dependency adjustments
  - blocking events

---

## DESIGN PRINCIPLES

1. Stories are the single source of truth.
2. Workflows define permissible stage sequences.
3. A User Story belongs to at most one workflow.
4. A story may be created without a workflow ("-").
5. Owner may reassign workflow at any time.
6. Non-owner roles may only advance stories forward one stage.
7. Governance (Review stages) are mandatory gates.
8. Execution roles cannot self-approve.
9. Dependencies must be satisfied before meaningful progression.
10. Blocked stories should not advance until resolved.
11. Reverse dependency relationships are derived, not stored.
12. All transitions and overrides should be logged.
