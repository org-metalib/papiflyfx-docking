# PapiflyFX Docking Agent Operating Model

This directory defines how the specialized repository agents collaborate. The role files describe each specialist. This document defines how work is routed, how handoffs happen, and what must be true before a task is considered complete.

## Team Topology

| Agent | Primary Ownership | Mandatory Review Triggers |
| --- | --- | --- |
| `@core-architect` | `papiflyfx-docking-api`, `papiflyfx-docking-docks`, shared docking contracts and layout/session models | Public API/SPI changes, layout model changes, session format changes |
| `@feature-dev` | `papiflyfx-docking-code`, `tree`, `media`, `hugo`, `github`, new dockable content | New content types, `ContentFactory` or `ContentStateAdapter` implementations |
| `@ops-engineer` | Root `pom.xml`, `papiflyfx-docking-settings-api`, `papiflyfx-docking-settings`, `papiflyfx-docking-samples`, release/build health | Dependency/plugin changes, settings persistence changes, sample coverage, release preparation |
| `@auth-specialist` | `papiflyfx-docking-login-idapi`, `papiflyfx-docking-login-session-api`, `papiflyfx-docking-login` | OAuth/IDP changes, session storage, secret handling, login lifecycle |
| `@ui-ux-designer` | Theme system, module-local CSS, UX review across all modules | Visual regressions, theme behavior, accessibility, interaction ergonomics |
| `@spec-steward` | `spec/**`, roadmap, planning/progress artifacts, repository-level docs | Cross-cutting task intake, spec drift, acceptance criteria, delivery tracking |
| `@qa-engineer` | Test strategy, test infrastructure, coverage, regression suites across all modules | Test infrastructure changes, headless profile updates, new module test scaffolding |

## Routing Rules

1. Start with the owning specialist for tasks that clearly fall inside one primary domain.
2. Start with `@spec-steward` for ambiguous, cross-cutting, roadmap-driven, or documentation-heavy tasks.
3. Designate exactly one lead agent. Other agents act as reviewers or contributors, not parallel owners of the same file set.
4. If a task crosses multiple primary domains, the lead agent must name the required reviewers before implementation begins.
5. If a task changes a shared contract, the lead may implement the change, but the owning specialist still approves the contract shape.

## Shared Workflow

The repository already uses a research -> plan -> implement -> validate pattern in `spec/.prompt/README.md`. The agent team should follow the same workflow for non-trivial work.

### 1. Intake

- State the user goal in repository terms.
- Identify impacted modules, invariants, and likely reviewers.
- Decide whether the task is local to one module or cross-cutting.

### 2. Research

- Create or update `research.md` when the area is unfamiliar, risky, or architectural.
- Capture existing extension points, constraints, and compatibility risks.
- Document any relevant SOLID concerns before proposing changes.

### 3. Planning

- Create or update `plan.md` before substantial changes.
- Include scope, non-goals, impacted modules, validation strategy, and acceptance criteria.
- For cross-module work, explicitly assign the lead and reviewers in the plan.

### 4. Implementation

- Keep edits inside the owning module when possible.
- Prefer extension points and adapters over widening central abstractions.
- Update `progress.md` as meaningful milestones are completed.

### 5. Validation

- Run the narrowest relevant checks first, then broaden as needed.
- Record the commands or manual checks performed in `validation.md` or the progress log.
- Call out any residual risk or untested path explicitly.

### 6. Closure

- Ensure spec docs, module README files, and progress artifacts match the final implementation.
- Summarize ownership-sensitive decisions so the next agent can continue without rediscovery.

## Handoff Contract

Every handoff between agents should include the following:

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

If an agent cannot complete the task, it should still leave a partial handoff in this format so the next agent inherits context instead of re-discovering it.

## Review Gates

- `@core-architect` reviews all changes to shared API/SPIs, docking structure, layout serialization, or restore semantics.
- `@feature-dev` reviews new content factories, content state adapters, and dock integration changes in feature modules.
- `@ops-engineer` reviews Maven, dependency, publishing, settings persistence, and sample-app changes.
- `@auth-specialist` reviews all security-sensitive flows, tokens, secret stores, and session persistence.
- `@ui-ux-designer` reviews theme behavior, CSS, spacing, focus states, hover/active states, and accessibility-sensitive UI work.
- `@spec-steward` reviews plans, progress docs, roadmap updates, cross-cutting acceptance criteria, and repository-level documentation.
- `@qa-engineer` reviews test infrastructure changes, headless profile updates, new module test scaffolding, and validates that bug fixes include regression tests.

## Definition of Done

A task is done only when all of the following are true:

- The lead agent kept the change within the correct ownership boundary or documented why a cross-boundary edit was necessary.
- Required reviewers were consulted for ownership-sensitive changes.
- Relevant automated checks or manual validation steps were recorded.
- Specs, plans, progress logs, and README files reflect the final state of the change.
- Public API/SPI changes include updated documentation and compatibility notes.
- Session, theme, and settings related behavior is validated when affected.

## Agent Activation Guide

These agent roles are designed to work with Claude Code (or similar AI coding assistants). To activate a specific agent persona in a conversation:

1. **Direct invocation**: Start a conversation or prompt with the agent handle, e.g., _"As @core-architect, review this change to DockManager."_
2. **Task-based routing**: Describe the task and let the routing rules above determine the lead. The human operator or `@spec-steward` decides when routing is ambiguous.
3. **Multi-agent review**: For cross-cutting changes, run separate conversations per reviewer role, e.g., one as `@core-architect` for API shape and another as `@ui-ux-designer` for visual polish.
4. **Context loading**: Each agent conversation should include (or reference) the agent's spec file from `spec/agents/<role>.md` plus the shared operating model (`spec/agents/README.md`). CLAUDE.md is loaded automatically.

Agents are stateless between conversations. Use the handoff contract (below) and `progress.md` artifacts to carry context across sessions.

## Escalation & Conflict Resolution

When agents (or the humans operating them) disagree on approach:

1. **Domain authority wins within its boundary.** If `@auth-specialist` and `@feature-dev` disagree on how to store a session token, `@auth-specialist` decides.
2. **Cross-boundary disputes go to `@spec-steward`** for mediation. The steward documents the trade-offs and proposes a resolution.
3. **If the steward cannot resolve**, the decision escalates to the human project owner. The steward captures the decision and rationale in the relevant `plan.md`.
4. **Architectural vetoes**: `@core-architect` can veto changes that break API compatibility or violate SOLID principles. The veto must include a written rationale and a suggested alternative.

## Priority & Severity Classification

All agents use this shared urgency language when triaging work:

| Level | Label | Description | Response Expectation |
| --- | --- | --- | --- |
| P0 | **Critical** | Build broken, security vulnerability, data loss risk, or blocking release | Immediate — drop current work |
| P1 | **High** | Regression in core functionality, test suite broken, or user-facing bug | Next task — complete current work first |
| P2 | **Normal** | Feature work, non-critical improvements, spec updates | Planned — fits into normal workflow |
| P3 | **Low** | Polish, minor docs fixes, nice-to-have improvements | Backlog — pick up when convenient |

When filing or routing work, include the priority level so agents can sequence appropriately.

## Branching & Concurrent Work Strategy

When multiple agents work in parallel:

1. **One branch per agent per task.** Branch from `main` using the pattern `<agent-handle>/<short-description>` (e.g., `feature-dev/tree-search-overlay`).
2. **No concurrent edits to the same file** without an explicit handoff note naming who owns the file for that task.
3. **Merge order**: Agents that change shared contracts (`api`, `docks`, `settings-api`) merge first. Downstream modules rebase onto the updated `main`.
4. **Conflict resolution**: If two branches conflict, the agent whose changes are closer to a shared contract resolves the conflict. If both are at the same level, `@spec-steward` assigns resolution.
5. **Short-lived branches**: Branches should be merged or rebased within the scope of one task. Long-lived feature branches are an anti-pattern.

## Anti-Patterns To Avoid

- Editing central docking abstractions when an existing extension point would solve the problem.
- Shipping a new dockable feature without a `ContentFactory`, `ContentStateAdapter`, or restore validation plan.
- Changing spec documents after implementation without recording the reason for divergence.
- Treating UI review as optional for interaction-heavy changes.
- Treating build green status as sufficient validation for session, theme, or authentication behavior.
