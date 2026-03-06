# open-workflows-project.md

## OPEN WORKFLOWS PROJECT

This file defines a Workflows Project.

A Workflows Project:

- Contains 1..n independent workflows
- Each workflow represents a structured delivery pipeline
- Each workflow is represented as a board in the tool
- The UI side menu contains "Workflows"
- Selecting it opens the last active workflow board
- Creating a new workflow creates a new board within this project

"open" is a bespoke namespace.
"workflows-project" reflects that multiple workflows exist under one project.

This file is a design artifact (not machine-parsed).
It defines conceptual rules for workflow behavior.

---

## AGENT TYPES (SYSTEM ROLE MODEL)

- **owner**  
  Strategic workflow authority who defines requirements, controls routing and standards, coordinates overall workflow, reviews outcomes, merges PRs, and is the final decision-maker across all workflows.

- **dev agent**  
  Plans tasks before implementation, implements features and fixes according to acceptance criteria, produces working code and PRs, incorporates feedback, and reworks rejected changes.

- **unit-test agent**  
  Plans and creates isolated, deterministic tests for individual components, maintains unit test quality, and reworks tests when failures or feedback require refinement.

- **integration-test agent**  
  Verifies cross-component interactions and is responsible for maintaining, updating, and operating the shared integration test harness system.

- **performance-test agent**  
  Assesses scalability, load handling, latency, and resource usage under stress, and maintains the shared performance test harness infrastructure.

- **devops agent**  
  Manages CI/CD, infrastructure, environments, deployments, platform updates, and operational reliability. Responsible for pushing deployments and may be invoked by other agents.

---

## AGENT ROSTER (Personas)

Two agents per role except owner. Personas define style, strengths, and default claiming behavior.

- **owner**
  - **style:** Strategic authority, coordinates workflows, final decision-maker.
  - **strengths:** Requirements, routing, standards, review and merge.
  - **default:** Single owner persona; no auto-claim.

- **dev-bob**
  - **style:** Fast implementer, minimal changes, focuses on getting it working end-to-end.
  - **strengths:** Plumbing, endpoints, wiring, quick bug fixes.
  - **default:** Claims next eligible dev TODO when free.

- **dev-lisa**
  - **style:** Refactor-minded, clean architecture, reduces tech debt.
  - **strengths:** Structure, maintainability, improves testability.
  - **default:** Claims next eligible dev TODO when free.

- **test-ava**
  - **style:** Strict and adversarial testing, loves edge cases and regressions.
  - **strengths:** Negative tests, boundary conditions, flaky test detection.
  - **default:** Claims next eligible unit-test stage when free.

- **test-ian**
  - **style:** Coverage-focused, documents behavior, keeps tests readable.
  - **strengths:** Clear specs, regression suites, test documentation.
  - **default:** Claims next eligible unit-test stage when free.

- **int-dana**
  - **style:** End-to-end and contract-first, validates cross-service behavior.
  - **strengths:** Integration harness, API contracts, environment parity.
  - **default:** Claims next eligible integration-test stage when free.

- **int-marc**
  - **style:** Failure injection and resilience, stresses error paths.
  - **strengths:** Chaos-style tests, timeouts, retries, fallbacks.
  - **default:** Claims next eligible integration-test stage when free.

- **perf-maya**
  - **style:** Load and latency focused, identifies bottlenecks and thresholds.
  - **strengths:** Baseline metrics, percentile analysis, capacity planning.
  - **default:** Claims next eligible performance-test stage when free.

- **perf-derek**
  - **style:** Stress and scale, pushes limits and finds breaking points.
  - **strengths:** High RPS, memory/CPU profiling, scalability reports.
  - **default:** Claims next eligible performance-test stage when free.

- **devops-sam**
  - **style:** Automation-first, repeatable deployments, infrastructure as code.
  - **strengths:** CI/CD pipelines, secrets, staging and production parity.
  - **default:** Claims next eligible devops stage when free.

- **devops-raj**
  - **style:** Observability and reliability, keeps systems healthy.
  - **strengths:** Logging, metrics, alerts, rollback procedures.
  - **default:** Claims next eligible devops stage when free.

---

## WORKFLOW MODEL PRINCIPLES

- Stage roles define who performs work in that stage.
- Review stages are governance gates owned by the owner.
- Execution roles perform work; owner provides approval and merge authority.
- Workflows are independent but may trigger one another.
- No execution role may bypass Review stages.

**Standard stage pattern:**

- Todo
- Planning (role)
- In Progress (role)
- Review (owner)
- Done

---

## TRANSITION MODEL

### Owner Authority

- The owner may move a story from any stage to any other stage.
- The owner may:
  - Fast-track
  - Rewind
  - Escalate
  - Skip stages
  - Reassign work
  - Correct workflow errors
- The owner is the ultimate governance authority.

### Non-Owner Roles

Non-owner roles (dev, unit-test, integration-test, performance-test, devops):

- May only move a story from their current stage to the immediate next stage in the workflow sequence.
- May not skip stages.
- May not move a story backwards.
- May not move beyond a Review stage.
- May not bypass owner governance.

### Example (Development Workflow)

**Allowed:**

3. Dev In Progress  
→ 4. Dev Review

**Not Allowed:**

3. Dev In Progress  
→ 5. Test Planning

This enforces:

- Linear progression
- Mandatory governance gates
- Clear accountability
- Deterministic state transitions

---

## WORKFLOW 1: DEVELOPMENT WORKFLOW

**Purpose:**  
Deliver features from implementation through integration validation.

**Stages:**

1. Todo
2. Dev Planning
3. Dev In Progress
4. Dev Review
5. Test Planning
6. Test In Progress
7. Test Review
8. Int Test Planning
9. Int Test In Progress
10. Int Test Review
11. Done

**Notes:**

- Dev implements feature.
- Owner reviews and merges.
- Unit-test validates isolated correctness.
- Owner reviews and merges.
- Integration-test validates cross-system behaviour.
- Owner reviews and merges.
- Completion may trigger:
  - Performance Testing Workflow
  - DevOps Workflow

---

## WORKFLOW 2: PERFORMANCE TESTING WORKFLOW

**Purpose:**  
Validate scalability and stress behaviour after integration stability.

**Stages:**

1. Todo
2. Planning (performance-test)
3. In Progress (performance-test)
4. Review (owner)
5. Done

**Notes:**

- Uses shared performance harness.
- May depend on integration-test plugins.
- Owner governs acceptance of performance outcomes.
- May trigger rework in Development Workflow if thresholds fail.

---

## WORKFLOW 3: DEVOPS WORKFLOW

**Purpose:**  
Deploy, promote, and operationalize validated changes.

**Stages:**

1. Todo
2. Planning (devops)
3. In Progress (devops)
4. Review (owner)
5. Done

**Notes:**

- Handles CI/CD adjustments.
- Manages infrastructure updates.
- Pushes deployments to selected environments.
- Owner governs production promotion decisions.

---

## WORKFLOW 4: MANUAL WORKFLOW

**Purpose:**  
Process all user stories that require manual human work only.

**Stages:**

1. Todo
2. Planning (owner)
3. In Progress (owner)
4. Review (owner)
5. Done

**Notes:**

- No automated agents are involved.
- Owner performs all stages.
- Review stage acts as a deliberate checkpoint before completion.
- Useful for:
  - Strategic planning
  - Research
  - Manual documentation
  - External coordination
  - Business decisions

---

## COORDINATION PRINCIPLES

- Owner governs all Review stages across all workflows.
- Execution roles cannot self-approve.
- Workflows may enqueue items into other workflows.
- Stories maintain identity independent of workflow.
- WIP limits apply per-agent unless globally overridden.
- Owner may intervene at any stage in any workflow.

---

## FUTURE EXTENSIONS

- Cross-workflow dependency tracking
- Automated workflow triggers
- SLA / priority modeling
- Escalation rules
- Parallel workflow execution
- Workflow templates
- Structured audit logging
