# PapiflyFX Docking Agent Playbook

This playbook turns the repository's agent model into a practical day-to-day workflow. Use it together with [spec/agents/README.md](README.md), [prompts.md](prompts.md), [cheatsheet.md](cheatsheet.md), and the role files in this directory.

## Purpose

- Choose the right lead agent quickly.
- Keep cross-cutting work coordinated without overlapping file ownership.
- Standardize prompts, handoffs, validation, and closure.
- Reduce re-discovery when work moves across conversations or tools.

## Non-Negotiable Rules

1. Assign exactly one lead agent per task.
2. Name required reviewers before implementation starts.
3. Do not let two agents edit the same file concurrently without an explicit handoff.
4. Use the spec-first flow for non-trivial work: research, plan, implement, validate.
5. Require a regression test for bug fixes unless the lead documents why one is not feasible.
6. Treat build green status as necessary but not sufficient when session, theme, settings, or auth behavior changes.

## Quick Routing

Use this table to pick the lead agent first.

| If the task is mainly about... | Lead agent | Typical reviewers |
| --- | --- | --- |
| Docking core, layout model, restore semantics, shared API/SPIs | `@core-architect` | `@qa-engineer`, `@ui-ux-designer` when UI is visible |
| Feature modules (`code`, `tree`, `media`, `hugo`, `github`), new dockable content | `@feature-dev` | `@qa-engineer`, `@ui-ux-designer`, `@core-architect` if contracts change |
| Maven, settings runtime, samples, dependency/plugin updates, release readiness | `@ops-engineer` | `@qa-engineer`, `@auth-specialist` for secret-store concerns |
| Login, IDPs, sessions, tokens, secure storage | `@auth-specialist` | `@qa-engineer`, `@ui-ux-designer`, `@ops-engineer` when samples/settings move |
| Theme API, CSS, spacing, interaction polish, accessibility | `@ui-ux-designer` | Owning domain agent, `@qa-engineer` for interaction regressions |
| UI Standardization across all modules | `@ui-ux-designer` | `@spec-steward`, `@feature-dev`, `@core-architect`, `@qa-engineer` |
| Test strategy, test infra, coverage gaps, flakiness, headless profile issues | `@qa-engineer` | Owning domain agent, `@ops-engineer` for build wiring |
| Ambiguous, roadmap-driven, documentation-heavy, or cross-cutting work | `@spec-steward` | Owning domain agent plus any triggered reviewers |

If two rows both apply, start with `@spec-steward` and make the routing decision explicit in the plan.

## Task Matrix

| Task type | Lead | Required reviewers | Required artifacts | Minimum validation |
| --- | --- | --- | --- | --- |
| Single-module bug fix in a feature module | `@feature-dev` | `@qa-engineer` | Update existing `progress.md` or create task note if non-trivial | Targeted module test and regression scenario |
| New dockable content or restorable content flow | `@feature-dev` | `@qa-engineer`, `@core-architect` if contracts change, `@ui-ux-designer` for user-facing UI | `research.md`, `plan.md`, `progress.md`, `validation.md` | Factory + state adapter wiring, restore round-trip, theme check |
| Layout persistence, floating window, minimize/maximize, or shared docking behavior | `@core-architect` | `@qa-engineer`, `@ui-ux-designer` if visuals change | `research.md` when risky, `plan.md`, `progress.md`, `validation.md` | Targeted core tests plus manual or automated restore validation |
| Public API/SPI change | Owning domain agent | `@core-architect`, `@spec-steward` | `plan.md`, compatibility notes, README/spec updates, `validation.md` | Build/tests of impacted modules and documentation update |
| Login provider, session lifecycle, secret handling | `@auth-specialist` | `@qa-engineer`, `@ui-ux-designer`, `@ops-engineer` when settings or samples change | `research.md`, `plan.md`, `progress.md`, `validation.md` | Negative-path tests, secure storage review, restore/session validation |
| Settings runtime or persistence format change | `@ops-engineer` | `@qa-engineer`, `@auth-specialist` if secrets are involved | `plan.md`, compatibility notes, `validation.md` | Backward-compatibility check and focused module tests |
| Sample-app wiring or demo coverage | `@ops-engineer` | Owning domain agent, `@qa-engineer` | `progress.md`, `validation.md` | Sample run path or smoke test update |
| Theme/CSS polish in one existing module | `@ui-ux-designer` | Owning domain agent, `@qa-engineer` if behavior changes | `progress.md` for scoped work; full spec set if cross-module | Visual verification in affected states and targeted tests if interaction changed |
| UI Standardization across all modules | `@ui-ux-designer` | `@spec-steward`, `@feature-dev`, `@core-architect`, `@qa-engineer` | `research.md`, `plan.md`, `progress.md`, `validation.md` | Consistent CSS variables, 100% theme binding coverage, shared component reuse |
| Test infrastructure or headless stability work | `@qa-engineer` | `@ops-engineer`, owning domain agent | `plan.md`, `progress.md`, `validation.md` | Headless rerun across affected modules |
| Roadmap, plans, delivery tracking, cross-cutting documentation | `@spec-steward` | Owning domain agent | Relevant `research.md` or `plan.md`, doc updates | Consistency review against current code and specs |

## Standard Operating Loop

Follow this loop for all P0, P1, and most P2 work.

### 1. Intake

- Restate the request in repository terms.
- Set priority: `P0`, `P1`, `P2`, or `P3`.
- Name the lead agent.
- Name required reviewers.
- List impacted modules.
- List key invariants that must not break.

### 2. Research

- Capture existing extension points before proposing central abstraction changes.
- Note compatibility risks for API, session, settings, and theme behavior.
- Create or update `research.md` when the area is unfamiliar, risky, or architectural.

### 3. Plan

- Record scope, non-goals, affected modules, validation strategy, and acceptance criteria.
- Explicitly state whether the task uses the fast path or full spec-first path.
- For cross-cutting work, include lead and reviewer assignments in `plan.md`.

### 4. Implement

- Keep changes inside the lead agent's ownership boundary when possible.
- Use extension points before widening shared contracts.
- Update `progress.md` at meaningful milestones.
- If the plan changes, update the spec before closing the task.

### 5. Review

- Ask reviewers to inspect only the slice they own.
- Resolve disagreements by domain authority first, then `@spec-steward`, then the human owner.
- Do not ask reviewers to re-solve the task from scratch.

### 6. Validate

- Run the narrowest relevant checks first, then broaden only if needed.
- Record exact commands or manual checks in `validation.md` or the progress log.
- Call out residual risks and untested paths explicitly.

### 7. Close

- Ensure README and spec docs match the final implementation.
- Leave a handoff note even when the task is complete.
- Make the next step obvious for the next conversation.

## Fast Path

Use the fast path only when all of the following are true:

- The task is isolated to one module or one documentation area.
- No public API/SPI, session format, auth behavior, theme system, build wiring, or test infrastructure changes are involved.
- The validation scope is obvious and small.
- No additional reviewer is required beyond a possible quick QA check.

Fast path rules:

- You may skip a separate `research.md`.
- You may keep the plan inline in the task note or `progress.md`.
- You still need a lead, validation record, and handoff summary.

## Runbooks By Work Type

### Single-Module Bug Fix

1. Route to the owning domain agent.
2. Have `@qa-engineer` define or confirm the regression scenario.
3. Implement the fix in the owning module only.
4. Run the narrowest module test set first.
5. Close with validation notes and any residual risk.

### New Dockable Content

1. Lead with `@feature-dev`.
2. Confirm `FACTORY_ID`, `ContentFactory`, `ContentStateAdapter`, and registration strategy.
3. Involve `@core-architect` only if the shared contracts need to move.
4. Involve `@ui-ux-designer` for theme and interaction review.
5. Involve `@qa-engineer` for restore round-trip coverage.
6. If the feature should appear in demos, ask `@ops-engineer` to update samples.

### Shared Contract Or Layout Change

1. Lead with `@core-architect`.
2. Start with research on extension points and compatibility risk.
3. Document session and layout invariants in `plan.md`.
4. Review contract shape before downstream module edits spread.
5. Validate serialization or restore behavior directly, not just compile status.

### Auth, IDP, Session, Or Secret Change

1. Lead with `@auth-specialist`.
2. Treat security defaults and negative cases as first-class acceptance criteria.
3. Involve `@ui-ux-designer` for login flow polish.
4. Involve `@ops-engineer` if settings persistence, secret store backends, or samples change.
5. Require negative-path validation such as expired token, invalid secret, or failed restore handling.

### Build, Settings, Or Samples Change

1. Lead with `@ops-engineer`.
2. Keep dependency and plugin version management centralized in the parent `pom.xml`.
3. Check JSON compatibility when persistence formats move.
4. Update smoke coverage or sample wiring when new features need demonstration.
5. Ask `@qa-engineer` to validate headless reliability if tests or build wiring changed.

### Theme, CSS, Or UX Polish

1. Lead with `@ui-ux-designer` when the change is mainly visual or ergonomic.
2. Keep styling scoped to the local module pattern.
3. Ask the owning domain agent to confirm no functional contract was broken.
4. Ask `@qa-engineer` to verify interaction-sensitive regressions.
5. Validate hover, focus, active, detached, and theme-switched states when relevant.

### Cross-Module UI Standardization

Use this workflow for any task that changes visual tokens, shared controls, or density rules across multiple modules.

1. Lead with `@ui-ux-designer` for the audit and token-definition phase.
2. Keep `Theme` as the only runtime styling source. Project CSS variables and shared metrics from `Theme`; do not create a second theme state.
3. Add shared primitives in `papiflyfx-docking-api` first when the abstraction is lightweight enough to stay additive.
4. Ask `@core-architect` to review any shared API, SPI, or theme-helper change before feature modules adopt it.
5. Hand off module adoption to `@feature-dev` once shared primitives and token helpers are settled.
6. Keep shared controls drop-in compatible and preserve existing ids, style classes, and `bindThemeProperty(...)` entry points while refactoring.
7. Ask `@qa-engineer` to add layout/state assertions for spacing, hover, active, selected, and theme-switch cases before declaring the rollout stable.
8. Run `./mvnw -Dtestfx.headless=true test` as the closing validation step for the rollout, not as the first check.
9. Close with `@spec-steward` updating root docs, module READMEs, plan state, and any workflow guidance learned during the rollout.

Lessons learned from the first UI standardization pass:

- Put shared spacing, radii, and control heights behind `UiMetrics` instead of repeating module-local constants.
- Prefer shared popup surfaces and compact-control classes over per-module overlay CSS vocabularies.
- Validate JavaFX visual state transitions in FX tests rather than assuming mapper/unit tests are sufficient.
- Treat intermittent headless fork-start failures as infrastructure issues unless module-local reports show an actual test regression.

### Docs, Roadmap, Or Cross-Cutting Coordination

1. Lead with `@spec-steward`.
2. Identify the owning technical reviewer early.
3. Align acceptance criteria with current code, not stale plans.
4. Update repository-level docs and spec artifacts together.

## Prompt Templates

Use these as the default prompt shapes when handing work to each agent.

### Intake Prompt To `@spec-steward`

```text
As @spec-steward, intake this task for papiflyfx-docking:

Task:
<describe the request>

Return:
1. Priority (P0-P3)
2. Lead agent
3. Required reviewers
4. Impacted modules
5. Key invariants
6. Whether this is fast path or full spec-first
7. Required spec/doc artifacts
8. Suggested validation strategy
```

### Implementation Prompt To The Lead Agent

```text
As <lead-agent>, execute this approved task in papiflyfx-docking.

Task scope:
<scope>

Priority:
<P0-P3>

Impacted modules:
<modules>

Required reviewers:
<reviewers>

Key invariants:
<invariants>

Acceptance criteria:
<criteria>

Validation expectations:
<tests/manual checks>

Before closing, return the repository handoff contract with files changed and validation performed.
```

### Reviewer Prompt

```text
As <reviewer-agent>, review only your ownership slice for this change.

Change summary:
<summary>

Files/modules touched:
<files/modules>

Review focus:
<contract shape | UX polish | auth/security | test coverage | build/runtime>

Return:
1. Findings ordered by severity
2. Open risks
3. Required follow-ups before close
```

### QA Validation Prompt

```text
As @qa-engineer, validate this change for regression risk.

Scope:
<scope>

Impacted modules:
<modules>

Expected behavior:
<expected behavior>

Please identify:
1. Missing regression coverage
2. The narrowest relevant automated checks
3. Any remaining manual verification needs
4. Residual risk if merged as-is
```

### Closure Prompt To `@spec-steward`

```text
As @spec-steward, close out this task.

Implementation summary:
<summary>

Validation performed:
<commands/checks>

Docs/spec updates:
<updated docs>

Open risks:
<risks or none>

Confirm whether the task meets the repository definition of done and list any final follow-ups.
```

## Handoff Contract

Use this exact structure whenever work moves between agents or conversations.

```md
Lead Agent:
Priority:
Task Scope:
Impacted Modules:
Files Changed:
Key Invariants:
Validation Performed:
Open Risks / Follow-ups:
Required Reviewer:
```

## File Ownership And Parallelism

- Use one branch per agent per task if work is actually being split across branches.
- Shared-contract branches merge first. Downstream branches rebase after the contract settles.
- If two agents need the same file, stop and assign file ownership explicitly before editing resumes.
- When a downstream agent depends on upstream contract work, do not start implementation against guessed APIs.

## Definition Of Ready

The lead agent should not start implementation until these are clear:

- The task has a lead.
- The priority is set.
- Reviewers are named.
- Impacted modules are listed.
- The acceptance criteria are specific enough to validate.
- Any compatibility-sensitive invariant is written down.

## Definition Of Done Checklist

- The correct lead owned the change.
- Required reviewers were consulted.
- Validation was performed and recorded.
- Specs and README files were updated when behavior, APIs, or plans changed.
- Bug fixes include regression coverage or an explicit explanation for the gap.
- A handoff note exists for the next conversation.

## Example Routing Decisions

- "Fix detached window layout not restoring after restart" -> `@core-architect` lead, `@qa-engineer` reviewer, `@ui-ux-designer` if window chrome changes.
- "Add searchable outline panel to the tree module" -> `@feature-dev` lead, `@qa-engineer` and `@ui-ux-designer` reviewers, `@core-architect` only if shared contracts move.
- "Add GitHub login provider with persisted session" -> `@auth-specialist` lead, `@qa-engineer` and `@ui-ux-designer` reviewers, `@ops-engineer` if samples or settings storage are updated.
- "Stabilize headless TestFX failures in samples" -> `@qa-engineer` lead, `@ops-engineer` reviewer, owning module agent if the fix touches product code.
- "Roll out a new shared spacing/token system across code, tree, github, media, and hugo" -> `@ui-ux-designer` lead for standards and shared primitives, `@feature-dev` lead for module adoption, `@qa-engineer` lead for validation, `@spec-steward` lead for closure docs.
