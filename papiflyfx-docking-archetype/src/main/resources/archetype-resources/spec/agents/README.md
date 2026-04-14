# ${artifactId} Agent Operating Model

This directory defines how the specialized repository agents collaborate. See [`../../AGENTS.md`](../../AGENTS.md) for role definitions.

## Team Topology

| Agent | Primary Ownership |
| --- | --- |
| `@app-dev` | `${rootArtifactId}-app`, application features, docking layout |
| `@ops-engineer` | Root `pom.xml`, CI/CD, dependency management |
| `@spec-steward` | `spec/`, docs, planning, coordination |

## Shared Workflow

Follow the research -> plan -> implement -> validate pattern for non-trivial work.

### 1. Intake
- State the goal, identify impacted modules and reviewers.

### 2. Research
- Create `research.md` when the area is unfamiliar or risky.

### 3. Planning
- Document the approach in `plan.md` with scope, non-goals, and validation strategy.

### 4. Implementation
- Keep `progress.md` current as milestones are completed.

### 5. Validation
- Record checks in `validation.md` or progress log.

### 6. Closure
- Ensure docs match final implementation.

## Handoff Contract

```md
Lead Agent:
Task Scope:
Impacted Modules:
Files Changed:
Key Invariants:
Validation Performed:
Open Risks / Follow-ups:
Required Reviewer:
```

## Definition of Done

- Lead agent kept changes within the correct boundary.
- Required reviewers were consulted.
- Validation was recorded.
- Specs and README files reflect the final state.
