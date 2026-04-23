# Ribbon 5 Review — Spec & Delivery Steward Perspective

**Priority:** P2 (Normal)  
**Lead Agent:** `@spec-steward`  
**Required Reviewers:** `@core-architect`, `@feature-dev`, `@ops-engineer`, `@ui-ux-designer`, `@qa-engineer`  
**Workflow:** review-only; emit findings into the `Findings` section at the bottom of this file. Consolidate the full review in `findings.md` after all six plans are complete.

## Goal

Assess the ribbon initiative's documentation, roadmap continuity, handoff hygiene, and acceptance-criteria quality across the four prior spec directories (`ribbon`, `ribbon-2`, `ribbon-3`, `ribbon-4`). Identify spec drift from implementation, redundant artifacts, missing READMEs, and weak acceptance criteria.

## Scope

### In scope

1. Prior spec directories:
   - `spec/papiflyfx-docking-docks/2026-04-19-0-ribbon/`
   - `spec/papiflyfx-docking-docks/2026-04-20-0-ribbon-2/`
   - `spec/papiflyfx-docking-docks/2026-04-23-0-ribbon-3/`
   - `spec/papiflyfx-docking-docks/2026-04-23-0-ribbon-4/`
2. `spec/papiflyfx-docking-docks/README.md` and `IMPLEMENTATION_PLAN.md`.
3. `spec/agents/README.md` (operating model) and agent role specs under `spec/agents/`.
4. `papiflyfx-docking-docks/README.md` module-level doc.
5. Any `spec/ui-standards/` entries relevant to ribbon.
6. `AGENTS.md` and `CLAUDE.md` sections that mention ribbon, Quick Access Toolbar, or `RibbonDockHost`.

### Out of scope

1. Implementation code review (already covered by the other five plans).
2. Writing the ribbon-6 plan (that is downstream of this review's conclusions).

## Review Questions

### A. Spec-to-implementation alignment

1. For `ribbon-1`, compare each phase's acceptance criteria to the shipped behavior. Flag anything marked done in `progress.md` that does not match the current code.
2. For `ribbon-2`, confirm each of the nine in-scope items landed (decoupling, typed capability, command registry, conflict diagnostics, adaptive layout improvements, reduction priority docs, persistence validation, tests, breaking cleanup).
3. For `ribbon-3`, confirm whether the clipping fix landed with the recommended test, and whether the CSS token changes stayed within the shared vocabulary.
4. For `ribbon-4`, confirm that the `GitHub Ribbon` and `Hugo Ribbon` samples are not yet implemented (current `progress.md` says 0%); decide whether this review should block on that work or proceed independently.

### B. Spec directory hygiene

1. Every spec directory should have a `README.md`, `plan.md`, `progress.md`, and (when applicable) `research.md` and `validation.md`. Flag any directory missing one of these that would be useful:
   - `ribbon-1` has `README.md`, `plan.md`, `progress.md`, `research-gemini.md` — is `research-gemini.md` the canonical research doc, or should it be renamed?
   - `ribbon-2` has an extra `adr-0001-svg-icons.md` — confirm it is linked from the plan and README.
   - `ribbon-3` has `README.md`, `plan.md`, `progress.md` — sufficient for a targeted fix.
   - `ribbon-4` has an unusual `prompt.md` — is that artifact redundant with `README.md` + `plan.md`?
2. Does every plan document priority, lead agent, and required reviewers? Cross-check against the agent operating model.
3. Are handoff snapshots present and populated at the bottom of every plan/progress file?

### C. Roadmap continuity

1. Does `spec/papiflyfx-docking-docks/README.md` or `IMPLEMENTATION_PLAN.md` track the ribbon initiative as a named stream with known next steps? If not, propose where to record it.
2. Is the roadmap in `spec/papiflyfx-docking-roadmap/` aware of the ribbon initiative? Confirm.
3. Is there a single source of truth for "ribbon status" that new contributors can read in under five minutes? Today that answer might be spread across four directories.

### D. Documentation debt

1. `papiflyfx-docking-docks/README.md` — does it describe the ribbon runtime (Ribbon, RibbonManager, RibbonDockHost) with the same care as drag/drop and floating windows?
2. `CLAUDE.md` — it mentions the ribbon briefly. Confirm the mention is correct and not stale (e.g., "Persist ribbon Quick Access Toolbar state through `RibbonManager#getQuickAccessCommandIds()`" line).
3. `AGENTS.md` — does it reference the ribbon SPI under `@core-architect`'s Focus Area (`org.metalib.papifly.fx.api.ribbon`)? Confirm present.
4. Is there a provider-authoring guide for feature-module owners? If not, propose where it should live (candidate: a dedicated `spec/papiflyfx-docking-docks/ribbon-provider-authoring.md` or an appendix in the docks README).

### E. Acceptance criteria quality

1. Rate each prior plan's acceptance criteria on whether they are:
   - observable (a reviewer can check them),
   - automated where possible,
   - worded unambiguously.
2. Flag any criterion that was subjective (e.g., "looks coherent", "works smoothly") and propose a more observable replacement.
3. Compare with the acceptance criteria format used by healthy plans elsewhere in `spec/` (e.g., `spec/ui-standards/` or `spec/papiflyfx-docking-code-lang-plugin/`).

### F. Handoff traceability

1. For each prior plan, locate the `Handoff Snapshot` block. Confirm each was populated at completion, not just at authoring.
2. Identify any plan that completed without an updated `progress.md` and `Validation Performed` line.
3. Note any overlap between adjacent plans (same spec noted in ribbon-1 and ribbon-2) that could cause confusion.

### G. Cross-cutting governance

1. The operating model requires one lead per task and named reviewers for cross-cutting changes. Did `ribbon-2` (which crossed `api`, `docks`, `github`, `hugo`, `samples`) respect this?
2. Were shared-contract changes reviewed by `@core-architect` per the Review Gates?
3. Was the ribbon-3 clipping fix reviewed by `@ui-ux-designer` before landing? Confirm.

### H. Output of this review series

1. Define the format of the consolidation doc (`findings.md`) this series will produce. Proposed skeleton:

    ```md
    # Ribbon 5 — Consolidated Findings

    ## Critical (P0)
    ### F-<role>-<NN>: <title>
    - Source plan: <review-*.md>
    - Evidence, risk, proposed owner, proposed cost, dependency on other findings

    ## High (P1) ...
    ## Normal (P2) ...
    ## Low (P3) ...

    ## Recommended Follow-up Plans
    - <short plan name> — lead role, rough scope
    ```

2. Nominate the target date for consolidation (suggested: one week from the last specialist review).
3. Decide whether `findings.md` replaces or supplements a follow-up `ribbon-6/plan.md`.

## Review Procedure

1. Read each prior spec directory end-to-end.
2. For each directory, record: present artifacts, missing artifacts, alignment with implementation, handoff completeness.
3. Read `spec/papiflyfx-docking-docks/README.md`, `IMPLEMENTATION_PLAN.md`, and `papiflyfx-docking-docks/README.md`.
4. Sample `CLAUDE.md` and `AGENTS.md` ribbon references and verify accuracy.

## Deliverable

Populate the `Findings` section below using the common template:

```md
### F-<NN>: <short title>
**Severity:** P0|P1|P2|P3  
**Area:** <Alignment | Hygiene | Roadmap | Documentation | Acceptance | Handoff | Governance | Output>  
**Evidence:** <spec:path or doc excerpt>  
**Risk:** <what a new contributor hits>  
**Suggested follow-up:** <lead role, rough cost S/M/L>
```

Also draft a short `Consolidation Plan` section at the end that outlines how to assemble `findings.md` once all six plans report in.

## Validation

No automated validation. Cross-reference quality is the primary check; each finding should point to a specific spec path.

## Findings

_Not yet started._

## Consolidation Plan

_To be drafted once the other five review plans begin filing findings. Target format is sketched in review question H.1._

## Handoff Snapshot

Lead Agent: `@spec-steward`  
Task Scope: spec and delivery review of the ribbon initiative; plus ownership of the consolidation doc once peers file findings  
Impacted Modules: `spec/**` only  
Files Changed: this file (on completion); later, a `findings.md` sibling  
Key Invariants:

- no production or code-repo README changes
- findings must cite spec paths
- consolidation should not start until all six review plans have been filed

Validation Performed: spec inspection only  
Open Risks / Follow-ups: recorded as numbered findings and captured in `Consolidation Plan`  
Required Reviewer: all other five role owners must sign off on findings affecting their domain before consolidation
