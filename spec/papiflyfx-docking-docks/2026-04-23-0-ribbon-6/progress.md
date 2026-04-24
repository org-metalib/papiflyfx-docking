# Progress - Ribbon 6 Compatibility Implementation

**Status:** Planning artifacts created and docs-only validation complete; implementation not started  
**Current Milestone:** Phase 0 - Planning And API Names pending  
**Priority:** P2 (Normal)  
**Planning Lead:** @spec-steward  
**Implementation Lead:** @core-architect  
**Required Reviewers:** @core-architect, @feature-dev, @ui-ux-designer, @qa-engineer, @spec-steward  
**Consult If Scope Expands:** @ops-engineer for Maven/TestFX, archetype, release, or session migration tooling

## Completion Summary

- Plan creation: 100%
- Prompt creation: 100%
- Phase 0 - Planning And API Names: 0%
- Phase 1 - Command And Boolean State Contracts: 0%
- Phase 2 - Control Strategy Model: 0%
- Phase 3 - RibbonManager Collaborators: 0%
- Phase 4 - Documentation, Migration, And Closure: 0%
- Validation: docs-only sanity complete

## Accomplishments

- [2026-04-24] Loaded @spec-steward role guidance because this task creates planning and coordination artifacts under `spec/**`.
- [2026-04-24] Read `ribbon-6-design.md` and extracted the accepted, rejected, and deferred Ribbon 6 compatibility decisions.
- [2026-04-24] Created `plan.md` with implementation phases for command contract split, boolean state subscriptions, control strategy/render-plan handling, `RibbonManager` decomposition, and session policy closure.
- [2026-04-24] Created `prompt.md` for starting the Ribbon 6 implementation session with @core-architect as lead.
- [2026-04-24] Kept Ribbon 6 implementation status as not started; the source design remains design-only until Phase 0 confirms exact public type names and migration policy.
- [2026-04-24] Ran docs-only sanity checks for Ribbon 6 keywords and whitespace.

## Current Understanding

Ribbon 6 is a coordinated compatibility break derived from Ribbon 5 Phase 4 design work. It should not be implemented piecemeal without first confirming public type names, deprecation/bridge policy, validation scope, and downstream provider migration impact.

The highest-risk areas are shared API contracts in `papiflyfx-docking-api`, runtime canonicalization and JavaFX binding behavior in `papiflyfx-docking-docks`, and downstream provider migration in GitHub, Hugo, samples, and any external consumers.

## Phase Status

| Phase | Lead | Status | Notes |
| --- | --- | --- | --- |
| Phase 0 - Planning And API Names | @core-architect / @spec-steward | Pending | Finalize names and compatibility bridge policy before code changes |
| Phase 1 - Command And Boolean State Contracts | @core-architect | Pending | Requires API, runtime, providers, tests, and docs |
| Phase 2 - Control Strategy Model | @core-architect | Pending | Requires UI-neutral strategy/render-plan design and rendering parity tests |
| Phase 3 - RibbonManager Collaborators | @core-architect | Pending | Non-breaking facade refactor after behavior is locked by tests |
| Phase 4 - Documentation, Migration, And Closure | @spec-steward / @core-architect | Pending | Release/migration notes and final validation evidence |

## Next Tasks

1. Start from `prompt.md`.
2. Read `ribbon-6-design.md`, `plan.md`, current ribbon status, and provider-authoring docs.
3. Confirm exact names for the new public contracts before editing Java sources.
4. Decide whether `PapiflyCommand`, `BoolState`, and current control records get deprecated compatibility bridges.
5. Update this tracker as each phase starts and closes.

## Validation Status

No Java source, Javadocs, POMs, CSS, runtime behavior, samples, or archetype templates have changed in this Ribbon 6 planning pass, so Maven compile/TestFX validation is not required yet.

Docs-only validation completed on 2026-04-24.

1. `rg -n "Ribbon 6|PapiflyCommand|BoolState|RibbonControlSpec|RibbonManager|extensions.ribbon|quickAccessCommandIds" spec/papiflyfx-docking-docks/2026-04-23-0-ribbon-6`
   - Passed. The design, plan, progress, prompt, and README include the expected Ribbon 6 compatibility terms.
2. `git diff --check`
   - Passed. No whitespace errors were reported.

## Open Risks

- Exact public type names are not finalized yet.
- A deprecated bridge may reduce migration churn but can extend support burden; Phase 0 must decide this explicitly.
- Any public API break requires downstream provider migration and clear release notes.
- Control strategy design must stay UI-neutral; allowing arbitrary JavaFX factories would violate the current module-boundary decision.
- Unknown customization field preservation on save remains undefined and must not be implied by Ribbon 6 session docs unless implemented.
- Busy/running command state, customization UI/schema, keytips, galleries, performance budgets, and archetype scaffold work remain deferred by design.
- Phase 3 and Phase 4 reviewer feedback from Ribbon 5 had no separate artifacts available when `ribbon-6-design.md` was drafted; Phase 0 should re-check with @feature-dev, @ui-ux-designer, @qa-engineer, and @spec-steward before implementation.

## Handoff

Lead Agent: `@spec-steward`  
Task Scope: create Ribbon 6 implementation planning artifacts from `ribbon-6-design.md`  
Impacted Modules: `spec/**` only  
Files Changed: `plan.md`, `progress.md`, `prompt.md`, and README links in this directory  
Key Invariants:

- no Ribbon 6 API/runtime/session implementation has started
- `ribbon-6-design.md` remains the source design note
- future implementation must keep feature modules dependent on `papiflyfx-docking-api`
- QAT id-first persistence and `extensions.ribbon` session namespacing remain mandatory

Validation Performed: docs-only keyword sanity and `git diff --check` completed on 2026-04-24  
Open Risks / Follow-ups: Phase 0 must finalize public names, bridge policy, migration sequence, and reviewer signoff before code changes  
Required Reviewers: `@core-architect`, `@feature-dev`, `@ui-ux-designer`, `@qa-engineer`, `@spec-steward`
