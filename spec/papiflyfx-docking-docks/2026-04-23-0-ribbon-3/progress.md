# Progress — Ribbon Shell Text Clipping In SamplesApp

**Status:** Implemented and validated  
**Current Milestone:** Complete; awaiting review  
**Priority:** P2 (Normal)  
**Lead Agent:** @core-architect  
**Required Reviewers:** @ui-ux-designer, @qa-engineer, @ops-engineer, @spec-steward

## Completion summary

- Research / snapshot analysis: 100%
- Planning: 100%
- Phase 1 — Reproduce and measure the clipping budget: 100%
- Phase 2 — Correct control geometry: 100%
- Phase 3 — Correct group footer geometry: 100%
- Phase 4 — Regression coverage: 100%

## Accomplishments

- [2026-04-23] Reviewed the user-provided SamplesApp snapshot and identified a consistent bottom-edge clipping pattern in ribbon labels and group captions.
- [2026-04-23] Inspected the ribbon runtime implementation in `papiflyfx-docking-docks`:
  - `RibbonControlFactory.configureGroupMetadata(...)` currently uses stacked wrapped labels for both `LARGE` and `MEDIUM` controls.
  - `ribbon.css` currently fixes large controls at `76px` high and medium controls at `58px` high.
  - `RibbonGroup` currently renders the footer caption row without an explicit height budget.
- [2026-04-23] Confirmed the issue is most likely a ribbon runtime geometry defect rather than bad sample-provider content.
- [2026-04-23] Created the task plan in `spec/papiflyfx-docking-docks/2026-04-23-0-ribbon-3/plan.md`.
- [2026-04-23] Measured the effective stacked-control geometry from the runtime code:
  - fallback large icons are rendered as `40px` square nodes (`32px` icon size plus `8px` wrapper allowance)
  - large controls previously had `76px` total height, leaving only a narrow margin for `40px` graphic + `6px` gap + label font metrics + `16px` vertical padding
  - medium controls previously used the same stacked text model with only `58px` total height, making wrapped labels such as `Pin Preview` especially vulnerable
  - group footers had top padding only and no explicit row height, so caption descenders depended on indirect content sizing
- [2026-04-23] Implemented the runtime geometry fix in `papiflyfx-docking-docks`:
  - increased large stacked ribbon controls from `76px` to `88px`
  - increased medium stacked ribbon controls from `58px` to `68px`
  - gave group footers an explicit `28px` row budget with bottom padding
  - centered footer captions and launcher buttons in `RibbonGroup`
- [2026-04-23] Added a TestFX regression in `RibbonAdaptiveLayoutFxTest` using sample-style labels: `Paste`, `Copy`, `Duplicate`, `Pin Preview`, `Clipboard`, and `Layout`.
- [2026-04-23] The regression asserts vertical containment of command text inside owning controls and group captions inside footer rows after CSS/layout in both `LARGE` and forced `MEDIUM` modes.

## Current understanding

The implemented fix treats the clipping as a stacked-control geometry issue plus an under-budgeted footer row:

1. `LARGE` and `MEDIUM` controls keep their current stacked icon-plus-label presentation, preserving the existing ribbon SPI and adaptive behavior.
2. The control heights now include enough room for JavaFX font metrics with fallback glyph graphics and wrapped labels.
3. The footer row now has an explicit vertical budget and bottom padding so captions remain readable with and without a launcher button.

## Next tasks

1. Review the visual density with `@ui-ux-designer` because the large and medium rows are intentionally taller.
2. Optionally perform an interactive SamplesApp spot check in a graphical environment.

## Open risks

- Increasing control height changes vertical density but does not affect the width-based adaptive collapse estimates.
- Interactive SamplesApp verification was not performed in this run; the sample labels were validated through a headless ribbon fixture.
- The TestFX regression avoids pixel-perfect screenshots, but it still depends on JavaFX skin text-node bounds being available after CSS/layout.

## Validation status

- `./mvnw -pl papiflyfx-docking-docks -am -Dtest=RibbonAdaptiveLayoutFxTest#sampleRibbonLabelsStayInsideControlsAndFooters -Dsurefire.failIfNoSpecifiedTests=false -Dtestfx.headless=true test`
  - Result: PASS, 1 test run, 0 failures, 0 errors.
- `./mvnw -pl papiflyfx-docking-docks,papiflyfx-docking-samples -am compile`
  - Result: PASS, 14-module reactor compile successful.
- `./mvnw -pl papiflyfx-docking-docks -am -Dtestfx.headless=true test`
  - Result: PASS, 89 tests run, 0 failures, 0 errors.
- Manual interactive SamplesApp verification:
  - Not run in this environment; covered by the headless TestFX sample-style fixture instead.

## Handoff snapshot

Lead Agent: `@core-architect`  
Task Scope: remove ribbon label and footer-caption clipping in the SamplesApp ribbon shell  
Impacted Modules: `papiflyfx-docking-docks`, `papiflyfx-docking-samples`, `spec/**`  
Files Changed:
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonGroup.java`
- `papiflyfx-docking-docks/src/main/resources/org/metalib/papifly/fx/docks/ribbon/ribbon.css`
- `papiflyfx-docking-docks/src/test/java/org/metalib/papifly/fx/docks/ribbon/RibbonAdaptiveLayoutFxTest.java`
- `spec/papiflyfx-docking-docks/2026-04-23-0-ribbon-3/progress.md`  
Key Invariants:
- preserve the current ribbon SPI and provider contracts
- fix runtime layout instead of shortening sample labels
- keep command access intact across adaptive size modes
Validation Performed:
- focused TestFX geometry regression
- affected reactor compile
- full `papiflyfx-docking-docks` headless test suite
Open Risks / Follow-ups:
- optional interactive SamplesApp spot check
Required Reviewers: `@ui-ux-designer`, `@qa-engineer`, `@ops-engineer`, `@spec-steward`
