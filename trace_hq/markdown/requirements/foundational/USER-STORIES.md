# user-stories.md

## USER STORIES COLLECTION

**Coverage:** At least one story at each stage in each workflow (Development 11 stages, Performance 5, DevOps 5, Manual 5). **WIP limit:** Each agent has at most one story in Planning or In Progress at a time; extra stories in those stages are unassigned.

---

### S001 — Add /health endpoint

- **id:** S001
- **title:** Add /health endpoint
- **description:** Implement a basic health endpoint returning service status.
- **type:** dev
- **priority:** medium
- **workflow:** development
- **createdBy:** owner
- **createdAt:** 2026-03-01T09:00:00Z
- **lastUpdatedAt:** 2026-03-02T10:15:00Z
- **dependencies:** []
- **blocked:** false
- **blockedReason:** ""
- **blockedAt:** ""
- **blockedBy:** ""
- **currentStage:** Dev In Progress
- **stageRole:** dev
- **assignee:** dev-lisa
- **stageHistory:**
  - Todo → Dev Planning
  - Dev Planning → Dev In Progress
- **acceptanceCriteria:**
  - GET /health returns 200
  - Response JSON is { "ok": true }
- **implementationNotes:** Basic Express route, no auth required.
- **relatedStories:** []
- **branch:** dev/S001-health
- **reviewReference:** ""
- **artifact:** ""
- **reviewStatus:** not-required
- **reviewNotes:** ""
- **rejectionCount:** 0
- **notes:**
  - Claimed by dev-lisa

---

### S002 — Write Getting Started guide

- **id:** S002
- **title:** Write Getting Started guide
- **description:** Create onboarding documentation for new developers.
- **type:** writing
- **priority:** high
- **workflow:** manual
- **createdBy:** owner
- **createdAt:** 2026-03-01T09:05:00Z
- **lastUpdatedAt:** 2026-03-02T08:30:00Z
- **dependencies:** []
- **blocked:** false
- **blockedReason:** ""
- **blockedAt:** ""
- **blockedBy:** ""
- **currentStage:** Review
- **stageRole:** owner
- **assignee:** owner
- **stageHistory:**
  - Todo → Planning (owner)
  - Planning → In Progress (owner)
  - In Progress → Review (owner)
- **acceptanceCriteria:**
  - Includes install instructions
  - Includes run instructions
  - Includes environment variables section
- **implementationNotes:** Draft written manually.
- **relatedStories:** []
- **branch:** ""
- **reviewReference:** docs/GETTING_STARTED.md
- **artifact:** docs/GETTING_STARTED.md
- **reviewStatus:** pending
- **reviewNotes:** ""
- **rejectionCount:** 0
- **notes:**
  - Ready for final owner approval

---

### S003 — Add config loader

- **id:** S003
- **title:** Add config loader
- **description:** Support loading configuration from env and JSON file.
- **type:** dev
- **priority:** high
- **workflow:** development
- **createdBy:** owner
- **createdAt:** 2026-03-01T09:10:00Z
- **lastUpdatedAt:** 2026-03-03T11:00:00Z
- **dependencies:** []
- **blocked:** false
- **blockedReason:** ""
- **blockedAt:** ""
- **blockedBy:** ""
- **currentStage:** Dev Review
- **stageRole:** owner
- **assignee:** owner
- **stageHistory:**
  - Todo → Dev Planning
  - Dev Planning → Dev In Progress
  - Dev In Progress → Dev Review
  - Supports JSON config file
  - Fails fast on missing required config
- **implementationNotes:** Schema validation added.
- **relatedStories:** []
- **branch:** dev/S003-config
- **reviewReference:** PR#18
- **artifact:** ""
- **reviewStatus:** pending
- **reviewNotes:** ""
- **rejectionCount:** 0
- **notes:**
  - Awaiting owner review

---

### S004 — Add integration test for config loader

- **id:** S004
- **title:** Add integration test for config loader
- **description:** Validate config loader across env + file interaction.
- **type:** dev
- **priority:** medium
- **workflow:** development
- **createdBy:** owner
- **createdAt:** 2026-03-01T09:20:00Z
- **lastUpdatedAt:** 2026-03-03T12:00:00Z
- **dependencies:** [S003]
- **blocked:** true
- **blockedReason:** Waiting for S003 to reach Done.
- **blockedAt:** 2026-03-03T12:00:00Z
- **blockedBy:** dependency
- **currentStage:** Todo
- **stageRole:** dev
- **assignee:** unassigned
- **stageHistory:**
  - Created in Todo
- **acceptanceCriteria:**
  - Validates mixed env + file behavior
  - Covers invalid config cases
- **implementationNotes:** ""
- **relatedStories:** []
- **branch:** ""
- **reviewReference:** ""
- **artifact:** ""
- **reviewStatus:** not-required
- **reviewNotes:** ""
- **rejectionCount:** 0
- **notes:**
  - Blocked by S003

---

### S005 — Add readiness endpoint

- **id:** S005
- **title:** Add readiness endpoint
- **description:** Add readiness endpoint dependent on health endpoint.
- **type:** dev
- **priority:** low
- **workflow:** development
- **createdBy:** owner
- **createdAt:** 2026-03-01T09:30:00Z
- **lastUpdatedAt:** 2026-03-02T10:20:00Z
- **dependencies:** [S001]
- **blocked:** false
- **blockedReason:** ""
- **blockedAt:** ""
- **blockedBy:** ""
- **currentStage:** Dev Planning
- **stageRole:** dev
- **assignee:** dev-bob
- **stageHistory:**
  - Todo → Dev Planning
  - Returns 503 if dependencies not satisfied
- **implementationNotes:** ""
- **relatedStories:** []
- **branch:** dev/S005-readiness
- **reviewReference:** ""
- **artifact:** ""
- **reviewStatus:** not-required
- **reviewNotes:** ""
- **rejectionCount:** 0
- **notes:**
  - Claimed by dev-bob

---

### S006 — Load test /health endpoint

- **id:** S006
- **title:** Load test /health endpoint
- **description:** Validate health endpoint under 10k RPS load.
- **type:** performance
- **priority:** medium
- **workflow:** performance
- **createdBy:** owner
- **createdAt:** 2026-03-02T09:00:00Z
- **lastUpdatedAt:** 2026-03-02T15:00:00Z
- **dependencies:** [S001]
- **blocked:** false
- **blockedReason:** ""
- **blockedAt:** ""
- **blockedBy:** ""
- **currentStage:** In Progress
- **stageRole:** performance-test
- **assignee:** perf-derek
- **stageHistory:**
  - Todo → Planning (performance-test)
  - Planning → In Progress (performance-test)
- **acceptanceCriteria:**
  - Sustains 10k RPS
  - < 100ms average latency
- **implementationNotes:** Using k6 harness.
- **relatedStories:** []
- **branch:** ""
- **reviewReference:** ""
- **artifact:** performance/report-S006.json
- **reviewStatus:** not-required
- **reviewNotes:** ""
- **rejectionCount:** 0
- **notes:**
  - Running benchmark tests

---

### S007 — Setup staging deployment pipeline

- **id:** S007
- **title:** Setup staging deployment pipeline
- **description:** Add GitHub Actions workflow for staging environment.
- **type:** infrastructure
- **priority:** high
- **workflow:** devops
- **createdBy:** owner
- **createdAt:** 2026-03-02T09:15:00Z
- **lastUpdatedAt:** 2026-03-02T14:00:00Z
- **dependencies:** []
- **blocked:** false
- **blockedReason:** ""
- **blockedAt:** ""
- **blockedBy:** ""
- **currentStage:** Review
- **stageRole:** owner
- **assignee:** owner
- **stageHistory:**
  - Todo → Planning (devops)
  - Planning → In Progress (devops)
  - In Progress → Review (owner)
- **acceptanceCriteria:**
  - Auto deploy on main branch
  - Secrets stored securely
- **implementationNotes:** Workflow YAML added.
- **relatedStories:** []
- **branch:** devops/S007-staging
- **reviewReference:** PR#21
- **artifact:** ""
- **reviewStatus:** pending
- **reviewNotes:** ""
- **rejectionCount:** 0
- **notes:**
  - Awaiting owner approval

---

### S008 — Research JWT expiry strategy

- **id:** S008
- **title:** Research JWT expiry strategy
- **description:** Evaluate token expiry and refresh approaches.
- **type:** research
- **priority:** medium
- **workflow:** manual
- **createdBy:** owner
- **createdAt:** 2026-03-02T10:00:00Z
- **lastUpdatedAt:** 2026-03-02T12:00:00Z
- **dependencies:** []
- **blocked:** false
- **blockedReason:** ""
- **blockedAt:** ""
- **blockedBy:** ""
- **currentStage:** In Progress
- **stageRole:** owner
- **assignee:** owner
- **stageHistory:**
  - Todo → Planning (owner)
  - Planning → In Progress (owner)
- **acceptanceCriteria:**
  - Compare 3 expiry strategies
  - Document pros/cons
- **implementationNotes:** ""
- **relatedStories:** []
- **branch:** ""
- **reviewReference:** ""
- **artifact:** docs/JWT-RESEARCH.md
- **reviewStatus:** not-required
- **reviewNotes:** ""
- **rejectionCount:** 0
- **notes:**
  - Ongoing research

---

### S009 — Add logging middleware

- **id:** S009
- **title:** Add logging middleware
- **description:** Log method + path for all requests.
- **type:** dev
- **priority:** medium
- **workflow:** development
- **createdBy:** owner
- **createdAt:** 2026-03-02T11:00:00Z
- **lastUpdatedAt:** 2026-03-03T09:00:00Z
- **dependencies:** []
- **blocked:** false
- **blockedReason:** ""
- **blockedAt:** ""
- **blockedBy:** ""
- **currentStage:** Done
- **stageRole:** owner
- **assignee:** unassigned
- **stageHistory:**
  - Todo → Dev Planning
  - Dev Planning → Dev In Progress
  - Dev In Progress → Dev Review
  - Dev Review → Done
  - Toggle via LOG_LEVEL
- **implementationNotes:** Uses winston logger.
- **relatedStories:** []
- **branch:** dev/S009-logging
- **reviewReference:** PR#17
- **artifact:** ""
- **reviewStatus:** approved
- **reviewNotes:** Looks good.
- **rejectionCount:** 0
- **notes:**
  - Merged by owner

---

### S010 — Create epic roadmap draft

- **id:** S010
- **title:** Create epic roadmap draft
- **description:** Draft roadmap for next 3 months.
- **type:** manual
- **priority:** critical
- **workflow:** manual
- **createdBy:** owner
- **createdAt:** 2026-03-03T08:00:00Z
- **lastUpdatedAt:** 2026-03-03T08:00:00Z
- **dependencies:** []
- **blocked:** false
- **blockedReason:** ""
- **blockedAt:** ""
- **blockedBy:** ""
- **currentStage:** Todo
- **stageRole:** owner
- **assignee:** unassigned
- **stageHistory:**
  - Created
- **acceptanceCriteria:**
  - Define 3 milestone themes
  - Align with business goals
- **implementationNotes:** ""
- **relatedStories:** []
- **branch:** ""
- **reviewReference:** ""
- **artifact:** ""
- **reviewStatus:** not-required
- **reviewNotes:** ""
- **rejectionCount:** 0
- **notes:**
  - In manual workflow; ready to claim

---

### S011 — Unit test health endpoint

- **id:** S011
- **title:** Unit test health endpoint
- **description:** Add isolated unit tests for /health route and response shape.
- **type:** dev
- **priority:** medium
- **workflow:** development
- **createdBy:** owner
- **createdAt:** 2026-03-03T10:00:00Z
- **lastUpdatedAt:** 2026-03-03T11:00:00Z
- **dependencies:** [S001]
- **blocked:** false
- **blockedReason:** ""
- **blockedAt:** ""
- **blockedBy:** ""
- **currentStage:** Test Planning
- **stageRole:** unit-test
- **assignee:** test-ava
- **stageHistory:**
  - Todo → Test Planning
  - No external calls
- **implementationNotes:** ""
- **relatedStories:** []
- **branch:** test/S011-health-unit
- **reviewReference:** ""
- **artifact:** ""
- **reviewStatus:** not-required
- **reviewNotes:** ""
- **rejectionCount:** 0
- **notes:**
  - Claimed by test-ava

---

### S012 — Unit tests for config loader

- **id:** S012
- **title:** Unit tests for config loader
- **description:** Isolated tests for config loader module.
- **type:** dev
- **priority:** medium
- **workflow:** development
- **createdBy:** owner
- **createdAt:** 2026-03-03T10:05:00Z
- **lastUpdatedAt:** 2026-03-03T12:00:00Z
- **dependencies:** [S003]
- **blocked:** false
- **blockedReason:** ""
- **blockedAt:** ""
- **blockedBy:** ""
- **currentStage:** Test In Progress
- **stageRole:** unit-test
- **assignee:** test-ian
- **stageHistory:**
  - Todo → Test Planning
  - Test Planning → Test In Progress
  - Fast, deterministic
- **implementationNotes:** ""
- **relatedStories:** []
- **branch:** test/S012-config-unit
- **reviewReference:** ""
- **artifact:** ""
- **reviewStatus:** not-required
- **reviewNotes:** ""
- **rejectionCount:** 0
- **notes:**
  - In progress by test-ian

---

### S013 — Review unit tests for config loader

- **id:** S013
- **title:** Review unit tests for config loader
- **description:** Owner review of S012 unit test PR before merge.
- **type:** dev
- **priority:** medium
- **workflow:** development
- **createdBy:** owner
- **createdAt:** 2026-03-03T12:30:00Z
- **lastUpdatedAt:** 2026-03-03T13:00:00Z
- **dependencies:** [S012]
- **blocked:** false
- **blockedReason:** ""
- **blockedAt:** ""
- **blockedBy:** ""
- **currentStage:** Test Review
- **stageRole:** owner
- **assignee:** owner
- **stageHistory:**
  - Todo → Test Planning
  - Test Planning → Test In Progress
  - Test In Progress → Test Review
- **acceptanceCriteria:**
  - Coverage and style approved
- **implementationNotes:** ""
- **relatedStories:** [S012]
- **branch:** test/S012-config-unit
- **reviewReference:** PR#22
- **artifact:** ""
- **reviewStatus:** pending
- **reviewNotes:** ""
- **rejectionCount:** 0
- **notes:**
  - Awaiting owner review

---

### S014 — Integration test plan for health + ready

- **id:** S014
- **title:** Integration test plan for health + ready
- **description:** Plan integration tests for health and readiness endpoints together.
- **type:** dev
- **priority:** medium
- **workflow:** development
- **createdBy:** owner
- **createdAt:** 2026-03-03T11:00:00Z
- **lastUpdatedAt:** 2026-03-03T14:00:00Z
- **dependencies:** [S001, S005]
- **blocked:** false
- **blockedReason:** ""
- **blockedAt:** ""
- **blockedBy:** ""
- **currentStage:** Int Test Planning
- **stageRole:** integration-test
- **assignee:** int-dana
- **stageHistory:**
  - Todo → Int Test Planning
  - Use shared harness
- **implementationNotes:** ""
- **relatedStories:** []
- **branch:** ""
- **reviewReference:** ""
- **artifact:** ""
- **reviewStatus:** not-required
- **reviewNotes:** ""
- **rejectionCount:** 0
- **notes:**
  - Claimed by int-dana

---

### S015 — Integration tests for health + ready

- **id:** S015
- **title:** Integration tests for health + ready
- **description:** Implement integration tests for health and readiness in harness.
- **type:** dev
- **priority:** medium
- **workflow:** development
- **createdBy:** owner
- **createdAt:** 2026-03-03T11:10:00Z
- **lastUpdatedAt:** 2026-03-03T15:00:00Z
- **dependencies:** [S001, S005]
- **blocked:** false
- **blockedReason:** ""
- **blockedAt:** ""
- **blockedBy:** ""
- **currentStage:** Int Test In Progress
- **stageRole:** integration-test
- **assignee:** int-marc
- **stageHistory:**
  - Todo → Int Test Planning
  - Int Test Planning → Int Test In Progress
  - 503 when deps down
- **implementationNotes:** ""
- **relatedStories:** []
- **branch:** int/S015-health-ready
- **reviewReference:** ""
- **artifact:** ""
- **reviewStatus:** not-required
- **reviewNotes:** ""
- **rejectionCount:** 0
- **notes:**
  - In progress by int-marc

---

### S016 — Review integration tests (health + ready)

- **id:** S016
- **title:** Review integration tests (health + ready)
- **description:** Owner review of integration test PR for health and ready.
- **type:** dev
- **priority:** medium
- **workflow:** development
- **createdBy:** owner
- **createdAt:** 2026-03-03T15:30:00Z
- **lastUpdatedAt:** 2026-03-03T16:00:00Z
- **dependencies:** [S015]
- **blocked:** false
- **blockedReason:** ""
- **blockedAt:** ""
- **blockedBy:** ""
- **currentStage:** Int Test Review
- **stageRole:** owner
- **assignee:** owner
- **stageHistory:**
  - Todo → Int Test Planning
  - Int Test Planning → Int Test In Progress
  - Int Test In Progress → Int Test Review
- **acceptanceCriteria:**
  - Harness usage and coverage approved
- **implementationNotes:** ""
- **relatedStories:** [S015]
- **branch:** int/S015-health-ready
- **reviewReference:** PR#23
- **artifact:** ""
- **reviewStatus:** pending
- **reviewNotes:** ""
- **rejectionCount:** 0
- **notes:**
  - Awaiting owner review

---

### S017 — Load test config loader

- **id:** S017
- **title:** Load test config loader
- **description:** Validate config loader performance under load.
- **type:** performance
- **priority:** low
- **workflow:** performance
- **createdBy:** owner
- **createdAt:** 2026-03-03T09:00:00Z
- **lastUpdatedAt:** 2026-03-03T09:00:00Z
- **dependencies:** [S003]
- **blocked:** false
- **blockedReason:** ""
- **blockedAt:** ""
- **blockedBy:** ""
- **currentStage:** Todo
- **stageRole:** performance-test
- **assignee:** unassigned
- **stageHistory:**
  - Created in Todo
- **acceptanceCriteria:**
  - No regression under 1k RPS
- **implementationNotes:** ""
- **relatedStories:** []
- **branch:** ""
- **reviewReference:** ""
- **artifact:** ""
- **reviewStatus:** not-required
- **reviewNotes:** ""
- **rejectionCount:** 0
- **notes:**
  - Queued after S003 Done

---

### S018 — Plan load test for /ready

- **id:** S018
- **title:** Plan load test for /ready
- **description:** Define load and latency targets for readiness endpoint.
- **type:** performance
- **priority:** medium
- **workflow:** performance
- **createdBy:** owner
- **createdAt:** 2026-03-03T09:30:00Z
- **lastUpdatedAt:** 2026-03-03T10:00:00Z
- **dependencies:** [S005]
- **blocked:** false
- **blockedReason:** ""
- **blockedAt:** ""
- **blockedBy:** ""
- **currentStage:** Planning
- **stageRole:** performance-test
- **assignee:** perf-maya
- **stageHistory:**
  - Todo → Planning (performance-test)
- **acceptanceCriteria:**
  - RPS and p99 targets documented
- **implementationNotes:** ""
- **relatedStories:** []
- **branch:** ""
- **reviewReference:** ""
- **artifact:** ""
- **reviewStatus:** not-required
- **reviewNotes:** ""
- **rejectionCount:** 0
- **notes:**
  - Claimed by perf-maya

---

### S019 — Review health endpoint load test

- **id:** S019
- **title:** Review health endpoint load test
- **description:** Owner approval of S006 load test results and thresholds.
- **type:** performance
- **priority:** medium
- **workflow:** performance
- **createdBy:** owner
- **createdAt:** 2026-03-03T14:00:00Z
- **lastUpdatedAt:** 2026-03-03T14:30:00Z
- **dependencies:** [S006]
- **blocked:** false
- **blockedReason:** ""
- **blockedAt:** ""
- **blockedBy:** ""
- **currentStage:** Review
- **stageRole:** owner
- **assignee:** owner
- **stageHistory:**
  - Todo → Planning (performance-test)
  - Planning → In Progress (performance-test)
  - In Progress → Review (owner)
- **acceptanceCriteria:**
  - 10k RPS and latency accepted
- **implementationNotes:** ""
- **relatedStories:** [S006]
- **branch:** ""
- **reviewReference:** ""
- **artifact:** performance/report-S006.json
- **reviewStatus:** pending
- **reviewNotes:** ""
- **rejectionCount:** 0
- **notes:**
  - Awaiting owner approval

---

### S020 — Load test readiness endpoint (done)

- **id:** S020
- **title:** Load test readiness endpoint
- **description:** Load test /ready; completed and approved.
- **type:** performance
- **priority:** low
- **workflow:** performance
- **createdBy:** owner
- **createdAt:** 2026-03-02T16:00:00Z
- **lastUpdatedAt:** 2026-03-03T11:00:00Z
- **dependencies:** [S005]
- **blocked:** false
- **blockedReason:** ""
- **blockedAt:** ""
- **blockedBy:** ""
- **currentStage:** Done
- **stageRole:** owner
- **assignee:** owner
- **stageHistory:**
  - Todo → Planning (performance-test)
  - Planning → In Progress (performance-test)
  - In Progress → Review (owner)
  - Review → Done
- **acceptanceCriteria:**
  - 5k RPS sustained
  - Report approved
- **implementationNotes:** k6 run completed.
- **relatedStories:** []
- **branch:** ""
- **reviewReference:** ""
- **artifact:** performance/report-S020.json
- **reviewStatus:** approved
- **reviewNotes:** Accepted.
- **rejectionCount:** 0
- **notes:**
  - Completed by perf-maya, approved by owner

---

### S021 — Production deployment pipeline

- **id:** S021
- **title:** Production deployment pipeline
- **description:** Add GitHub Actions workflow for production with approvals.
- **type:** infrastructure
- **priority:** high
- **workflow:** devops
- **createdBy:** owner
- **createdAt:** 2026-03-03T08:30:00Z
- **lastUpdatedAt:** 2026-03-03T08:30:00Z
- **dependencies:** [S007]
- **blocked:** false
- **blockedReason:** ""
- **blockedAt:** ""
- **blockedBy:** ""
- **currentStage:** Todo
- **stageRole:** devops
- **assignee:** unassigned
- **stageHistory:**
  - Created in Todo
- **acceptanceCriteria:**
  - Manual approval gate
  - Rollback documented
- **implementationNotes:** ""
- **relatedStories:** []
- **branch:** ""
- **reviewReference:** ""
- **artifact:** ""
- **reviewStatus:** not-required
- **reviewNotes:** ""
- **rejectionCount:** 0
- **notes:**
  - Depends on S007 Done

---

### S022 — Plan production deployment

- **id:** S022
- **title:** Plan production deployment
- **description:** Design production deploy steps and approval flow.
- **type:** infrastructure
- **priority:** high
- **workflow:** devops
- **createdBy:** owner
- **createdAt:** 2026-03-03T09:00:00Z
- **lastUpdatedAt:** 2026-03-03T09:30:00Z
- **dependencies:** []
- **blocked:** false
- **blockedReason:** ""
- **blockedAt:** ""
- **blockedBy:** ""
- **currentStage:** Planning
- **stageRole:** devops
- **assignee:** devops-sam
- **stageHistory:**
  - Todo → Planning (devops)
- **acceptanceCriteria:**
  - Steps and rollback documented
- **implementationNotes:** ""
- **relatedStories:** []
- **branch:** ""
- **reviewReference:** ""
- **artifact:** ""
- **reviewStatus:** not-required
- **reviewNotes:** ""
- **rejectionCount:** 0
- **notes:**
  - Claimed by devops-sam

---

### S023 — Implement production deploy workflow

- **id:** S023
- **title:** Implement production deploy workflow
- **description:** Add production workflow YAML and approval job.
- **type:** infrastructure
- **priority:** high
- **workflow:** devops
- **createdBy:** owner
- **createdAt:** 2026-03-02T17:00:00Z
- **lastUpdatedAt:** 2026-03-03T10:00:00Z
- **dependencies:** [S007]
- **blocked:** false
- **blockedReason:** ""
- **blockedAt:** ""
- **blockedBy:** ""
- **currentStage:** In Progress
- **stageRole:** devops
- **assignee:** devops-raj
- **stageHistory:**
  - Todo → Planning (devops)
  - Planning → In Progress (devops)
- **acceptanceCriteria:**
  - Workflow runs on tag
  - Approval required
- **implementationNotes:** ""
- **relatedStories:** []
- **branch:** devops/S023-production
- **reviewReference:** ""
- **artifact:** ""
- **reviewStatus:** not-required
- **reviewNotes:** ""
- **rejectionCount:** 0
- **notes:**
  - In progress by devops-raj

---

### S024 — Staging pipeline (done)

- **id:** S024
- **title:** Staging pipeline completed
- **description:** Staging deployment pipeline implemented and approved (historical).
- **type:** infrastructure
- **priority:** high
- **workflow:** devops
- **createdBy:** owner
- **createdAt:** 2026-03-01T14:00:00Z
- **lastUpdatedAt:** 2026-03-02T16:00:00Z
- **dependencies:** []
- **blocked:** false
- **blockedReason:** ""
- **blockedAt:** ""
- **blockedBy:** ""
- **currentStage:** Done
- **stageRole:** owner
- **assignee:** owner
- **stageHistory:**
  - Todo → Planning (devops)
  - Planning → In Progress (devops)
  - In Progress → Review (owner)
  - Review → Done
- **acceptanceCriteria:**
  - Auto deploy on main
  - Secrets secured
- **implementationNotes:** Completed by devops-sam.
- **relatedStories:** []
- **branch:** devops/S024-staging
- **reviewReference:** PR#20
- **artifact:** ""
- **reviewStatus:** approved
- **reviewNotes:** Merged.
- **rejectionCount:** 0
- **notes:**
  - Done; devops-sam implemented, owner merged

---

### S025 — Plan Q2 roadmap

- **id:** S025
- **title:** Plan Q2 roadmap
- **description:** Outline Q2 themes and milestones for stakeholder review.
- **type:** manual
- **priority:** high
- **workflow:** manual
- **createdBy:** owner
- **createdAt:** 2026-03-03T08:00:00Z
- **lastUpdatedAt:** 2026-03-03T08:30:00Z
- **dependencies:** []
- **blocked:** false
- **blockedReason:** ""
- **blockedAt:** ""
- **blockedBy:** ""
- **currentStage:** Planning
- **stageRole:** owner
- **assignee:** unassigned
- **stageHistory:**
  - Todo → Planning (owner)
- **acceptanceCriteria:**
  - Three themes with dates
  - Aligned with business
- **implementationNotes:** ""
- **relatedStories:** []
- **branch:** ""
- **reviewReference:** ""
- **artifact:** ""
- **reviewStatus:** not-required
- **reviewNotes:** ""
- **rejectionCount:** 0
- **notes:**
  - Unassigned; owner at WIP limit (S008 In Progress)

---

### S026 — Document API decisions (done)

- **id:** S026
- **title:** Document API decisions
- **description:** Record API versioning and error-format decisions; completed.
- **type:** docs
- **priority:** medium
- **workflow:** manual
- **createdBy:** owner
- **createdAt:** 2026-03-02T11:00:00Z
- **lastUpdatedAt:** 2026-03-03T09:00:00Z
- **dependencies:** []
- **blocked:** false
- **blockedReason:** ""
- **blockedAt:** ""
- **blockedBy:** ""
- **currentStage:** Done
- **stageRole:** owner
- **assignee:** owner
- **stageHistory:**
  - Todo → Planning (owner)
  - Planning → In Progress (owner)
  - In Progress → Review (owner)
  - Review → Done
- **acceptanceCriteria:**
  - ADR or doc updated
  - Decisions traceable
- **implementationNotes:** docs/API-DECISIONS.md.
- **relatedStories:** []
- **branch:** ""
- **reviewReference:** ""
- **artifact:** docs/API-DECISIONS.md
- **reviewStatus:** approved
- **reviewNotes:** ""
- **rejectionCount:** 0
- **notes:**
  - Completed by owner

---

### S027 — Triage support backlog

- **id:** S027
- **title:** Triage support backlog
- **description:** Review and prioritise open support items for next sprint.
- **type:** manual
- **priority:** low
- **workflow:** manual
- **createdBy:** owner
- **createdAt:** 2026-03-03T12:00:00Z
- **lastUpdatedAt:** 2026-03-03T12:00:00Z
- **dependencies:** []
- **blocked:** false
- **blockedReason:** ""
- **blockedAt:** ""
- **blockedBy:** ""
- **currentStage:** Todo
- **stageRole:** owner
- **assignee:** owner
- **stageHistory:**
  - Created in Todo
- **acceptanceCriteria:**
  - Items labelled and prioritised
  - Blockers called out
- **implementationNotes:** ""
- **relatedStories:** []
- **branch:** ""
- **reviewReference:** ""
- **artifact:** ""
- **reviewStatus:** not-required
- **reviewNotes:** ""
- **rejectionCount:** 0
- **notes:**
  - Not yet started

---
