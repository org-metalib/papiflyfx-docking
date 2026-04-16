#set($h1 = '#')
#set($h2 = '##')
#set($h3 = '###')
${h1} ${artifactId} Agent Operating Model

This directory defines how the specialized repository agents collaborate. See [`../../AGENTS.md`](../../AGENTS.md) for role definitions.

${h2} Team Topology

| Agent | Primary Ownership |
| --- | --- |
| `@app-dev` | `${rootArtifactId}-app`, application features, docking layout |
| `@ops-engineer` | Root `pom.xml`, CI/CD, dependency management |
| `@spec-steward` | `spec/`, docs, planning, coordination |

${h2} Shared Workflow

Follow the research -> plan -> implement -> validate pattern for non-trivial work.

${h3} 1. Intake
- State the goal, identify impacted modules and reviewers.

${h3} 2. Research
- Create `research.md` when the area is unfamiliar or risky.

${h3} 3. Planning
- Document the approach in `plan.md` with scope, non-goals, and validation strategy.

${h3} 4. Implementation
- Keep `progress.md` current as milestones are completed.

${h3} 5. Validation
- Record checks in `validation.md` or progress log.

${h3} 6. Closure
- Ensure docs match final implementation.

${h2} Handoff Contract

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

${h2} Definition of Done

- Lead agent kept changes within the correct boundary.
- Required reviewers were consulted.
- Validation was recorded.
- Specs and README files reflect the final state.
