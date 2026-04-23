# Plan — Ribbon Shell Text Clipping In SamplesApp

**Priority:** P2 (Normal)  
**Lead Agent:** @core-architect  
**Required Reviewers:** @ui-ux-designer, @qa-engineer, @ops-engineer, @spec-steward  
**Workflow:** snapshot analysis complete -> implementation -> validation

## Goal

Remove text clipping in the SamplesApp ribbon shell so ribbon command labels and group captions render cleanly at the default sample size and across the supported adaptive size modes.

## Snapshot analysis

The provided SamplesApp snapshot shows a consistent bottom-edge clipping pattern in the ribbon content area:

1. Large command labels such as `Paste`, `Copy`, `Duplicate`, and `Pin Preview` lose their lower glyph area.
2. The group caption `Clipboard` is also clipped, which indicates the issue is not isolated to one control type.
3. The clipping occurs in the baseline wide layout before adaptive collapse, so the default large-mode geometry is already too tight.

This points to a ribbon runtime layout defect in `papiflyfx-docking-docks`, not a provider-data problem in `papiflyfx-docking-samples`.

## Baseline findings

### Runtime hot spots

1. `RibbonControlFactory.configureGroupMetadata(...)` uses stacked text for both `LARGE` and `MEDIUM` controls, with wrapping enabled for both modes.
2. `ribbon.css` fixes large controls at `76px` high and medium controls at `58px` high, while also applying icon, padding, and `graphic-text-gap` constraints.
3. `RibbonGroup` renders the group footer with only top padding and no explicit footer-height budget for the caption/launcher row.

### Likely root causes

1. The fixed heights in `.pf-ribbon-control-large` and `.pf-ribbon-control-medium` are too small for the actual font metrics on the current JavaFX/macOS rendering path.
2. Group-footer sizing is driven indirectly by content, so the caption descenders can be compressed by the launcher button row.
3. The current control layout treats large and medium presentations too similarly; medium mode likely needs a different text/icon arrangement rather than a shorter stacked layout.

## Scope

### In scope

1. Adjust ribbon command control sizing and layout rules in `papiflyfx-docking-docks`.
2. Adjust group footer sizing so group captions do not clip.
3. Revalidate adaptive transitions after sizing changes so large/medium/small/collapsed modes still behave deterministically.
4. Add regression coverage for the clipping scenario using ribbon-focused tests.
5. Use SamplesApp ribbon content as the acceptance fixture because it already exposes the failure clearly.

### Out of scope

1. Ribbon feature expansion or command-model refactors.
2. Provider-contract changes in `papiflyfx-docking-api`.
3. Visual redesign beyond the spacing/legibility work required to remove clipping.

## Impacted modules

| Module | Planned responsibility |
| --- | --- |
| `papiflyfx-docking-docks` | ribbon control geometry, footer layout, CSS tuning, regression tests |
| `papiflyfx-docking-samples` | manual validation surface for the fix; sample content should remain unchanged unless a test fixture helper is needed |
| `spec/**` | task plan and validation notes |

## Key invariants

1. Ribbon commands must remain accessible through all adaptive size modes.
2. The fix must preserve the current SPI and provider shapes.
3. Shared ribbon styling must continue using the established `-pf-ui-*` token vocabulary.
4. The fix must not rely on sample-specific label shortening to hide a runtime layout defect.
5. Group chrome, launcher behavior, and collapsed popup behavior must remain intact.

## Proposed implementation

### Phase 1 — Reproduce and measure the clipping budget

1. Reproduce the issue in the SamplesApp ribbon shell and inspect large/medium control bounds plus group-footer bounds.
2. Record the effective height budget for:
   - icon area
   - graphic/text gap
   - wrapped label area
   - footer caption and launcher row
3. Confirm whether clipping is caused by control height, footer height, text baseline alignment, or a combination.

### Phase 2 — Correct control geometry

1. Rework large-mode sizing so stacked icon-plus-label controls have enough vertical room for real font metrics.
2. Revisit medium-mode presentation:
   - either increase height to support stacked labels
   - or switch medium mode to a more compact icon-plus-inline-label layout
3. Keep width changes minimal unless height-only changes fail to remove clipping.
4. Verify that long labels such as `Pin Preview` and `Front Matter` still render predictably.

### Phase 3 — Correct group footer geometry

1. Give the footer row an explicit vertical budget instead of relying on the launcher button to define the row height.
2. Add bottom padding, min-height, or alignment adjustments so group captions like `Clipboard` and `Layout` keep full descenders visible.
3. Ensure launcher visibility changes do not shift or clip the footer label.

### Phase 4 — Regression coverage

1. Add or extend a ribbon Fx test that renders a representative large-mode group with labels matching the sample fixture.
2. Assert that command-label and footer-label nodes fit within their owning control/footer bounds after CSS/layout is applied.
3. Keep the regression focused on geometry/visibility rather than pixel-perfect screenshot matching.
4. Re-run adaptive-layout tests to confirm the sizing change does not break collapse ordering or cached control reuse.

## Acceptance criteria

- [ ] In SamplesApp, the `Ribbon Shell` sample no longer clips the labels for `Paste`, `Copy`, `Duplicate`, `Pin Preview`, or group captions such as `Clipboard`.
- [ ] Large-mode ribbon controls render full text descenders at the default sample window size.
- [ ] Medium-mode and collapsed transitions still preserve command accessibility.
- [ ] Group footer captions remain readable with and without a dialog launcher button.
- [ ] Ribbon regression tests cover the clipping scenario and pass headlessly.

## Validation strategy

1. Compile the affected modules:
   - `./mvnw -pl papiflyfx-docking-docks,papiflyfx-docking-samples -am compile`
2. Run ribbon-focused headless tests:
   - `./mvnw -pl papiflyfx-docking-docks -am -Dtestfx.headless=true test`
3. Manually verify in SamplesApp:
   - open `Docks -> Ribbon Shell`
   - confirm large-mode labels are fully visible at the initial window size
   - resize narrower and wider to confirm no new clipping appears during adaptive transitions

## Risks and mitigations

| Risk | Mitigation |
| --- | --- |
| Height increases make groups feel oversized | Prefer the smallest geometry change that clears descenders, and review with `@ui-ux-designer` |
| Medium-mode adjustments disturb adaptive width thresholds | Re-run existing adaptive-layout tests and keep thresholds under review |
| Footer fixes regress launcher alignment | Validate both launcher-present and launcher-absent groups |
| TestFX geometry assertions are brittle across platforms | Assert containment/visibility invariants instead of exact pixel values |

## Handoff notes

Lead Agent: `@core-architect`  
Task Scope: remove ribbon text clipping in the SamplesApp shell by correcting ribbon runtime geometry and adding regression coverage  
Impacted Modules: `papiflyfx-docking-docks`, `papiflyfx-docking-samples`, `spec/**`  
Files Changed: `spec/papiflyfx-docking-docks/2026-04-23-0-ribbon-3/plan.md`  
Key Invariants: preserve SPI shape, preserve adaptive access to commands, fix runtime layout instead of shortening sample labels  
Validation Performed: snapshot analysis and source inspection only  
Open Risks / Follow-ups: confirm whether medium mode should remain stacked or move to a denser inline layout  
Required Reviewer: `@spec-steward`
