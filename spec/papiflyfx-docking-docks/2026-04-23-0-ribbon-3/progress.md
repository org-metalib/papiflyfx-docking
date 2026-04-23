# Progress — Ribbon Shell Text Clipping In SamplesApp

**Status:** Planning complete; implementation not started  
**Current Milestone:** Phase 1 — reproduce and measure the clipping budget  
**Priority:** P2 (Normal)  
**Lead Agent:** @core-architect  
**Required Reviewers:** @ui-ux-designer, @qa-engineer, @ops-engineer, @spec-steward

## Completion summary

- Research / snapshot analysis: 100%
- Planning: 100%
- Phase 1 — Reproduce and measure the clipping budget: 0%
- Phase 2 — Correct control geometry: 0%
- Phase 3 — Correct group footer geometry: 0%
- Phase 4 — Regression coverage: 0%

## Accomplishments

- [2026-04-23] Reviewed the user-provided SamplesApp snapshot and identified a consistent bottom-edge clipping pattern in ribbon labels and group captions.
- [2026-04-23] Inspected the ribbon runtime implementation in `papiflyfx-docking-docks`:
  - `RibbonControlFactory.configureGroupMetadata(...)` currently uses stacked wrapped labels for both `LARGE` and `MEDIUM` controls.
  - `ribbon.css` currently fixes large controls at `76px` high and medium controls at `58px` high.
  - `RibbonGroup` currently renders the footer caption row without an explicit height budget.
- [2026-04-23] Confirmed the issue is most likely a ribbon runtime geometry defect rather than bad sample-provider content.
- [2026-04-23] Created the task plan in `spec/papiflyfx-docking-docks/2026-04-23-0-ribbon-3/plan.md`.

## Current understanding

The strongest current hypothesis is that the clipping is caused by a combination of:

1. fixed large/medium ribbon control heights that are too small for actual JavaFX font metrics on the current rendering path
2. insufficient vertical budget in the group footer row for caption descenders
3. medium-mode presentation being too dense for the current stacked text treatment

## Next tasks

1. Reproduce the clipping in the `SamplesApp` `Docks -> Ribbon Shell` sample and inspect the layout bounds for large controls and group footers.
2. Decide whether medium mode should stay stacked with larger height or switch to a denser inline label arrangement.
3. Implement the minimum geometry changes needed in `papiflyfx-docking-docks`.
4. Add focused ribbon regression coverage for label/footer containment after CSS and layout are applied.
5. Run compile and headless ribbon tests, then record the results.

## Open risks

- Increasing control height may change the ribbon’s visual density and adaptive thresholds more than expected.
- Footer fixes may interact with launcher-button alignment in groups that do expose a dialog launcher.
- Geometry assertions in TestFX can become brittle if they depend on exact pixels instead of containment/visibility invariants.

## Validation status

- No automated validation has been run for this task yet.
- Validation performed so far is limited to snapshot analysis and source inspection.

## Handoff snapshot

Lead Agent: `@core-architect`  
Task Scope: remove ribbon label and footer-caption clipping in the SamplesApp ribbon shell  
Impacted Modules: `papiflyfx-docking-docks`, `papiflyfx-docking-samples`, `spec/**`  
Files Changed: `spec/papiflyfx-docking-docks/2026-04-23-0-ribbon-3/plan.md`, `spec/papiflyfx-docking-docks/2026-04-23-0-ribbon-3/progress.md`  
Key Invariants:
- preserve the current ribbon SPI and provider contracts
- fix runtime layout instead of shortening sample labels
- keep command access intact across adaptive size modes
Validation Performed:
- snapshot review of the provided image
- source inspection of ribbon runtime classes and CSS
Open Risks / Follow-ups:
- confirm whether medium mode should remain stacked or move to inline labels
- verify footer sizing with and without a launcher button
Required Reviewer: `@spec-steward`
