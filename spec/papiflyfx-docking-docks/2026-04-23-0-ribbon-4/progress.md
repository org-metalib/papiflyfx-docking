# Progress — GitHub And Hugo Ribbon Samples

**Status:** Planned; awaiting implementation  
**Current Milestone:** Implementation kickoff  
**Priority:** P2 (Normal)  
**Planning Lead:** @spec-steward  
**Implementation Lead:** @ops-engineer  
**Required Reviewers:** @feature-dev, @ui-ux-designer, @qa-engineer, @spec-steward

## Completion Summary

- Planning: 100%
- Phase 1 — Add GitHub Ribbon Sample: 0%
- Phase 2 — Add Hugo Ribbon Sample: 0%
- Phase 3 — Catalog And Navigation: 0%
- Phase 4 — Regression Coverage: 0%
- Validation: 0%

## Accomplishments

- [2026-04-23] Created `spec/papiflyfx-docking-docks/2026-04-23-0-ribbon-4/plan.md`.
- [2026-04-23] Confirmed the current SamplesApp baseline:
  - `Docks -> Ribbon Shell` already mounts a generic `RibbonDockHost`.
  - `GitHub -> GitHub Toolbar` exists as a standalone toolbar demo.
  - `Hugo -> Hugo Preview` exists as a docked preview demo.
- [2026-04-23] Identified the target implementation path: add dedicated `GitHub Ribbon` and `Hugo Ribbon` sample scenes that expose typed ribbon action capabilities from active dock content.
- [2026-04-23] Added this progress tracker and a task prompt document for implementation handoff.

## Current Understanding

The new demos should be SamplesApp entries, not replacements for existing samples. The implementation should stay inside `papiflyfx-docking-samples` unless a real API gap is found.

The preferred implementation shape is:

1. A `GitHubRibbonSample` under the samples GitHub package.
2. A `HugoRibbonSample` under the samples Hugo package.
3. Private deterministic sample content nodes that implement `GitHubRibbonActions` and `HugoRibbonActions`.
4. `RibbonDockHost` composition using existing `DockManager` ribbon-context propagation.
5. Catalog registration next to the existing GitHub and Hugo samples.

## Next Tasks

1. Implement `GitHubRibbonSample`.
2. Implement `HugoRibbonSample`.
3. Register both scenes in `SampleCatalog`.
4. Add focused TestFX coverage for provider tabs and representative safe command execution.
5. Run the planned compile and headless sample tests.

## Validation Status

No automated validation has been run yet for this initiative. This progress file currently records planning only.

Planned validation:

1. `./mvnw -pl papiflyfx-docking-samples -am compile`
2. `./mvnw -pl papiflyfx-docking-samples -am -Dtest=SamplesSmokeTest -Dsurefire.failIfNoSpecifiedTests=false -Dtestfx.headless=true test`
3. `./mvnw -pl papiflyfx-docking-samples -am -Dtest=*Ribbon*FxTest -Dsurefire.failIfNoSpecifiedTests=false -Dtestfx.headless=true test`

## Open Risks

- The dedicated samples may show all ServiceLoader-discovered tabs unless the implementation intentionally isolates providers or relies on active-context selection.
- The Hugo contextual tab depends on matching `HugoRibbonProvider` visibility heuristics through factory id, content id, active content type, or dock title.
- Test assertions should avoid exact layout geometry because the ribbon can adapt across widths and platforms.

## Handoff Snapshot

Lead Agent: `@ops-engineer`  
Task Scope: add SamplesApp demos for GitHub and Hugo ribbon providers using deterministic sample content and typed action capabilities  
Impacted Modules: `papiflyfx-docking-samples`, `spec/**`  
Files Changed:
- `spec/papiflyfx-docking-docks/2026-04-23-0-ribbon-4/plan.md`
- `spec/papiflyfx-docking-docks/2026-04-23-0-ribbon-4/progress.md`
- `spec/papiflyfx-docking-docks/2026-04-23-0-ribbon-4/prompt.md`  
Key Invariants:
- no public API changes without updating the spec first
- no credentials, network, repository mutation, or Hugo CLI dependency in sample actions
- preserve existing sample entries and theme/disposal behavior
- use typed ribbon capabilities instead of deprecated raw-node routing
Validation Performed: planning/source inspection only  
Open Risks / Follow-ups: confirm during implementation whether explicit provider isolation is needed for focused demos  
Required Reviewers: `@feature-dev`, `@ui-ux-designer`, `@qa-engineer`, `@spec-steward`
