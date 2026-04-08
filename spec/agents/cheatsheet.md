# PapiflyFX Docking Prompt Cheat Sheet

This is the short operator version of the agent prompt system. Use it when you want the fastest route from task to the right Claude/Codex session. For the full prompt pack, see [prompts.md](prompts.md). For routing and workflow rules, see [playbook.md](playbook.md).

## Quick Picker

| If you need to... | Use this agent | Then usually involve |
| --- | --- | --- |
| Route an unclear or cross-cutting task | `@spec-steward` | Owning domain reviewer |
| Fix docking core, layout restore, floating/minimize behavior, or shared API/SPIs | `@core-architect` | `@qa-engineer`, `@ui-ux-designer` if visible UI changes |
| Build or fix a content module feature | `@feature-dev` | `@qa-engineer`, `@ui-ux-designer`, `@core-architect` if contracts move |
| Fix Maven, settings runtime, samples, or release-readiness issues | `@ops-engineer` | `@qa-engineer` |
| Change login, session, token, IDP, or secret handling | `@auth-specialist` | `@qa-engineer`, `@ui-ux-designer` |
| Polish theme, CSS, spacing, or interaction UX | `@ui-ux-designer` | Owning domain agent, `@qa-engineer` if behavior changes |
| Validate regressions, flakiness, or headless test issues | `@qa-engineer` | Owning domain agent, `@ops-engineer` if build wiring changed |
| Close the loop on docs, plans, and done criteria | `@spec-steward` | Owning domain reviewer |

## Default Flow

1. Paste the `@spec-steward` intake prompt if the lead is not obvious.
2. Paste the lead prompt for the owning agent.
3. Paste one focused reviewer prompt per required review gate.
4. Paste the QA validation prompt for regression coverage.
5. Paste the `@spec-steward` closure prompt when the task is ready to close.

## Fastest Starters

These are the shortest copy-paste prompts for the most common session types.

### Route This Task

Use when the task is ambiguous or spans multiple modules.

```text
As @spec-steward, intake this PapiflyFX Docking task.

Read:
- AGENTS.md
- spec/agents/README.md
- spec/agents/playbook.md
- spec/agents/spec-steward.md

Task:
<task>

Return:
1. Priority (`P0`-`P3`)
2. Lead agent
3. Required reviewers
4. Impacted modules
5. Key invariants
6. Fast path vs full spec-first workflow
7. Required spec/doc artifacts
8. Suggested validation strategy
```

### Implement A Feature-Module Change

Use for `code`, `tree`, `media`, `hugo`, and `github` work.

```text
As @feature-dev, handle this PapiflyFX Docking feature task.

Read:
- AGENTS.md
- spec/agents/README.md
- spec/agents/playbook.md
- spec/agents/feature-dev.md
- <spec-paths>

Task:
<task>

Priority:
<priority>

Required reviewers:
<required-reviewers>

Key invariants:
<key-invariants>

Acceptance criteria:
<acceptance-criteria>

Validation expectations:
<validation-expectations>

If implementation is appropriate, make the change and return the repository handoff block.
```

### Implement A Docking Core Or API Change

Use for `papiflyfx-docking-api` and `papiflyfx-docking-docks`.

```text
As @core-architect, handle this PapiflyFX Docking core task.

Read:
- AGENTS.md
- spec/agents/README.md
- spec/agents/playbook.md
- spec/agents/core-architect.md
- <spec-paths>

Task:
<task>

Priority:
<priority>

Required reviewers:
<required-reviewers>

Key invariants:
<key-invariants>

Acceptance criteria:
<acceptance-criteria>

Validation expectations:
<validation-expectations>

Check extension points before changing shared contracts. If implementation is appropriate, make the change and return the repository handoff block.
```

### Implement A Build, Settings, Or Samples Change

```text
As @ops-engineer, handle this PapiflyFX Docking build/runtime task.

Read:
- AGENTS.md
- spec/agents/README.md
- spec/agents/playbook.md
- spec/agents/ops-engineer.md
- <spec-paths>

Task:
<task>

Priority:
<priority>

Required reviewers:
<required-reviewers>

Key invariants:
<key-invariants>

Acceptance criteria:
<acceptance-criteria>

Validation expectations:
<validation-expectations>

Preserve dependency hygiene and persistence compatibility unless the plan explicitly says otherwise. If implementation is appropriate, make the change and return the repository handoff block.
```

### Implement An Auth Or Session Change

```text
As @auth-specialist, handle this PapiflyFX Docking auth/security task.

Read:
- AGENTS.md
- spec/agents/README.md
- spec/agents/playbook.md
- spec/agents/auth-specialist.md
- <spec-paths>

Task:
<task>

Priority:
<priority>

Required reviewers:
<required-reviewers>

Key invariants:
<key-invariants>

Acceptance criteria:
<acceptance-criteria>

Validation expectations:
<validation-expectations>

Treat secure defaults and negative-path handling as first-class. If implementation is appropriate, make the change and return the repository handoff block.
```

### Implement A Theme Or UX Change

```text
As @ui-ux-designer, handle this PapiflyFX Docking UI/UX task.

Read:
- AGENTS.md
- spec/agents/README.md
- spec/agents/playbook.md
- spec/agents/ui-ux-designer.md
- <spec-paths>

Task:
<task>

Priority:
<priority>

Required reviewers:
<required-reviewers>

Key invariants:
<key-invariants>

Acceptance criteria:
<acceptance-criteria>

Validation expectations:
<validation-expectations>

Keep styling scoped and verify the affected interaction states. If implementation is appropriate, make the change and return the repository handoff block.
```

### Review Shared Contracts Or Restore Semantics

```text
As @core-architect, review this change only for shared API/SPI shape, docking structure, and restore semantics.

Read:
- AGENTS.md
- spec/agents/README.md
- spec/agents/playbook.md
- spec/agents/core-architect.md
- <spec-paths>

Change summary:
<change-summary>

Files:
<files-or-paths>

Return findings ordered by severity, plus any required follow-ups before close.
```

### Review UX And Visual Polish

```text
As @ui-ux-designer, review this change only for theme behavior, spacing, state feedback, ergonomics, and accessibility.

Read:
- AGENTS.md
- spec/agents/README.md
- spec/agents/playbook.md
- spec/agents/ui-ux-designer.md
- <spec-paths>

Change summary:
<change-summary>

Files:
<files-or-paths>

Return findings ordered by severity, plus any required UX follow-ups before close.
```

### Validate Regression Risk

```text
As @qa-engineer, validate this PapiflyFX Docking change for regression risk.

Read:
- AGENTS.md
- spec/agents/README.md
- spec/agents/playbook.md
- spec/agents/qa-engineer.md
- <spec-paths>

Change summary:
<change-summary>

Files:
<files-or-paths>

Expected behavior:
<acceptance-criteria>

Return:
1. Missing regression coverage
2. The narrowest relevant automated checks
3. Manual verification still needed
4. Residual risk if merged as-is
```

### Close The Task

```text
As @spec-steward, close this PapiflyFX Docking task against the repository definition of done.

Read:
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
1. Whether the task is done
2. Missing review gates, docs, or tests
3. Final follow-ups
4. A clean closure handoff block
```

## Common Scenarios

- Bug fix in `tree`, `media`, `github`, `code`, or `hugo`: start with `@feature-dev`, then `@qa-engineer`.
- Session restore, floating window, minimize/maximize bug: start with `@core-architect`, then `@qa-engineer`.
- New dockable content: start with `@feature-dev`; add `@core-architect` if contracts move and `@ui-ux-designer` for user-facing polish.
- Login provider or session storage change: start with `@auth-specialist`; add `@qa-engineer` and usually `@ui-ux-designer`.
- Build break, dependency drift, sample wiring, settings runtime issue: start with `@ops-engineer`.
- Ambiguous roadmap or cross-module initiative: start with `@spec-steward`.

## Shared Handoff Block

Paste this at the end of any implementation or closure session:

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
