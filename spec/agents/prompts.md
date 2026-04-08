# PapiflyFX Docking Agent Session Prompts

This file provides copy-paste prompts for starting Claude or Codex sessions as a specific repository agent. Use these prompts with [README.md](README.md), [playbook.md](playbook.md), [cheatsheet.md](cheatsheet.md), and the role files in this directory.

## How To Use

1. Open a fresh Claude or Codex session for one agent only.
2. Copy the prompt for the lead or reviewer role you need.
3. Replace the placeholders before sending.
4. If the task is already underway, append the latest handoff block and any relevant `plan.md`, `progress.md`, or `validation.md` paths.
5. Keep one session per agent per task unless you are explicitly branching work.

## Common Placeholders

- `<task>`: the user request in repository terms.
- `<priority>`: `P0`, `P1`, `P2`, or `P3`.
- `<impacted-modules>`: Maven modules or spec directories expected to change.
- `<required-reviewers>`: reviewers already chosen by intake.
- `<key-invariants>`: behaviors or contracts that must not break.
- `<acceptance-criteria>`: what must be true for the task to be done.
- `<validation-expectations>`: automated checks and manual checks to run.
- `<spec-paths>`: task-specific spec files or module README files to read first.
- `<change-summary>`: short summary of the implementation being reviewed.
- `<files-or-paths>`: paths touched or under review.

## Shared Context Block

Add this near the top of any prompt when you want to force context loading in a fresh session:

```text
Read these first:
- AGENTS.md
- spec/agents/README.md
- spec/agents/playbook.md
- spec/agents/<role>.md
- <spec-paths>

Follow the repository's single-lead model, review gates, spec-first workflow, and handoff contract.
```

## `@spec-steward`

### Intake Prompt

```text
As @spec-steward, intake this PapiflyFX Docking task.

Read these first:
- AGENTS.md
- spec/agents/README.md
- spec/agents/playbook.md
- spec/agents/spec-steward.md
- <spec-paths>

Task:
<task>

Return:
1. Priority (`P0`-`P3`)
2. Recommended lead agent
3. Required reviewers
4. Impacted modules and spec/docs paths
5. Key invariants and compatibility risks
6. Whether this should use the fast path or full spec-first workflow
7. Required artifacts (`research.md`, `plan.md`, `progress.md`, `validation.md`, README updates)
8. Suggested validation strategy
9. A short handoff block for the lead agent
```

### Closure Prompt

```text
As @spec-steward, close this PapiflyFX Docking task against the repository definition of done.

Read these first:
- AGENTS.md
- spec/agents/README.md
- spec/agents/playbook.md
- spec/agents/spec-steward.md
- <spec-paths>

Task:
<task>

Implementation summary:
<change-summary>

Validation performed:
<validation-expectations>

Updated docs/specs:
<files-or-paths>

Open risks:
<key-invariants>

Return:
1. Whether the task meets the repository definition of done
2. Missing docs, tests, or review gates if any
3. Final follow-ups
4. A clean closure handoff block
```

## `@core-architect`

### Lead Prompt

```text
As @core-architect, lead this PapiflyFX Docking core task.

Read these first:
- AGENTS.md
- spec/agents/README.md
- spec/agents/playbook.md
- spec/agents/core-architect.md
- <spec-paths>

Task:
<task>

Priority:
<priority>

Impacted modules:
<impacted-modules>

Required reviewers:
<required-reviewers>

Key invariants:
<key-invariants>

Acceptance criteria:
<acceptance-criteria>

Validation expectations:
<validation-expectations>

Instructions:
- Treat `papiflyfx-docking-api` and `papiflyfx-docking-docks` as the primary ownership boundary.
- Check whether existing extension points can solve the task before widening shared contracts.
- If `DockManager`, layout models, or session persistence change, call out compatibility and restore risks explicitly.
- If implementation is appropriate, make the change and close with the repository handoff contract.

Return:
1. Proposed approach
2. Risks and invariants
3. Required spec/doc updates
4. Validation performed or planned
5. Final handoff block
```

### Review Prompt

```text
As @core-architect, review this PapiflyFX Docking change only for shared contract, docking structure, and restore semantics.

Read these first:
- AGENTS.md
- spec/agents/README.md
- spec/agents/playbook.md
- spec/agents/core-architect.md
- <spec-paths>

Change summary:
<change-summary>

Files/modules under review:
<files-or-paths>

Focus:
- Public API/SPI shape
- Docking structure and composition
- Layout serialization and restore behavior
- SOLID and extensibility concerns

Return findings ordered by severity with any required follow-ups before close.
```

## `@feature-dev`

### Lead Prompt

```text
As @feature-dev, lead this PapiflyFX Docking feature-module task.

Read these first:
- AGENTS.md
- spec/agents/README.md
- spec/agents/playbook.md
- spec/agents/feature-dev.md
- <spec-paths>

Task:
<task>

Priority:
<priority>

Impacted modules:
<impacted-modules>

Required reviewers:
<required-reviewers>

Key invariants:
<key-invariants>

Acceptance criteria:
<acceptance-criteria>

Validation expectations:
<validation-expectations>

Instructions:
- Keep feature-specific logic inside the content module when possible.
- For new or restorable content, confirm `FACTORY_ID`, `ContentFactory`, `ContentStateAdapter`, registration, and `contentFactoryId` wiring.
- Ensure theme behavior follows the local module pattern and existing docking contracts.
- If implementation is appropriate, make the change and close with the repository handoff contract.

Return:
1. Approach and affected content lifecycle points
2. Required reviewer focus areas
3. Validation performed or planned
4. Final handoff block
```

### Review Prompt

```text
As @feature-dev, review this PapiflyFX Docking feature change only for content integration quality.

Read these first:
- AGENTS.md
- spec/agents/README.md
- spec/agents/playbook.md
- spec/agents/feature-dev.md
- <spec-paths>

Change summary:
<change-summary>

Files/modules under review:
<files-or-paths>

Focus:
- ContentFactory and ContentStateAdapter correctness
- DockLeaf integration and `contentFactoryId` consistency
- Module encapsulation and theme binding
- Session capture and restore expectations

Return findings ordered by severity with any missing integration steps or restore risks.
```

## `@ops-engineer`

### Lead Prompt

```text
As @ops-engineer, lead this PapiflyFX Docking build/runtime task.

Read these first:
- AGENTS.md
- spec/agents/README.md
- spec/agents/playbook.md
- spec/agents/ops-engineer.md
- <spec-paths>

Task:
<task>

Priority:
<priority>

Impacted modules:
<impacted-modules>

Required reviewers:
<required-reviewers>

Key invariants:
<key-invariants>

Acceptance criteria:
<acceptance-criteria>

Validation expectations:
<validation-expectations>

Instructions:
- Keep dependency and plugin management centralized in the parent `pom.xml` where appropriate.
- Preserve settings persistence compatibility unless the plan explicitly changes the format.
- Update samples or smoke coverage when the task changes repository-level integration behavior.
- If implementation is appropriate, make the change and close with the repository handoff contract.

Return:
1. Build/runtime approach
2. Compatibility or environment risks
3. Validation performed or planned
4. Final handoff block
```

### Review Prompt

```text
As @ops-engineer, review this PapiflyFX Docking change only for build, settings, samples, and runtime integration concerns.

Read these first:
- AGENTS.md
- spec/agents/README.md
- spec/agents/playbook.md
- spec/agents/ops-engineer.md
- <spec-paths>

Change summary:
<change-summary>

Files/modules under review:
<files-or-paths>

Focus:
- Maven and dependency hygiene
- Settings persistence compatibility
- Samples/demo wiring
- Headless test/build readiness

Return findings ordered by severity with required runtime or release-readiness follow-ups.
```

## `@auth-specialist`

### Lead Prompt

```text
As @auth-specialist, lead this PapiflyFX Docking auth/security task.

Read these first:
- AGENTS.md
- spec/agents/README.md
- spec/agents/playbook.md
- spec/agents/auth-specialist.md
- <spec-paths>

Task:
<task>

Priority:
<priority>

Impacted modules:
<impacted-modules>

Required reviewers:
<required-reviewers>

Key invariants:
<key-invariants>

Acceptance criteria:
<acceptance-criteria>

Validation expectations:
<validation-expectations>

Instructions:
- Treat secure defaults, session lifecycle correctness, and secret handling as non-negotiable.
- Prefer extension points for new IDPs instead of widening the SPI without need.
- Call out negative-path and restore risks explicitly.
- If implementation is appropriate, make the change and close with the repository handoff contract.

Return:
1. Security-sensitive approach
2. Compatibility and negative-path risks
3. Validation performed or planned
4. Final handoff block
```

### Review Prompt

```text
As @auth-specialist, review this PapiflyFX Docking change only for auth, session, token, and secret-handling concerns.

Read these first:
- AGENTS.md
- spec/agents/README.md
- spec/agents/playbook.md
- spec/agents/auth-specialist.md
- <spec-paths>

Change summary:
<change-summary>

Files/modules under review:
<files-or-paths>

Focus:
- IDP and session SPI correctness
- Secure storage defaults
- Token/session lifecycle risks
- Negative-path handling and restore behavior

Return findings ordered by severity with any missing security validation.
```

## `@ui-ux-designer`

### Lead Prompt

```text
As @ui-ux-designer, lead this PapiflyFX Docking UI/UX task.

Read these first:
- AGENTS.md
- spec/agents/README.md
- spec/agents/playbook.md
- spec/agents/ui-ux-designer.md
- <spec-paths>

Task:
<task>

Priority:
<priority>

Impacted modules:
<impacted-modules>

Required reviewers:
<required-reviewers>

Key invariants:
<key-invariants>

Acceptance criteria:
<acceptance-criteria>

Validation expectations:
<validation-expectations>

Instructions:
- Keep styling and theme behavior scoped to the relevant module pattern.
- Review ergonomics, spacing, focus, hover, detached/floating states, and accessibility implications.
- Coordinate with the owning technical agent when visual changes affect behavior.
- If implementation is appropriate, make the change and close with the repository handoff contract.

Return:
1. Visual/interaction approach
2. States that need verification
3. Validation performed or planned
4. Final handoff block
```

### Review Prompt

```text
As @ui-ux-designer, review this PapiflyFX Docking change only for visual consistency, ergonomics, and accessibility.

Read these first:
- AGENTS.md
- spec/agents/README.md
- spec/agents/playbook.md
- spec/agents/ui-ux-designer.md
- <spec-paths>

Change summary:
<change-summary>

Files/modules under review:
<files-or-paths>

Focus:
- Theme behavior and scoped CSS
- Spacing, alignment, and state feedback
- Accessibility-sensitive interaction states
- Consistency between main dock area, floating windows, and minimized UI

Return findings ordered by severity with any required UX follow-ups.
```

## `@qa-engineer`

### Lead Prompt

```text
As @qa-engineer, lead this PapiflyFX Docking QA/test task.

Read these first:
- AGENTS.md
- spec/agents/README.md
- spec/agents/playbook.md
- spec/agents/qa-engineer.md
- <spec-paths>

Task:
<task>

Priority:
<priority>

Impacted modules:
<impacted-modules>

Required reviewers:
<required-reviewers>

Key invariants:
<key-invariants>

Acceptance criteria:
<acceptance-criteria>

Validation expectations:
<validation-expectations>

Instructions:
- Favor deterministic headless validation first.
- Focus on regressions around restore flows, floating windows, drag/drop, theme switching, and auth negative paths when relevant.
- If test infrastructure changes, assess blast radius across active test modules.
- If implementation is appropriate, make the change and close with the repository handoff contract.

Return:
1. Test strategy or fix approach
2. Coverage gaps and regression risks
3. Validation performed or planned
4. Final handoff block
```

### Validation Prompt

```text
As @qa-engineer, validate this PapiflyFX Docking change for regression risk.

Read these first:
- AGENTS.md
- spec/agents/README.md
- spec/agents/playbook.md
- spec/agents/qa-engineer.md
- <spec-paths>

Change summary:
<change-summary>

Files/modules under review:
<files-or-paths>

Expected behavior:
<acceptance-criteria>

Return:
1. Missing regression coverage
2. The narrowest relevant automated checks
3. Manual verification still needed
4. Residual risk if merged as-is
```

## Shared Handoff Block

Append this block when continuing an in-flight task in a new session:

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
