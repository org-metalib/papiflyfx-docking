# Ribbon 5 — Multi-Perspective Review Plans

**Initiative:** Ribbon design and implementation review  
**Date:** 2026-04-23  
**Planning Lead:** @spec-steward  
**Scope:** Review-only. No implementation is authorized by this spec. Any follow-up work must open a separate plan document.

## Goal

Produce a structured, multi-perspective review of the current ribbon design and implementation so the team can decide where to invest in the next iteration. Each perspective in this directory is owned by one specialist role and focuses on the questions that role is accountable for. Findings feed into a consolidated action list that a future `ribbon-6` plan can convert into concrete work.

## Current Baseline (inputs to every review)

Prior ribbon specs in `spec/papiflyfx-docking-docks/`:

1. `2026-04-19-0-ribbon` — Iteration 1. Introduced the command/ribbon SPI, runtime shell, adaptive sizing, QAT, contextual tabs, and session persistence. Phase 5 complete.
2. `2026-04-20-0-ribbon-2` — Iteration 2. Decoupling and hardening: removed UI leakage from command contracts, introduced canonical command identity, typed capability resolution, and serializer validation. Compatibility was explicitly not a concern.
3. `2026-04-23-0-ribbon-3` — Targeted fix for label/caption clipping in SamplesApp ribbon shell.
4. `2026-04-23-0-ribbon-4` — Planned `GitHub Ribbon` and `Hugo Ribbon` SamplesApp demos. Implementation pending.

Live implementation surface:

1. API/SPI — `papiflyfx-docking-api/src/main/java/org/metalib/papifly/fx/api/ribbon/`
   - `PapiflyCommand`, `BoolState`/`MutableBoolState`, `RibbonProvider`, `RibbonContext`, `RibbonContextAttributes`, `RibbonTabSpec`, `RibbonGroupSpec`, `RibbonButtonSpec`, `RibbonToggleSpec`, `RibbonSplitButtonSpec`, `RibbonMenuSpec`, `RibbonControlSpec`, `RibbonIconHandle`.
2. Runtime — `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/`
   - `Ribbon` (438 LOC), `RibbonManager` (523 LOC), `RibbonGroup` (515 LOC), `RibbonControlFactory` (410 LOC), `RibbonDockHost` (91 LOC), `RibbonTabStrip` (148 LOC), `QuickAccessToolbar`, `CommandRegistry` (196 LOC), `RibbonLayoutTelemetry*`, `RibbonSessionCodec`, `RibbonSessionStateContributor`, `RibbonIconLoader` (233 LOC), `RibbonThemeSupport`, `JavaFxCommandBindings`, `RibbonGroupSizeMode`.
3. Providers — `GitHubRibbonProvider`, `HugoRibbonProvider`, `SampleRibbonProvider`.
4. Integration — `DockManager.trackRibbonContext(...)`, `DockManager.syncRibbonContextFromTree(...)`, `DockManager.resolveActiveRibbonLeaf(...)`; `ContentStateRegistry`; `DockSessionStateContributor` hook.
5. Resources — `papiflyfx-docking-docks/src/main/resources/org/metalib/papifly/fx/docks/ribbon/ribbon.css`.
6. Tests — `CommandRegistryTest`, `RibbonManagerTest`, `RibbonAdaptiveLayoutFxTest`, `RibbonSessionPersistenceFxTest`, `RibbonContextResolutionFxTest`, `RibbonCommandRegistryFxTest`.

## Review Plans In This Directory

| File | Owning Role | Focus |
| --- | --- | --- |
| [`review-core-architect.md`](review-core-architect.md) | `@core-architect` | API/SPI shape, command identity, refresh lifecycle, session format, SOLID boundaries |
| [`review-feature-dev.md`](review-feature-dev.md) | `@feature-dev` | Provider onboarding ergonomics, capability resolution, contextual tab heuristics, command lifecycle in feature modules |
| [`review-ops-engineer.md`](review-ops-engineer.md) | `@ops-engineer` | Build/Maven/module boundaries, sample coverage, persistence wiring, dependency surface, headless profile health |
| [`review-ui-ux-designer.md`](review-ui-ux-designer.md) | `@ui-ux-designer` | Visual system, CSS tokens, adaptive layout behavior, clipping/legibility, focus/hover/disabled states, accessibility |
| [`review-qa-engineer.md`](review-qa-engineer.md) | `@qa-engineer` | Test coverage gaps, determinism under headless, regression fixtures, benchmark strategy, flakiness risks |
| [`review-spec-steward.md`](review-spec-steward.md) | `@spec-steward` | Spec alignment, roadmap continuity, doc debt, handoff traceability, acceptance-criteria quality |

Each file is self-contained and can be executed by its owning specialist without reading the others. A concluding synthesis should be added as `findings.md` once all reviews are complete.

## Workflow

1. `@spec-steward` seeds this directory (this document, the six review-plan stubs).
2. Each specialist runs their own review against the baseline above and records findings in the corresponding file's `Findings` section (initially empty).
3. Reviewers file cross-cutting issues in the sibling file whose role owns the fix, not inside their own file, so ownership stays clear.
4. After all six reviews land, `@spec-steward` consolidates into `findings.md` with severity, ownership, and recommended follow-up plans.
5. Only then a new `ribbon-6` plan (or targeted fix plans) is authored.

## Scope And Guardrails

### In scope

1. Reading existing ribbon code, CSS, specs, and tests.
2. Comparing the implementation to the `ribbon-1` and `ribbon-2` plan invariants.
3. Recording concrete, file/line-anchored findings.
4. Proposing follow-up work items, each tagged with priority (P0–P3) and the role that should lead the fix.

### Out of scope

1. Editing production code under `papiflyfx-docking-*` modules.
2. Changing the public ribbon API/SPI.
3. Writing a new implementation plan; the outcome of this directory is a review, not an implementation.
4. Triggering real GitHub or Hugo integrations.

## Handoff Snapshot

Lead Agent: `@spec-steward`  
Task Scope: organize a multi-perspective ribbon review under `spec/papiflyfx-docking-docks/2026-04-23-0-ribbon-5`  
Impacted Modules: `spec/**` only  
Files Changed:

- `spec/papiflyfx-docking-docks/2026-04-23-0-ribbon-5/README.md`
- `spec/papiflyfx-docking-docks/2026-04-23-0-ribbon-5/review-core-architect.md`
- `spec/papiflyfx-docking-docks/2026-04-23-0-ribbon-5/review-feature-dev.md`
- `spec/papiflyfx-docking-docks/2026-04-23-0-ribbon-5/review-ops-engineer.md`
- `spec/papiflyfx-docking-docks/2026-04-23-0-ribbon-5/review-ui-ux-designer.md`
- `spec/papiflyfx-docking-docks/2026-04-23-0-ribbon-5/review-qa-engineer.md`
- `spec/papiflyfx-docking-docks/2026-04-23-0-ribbon-5/review-spec-steward.md`

Key Invariants:

- Review only; no production code changes.
- Each review plan is self-contained and anchored in the current implementation.
- Findings must cite files and, where useful, methods or line numbers.
- No public API or session format changes are authorized by this spec.

Validation Performed: source and spec inspection only  
Open Risks / Follow-ups: specialists should coordinate on overlapping findings through the cross-reference convention in each plan  
Required Reviewer: `@spec-steward`
