# user-stories.md

# ==========================================================
# USER STORIES COLLECTION
# ==========================================================

---------------------------------------------------------------------
- id: S001
  title: Add /health endpoint
  description: Implement a basic health endpoint returning service status.
  type: dev
  priority: medium
  workflow: development
  createdBy: owner
  createdAt: 2026-03-01T09:00:00Z
  lastUpdatedAt: 2026-03-02T10:15:00Z

  dependencies: []

  blocked: false
  blockedReason: ""
  blockedAt: ""
  blockedBy: ""

  currentStage: In Progress
  stageRole: dev
  assignee: dev-lisa
  stageHistory:
    - Todo → Planning (dev)
    - Planning → In Progress (dev)

  acceptanceCriteria:
    - GET /health returns 200
    - Response JSON is { "ok": true }

  implementationNotes: Basic Express route, no auth required.
  relatedStories: []

  branch: dev/S001-health
  reviewReference: ""
  artifact: ""

  reviewStatus: not-required
  reviewNotes: ""
  rejectionCount: 0

  notes:
    - Claimed by dev-lisa
---------------------------------------------------------------------

- id: S002
  title: Write Getting Started guide
  description: Create onboarding documentation for new developers.
  type: writing
  priority: high
  workflow: manual
  createdBy: owner
  createdAt: 2026-03-01T09:05:00Z
  lastUpdatedAt: 2026-03-02T08:30:00Z

  dependencies: []

  blocked: false
  blockedReason: ""
  blockedAt: ""
  blockedBy: ""

  currentStage: Review
  stageRole: owner
  assignee: owner
  stageHistory:
    - Todo → Planning (owner)
    - Planning → In Progress (owner)
    - In Progress → Review (owner)

  acceptanceCriteria:
    - Includes install instructions
    - Includes run instructions
    - Includes environment variables section

  implementationNotes: Draft written manually.
  relatedStories: []

  branch: ""
  reviewReference: docs/GETTING_STARTED.md
  artifact: docs/GETTING_STARTED.md

  reviewStatus: pending
  reviewNotes: ""
  rejectionCount: 0

  notes:
    - Ready for final owner approval
---------------------------------------------------------------------

- id: S003
  title: Add config loader
  description: Support loading configuration from env and JSON file.
  type: dev
  priority: high
  workflow: development
  createdBy: owner
  createdAt: 2026-03-01T09:10:00Z
  lastUpdatedAt: 2026-03-03T11:00:00Z

  dependencies: []

  blocked: false
  blockedReason: ""
  blockedAt: ""
  blockedBy: ""

  currentStage: Review
  stageRole: owner
  assignee: owner
  stageHistory:
    - Todo → Planning (dev)
    - Planning → In Progress (dev)
    - In Progress → Review (owner)

  acceptanceCriteria:
    - Loads from process.env
    - Supports JSON config file
    - Fails fast on missing required config

  implementationNotes: Schema validation added.
  relatedStories: []

  branch: dev/S003-config
  reviewReference: PR#18
  artifact: ""

  reviewStatus: pending
  reviewNotes: ""
  rejectionCount: 0

  notes:
    - Awaiting owner review
---------------------------------------------------------------------

- id: S004
  title: Add integration test for config loader
  description: Validate config loader across env + file interaction.
  type: dev
  priority: medium
  workflow: development
  createdBy: owner
  createdAt: 2026-03-01T09:20:00Z
  lastUpdatedAt: 2026-03-03T12:00:00Z

  dependencies: [S003]

  blocked: true
  blockedReason: Waiting for S003 to reach Done.
  blockedAt: 2026-03-03T12:00:00Z
  blockedBy: dependency

  currentStage: Todo
  stageRole: dev
  assignee: unassigned
  stageHistory:
    - Created in Todo

  acceptanceCriteria:
    - Validates mixed env + file behavior
    - Covers invalid config cases

  implementationNotes: ""
  relatedStories: []

  branch: ""
  reviewReference: ""
  artifact: ""

  reviewStatus: not-required
  reviewNotes: ""
  rejectionCount: 0

  notes:
    - Blocked by S003
---------------------------------------------------------------------

- id: S005
  title: Add readiness endpoint
  description: Add readiness endpoint dependent on health endpoint.
  type: dev
  priority: low
  workflow: development
  createdBy: owner
  createdAt: 2026-03-01T09:30:00Z
  lastUpdatedAt: 2026-03-02T10:20:00Z

  dependencies: [S001]

  blocked: false
  blockedReason: ""
  blockedAt: ""
  blockedBy: ""

  currentStage: Planning
  stageRole: dev
  assignee: dev-dave
  stageHistory:
    - Todo → Planning (dev)

  acceptanceCriteria:
    - GET /ready returns readiness state
    - Returns 503 if dependencies not satisfied

  implementationNotes: ""
  relatedStories: []

  branch: dev/S005-readiness
  reviewReference: ""
  artifact: ""

  reviewStatus: not-required
  reviewNotes: ""
  rejectionCount: 0

  notes:
    - Claimed by dev-dave
---------------------------------------------------------------------

- id: S006
  title: Load test /health endpoint
  description: Validate health endpoint under 10k RPS load.
  type: performance
  priority: medium
  workflow: performance
  createdBy: owner
  createdAt: 2026-03-02T09:00:00Z
  lastUpdatedAt: 2026-03-02T15:00:00Z

  dependencies: [S001]

  blocked: false
  blockedReason: ""
  blockedAt: ""
  blockedBy: ""

  currentStage: In Progress
  stageRole: performance-test
  assignee: performance-test-derek
  stageHistory:
    - Todo → Planning (performance-test)
    - Planning → In Progress (performance-test)

  acceptanceCriteria:
    - Sustains 10k RPS
    - < 100ms average latency

  implementationNotes: Using k6 harness.
  relatedStories: []

  branch: ""
  reviewReference: ""
  artifact: performance/report-S006.json

  reviewStatus: not-required
  reviewNotes: ""
  rejectionCount: 0

  notes:
    - Running benchmark tests
---------------------------------------------------------------------

- id: S007
  title: Setup staging deployment pipeline
  description: Add GitHub Actions workflow for staging environment.
  type: infrastructure
  priority: high
  workflow: devops
  createdBy: owner
  createdAt: 2026-03-02T09:15:00Z
  lastUpdatedAt: 2026-03-02T14:00:00Z

  dependencies: []

  blocked: false
  blockedReason: ""
  blockedAt: ""
  blockedBy: ""

  currentStage: Review
  stageRole: owner
  assignee: owner
  stageHistory:
    - Todo → Planning (devops)
    - Planning → In Progress (devops)
    - In Progress → Review (owner)

  acceptanceCriteria:
    - Auto deploy on main branch
    - Secrets stored securely

  implementationNotes: Workflow YAML added.
  relatedStories: []

  branch: devops/S007-staging
  reviewReference: PR#21
  artifact: ""

  reviewStatus: pending
  reviewNotes: ""
  rejectionCount: 0

  notes:
    - Awaiting owner approval
---------------------------------------------------------------------

- id: S008
  title: Research JWT expiry strategy
  description: Evaluate token expiry and refresh approaches.
  type: research
  priority: medium
  workflow: manual
  createdBy: owner
  createdAt: 2026-03-02T10:00:00Z
  lastUpdatedAt: 2026-03-02T12:00:00Z

  dependencies: []

  blocked: false
  blockedReason: ""
  blockedAt: ""
  blockedBy: ""

  currentStage: In Progress
  stageRole: owner
  assignee: owner
  stageHistory:
    - Todo → Planning (owner)
    - Planning → In Progress (owner)

  acceptanceCriteria:
    - Compare 3 expiry strategies
    - Document pros/cons

  implementationNotes: ""
  relatedStories: []

  branch: ""
  reviewReference: ""
  artifact: docs/JWT-RESEARCH.md

  reviewStatus: not-required
  reviewNotes: ""
  rejectionCount: 0

  notes:
    - Ongoing research
---------------------------------------------------------------------

- id: S009
  title: Add logging middleware
  description: Log method + path for all requests.
  type: dev
  priority: medium
  workflow: development
  createdBy: owner
  createdAt: 2026-03-02T11:00:00Z
  lastUpdatedAt: 2026-03-03T09:00:00Z

  dependencies: []

  blocked: false
  blockedReason: ""
  blockedAt: ""
  blockedBy: ""

  currentStage: Done
  stageRole: owner
  assignee: unassigned
  stageHistory:
    - Todo → Planning (dev)
    - Planning → In Progress (dev)
    - In Progress → Review (owner)
    - Review → Done

  acceptanceCriteria:
    - Logs method + path
    - Toggle via LOG_LEVEL

  implementationNotes: Uses winston logger.
  relatedStories: []

  branch: dev/S009-logging
  reviewReference: PR#17
  artifact: ""

  reviewStatus: approved
  reviewNotes: Looks good.
  rejectionCount: 0

  notes:
    - Merged by owner
---------------------------------------------------------------------

- id: S010
  title: Create epic roadmap draft
  description: Draft roadmap for next 3 months.
  type: manual
  priority: critical
  workflow: -
  createdBy: owner
  createdAt: 2026-03-03T08:00:00Z
  lastUpdatedAt: 2026-03-03T08:00:00Z

  dependencies: []

  blocked: false
  blockedReason: ""
  blockedAt: ""
  blockedBy: ""

  currentStage: Todo
  stageRole: owner
  assignee: unassigned
  stageHistory:
    - Created

  acceptanceCriteria:
    - Define 3 milestone themes
    - Align with business goals

  implementationNotes: ""
  relatedStories: []

  branch: ""
  reviewReference: ""
  artifact: ""

  reviewStatus: not-required
  reviewNotes: ""
  rejectionCount: 0

  notes:
    - Not yet assigned to a workflow
---------------------------------------------------------------------