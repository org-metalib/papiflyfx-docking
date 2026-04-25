# Progress - Ribbon 10 Side Toolbar Placement

**Status:** Planning started
**Lead Agent:** @core-architect
**Design Support:** @ui-ux-designer
**Validation:** @qa-engineer
**Spec Steward:** @spec-steward

## Progress

- [2026-04-25] Captured user feedback that the Ribbon 9 vertical side-ribbon implementation looks too heavy and should move toward an IntelliJ-style toolbar/tool-window stripe.
- [2026-04-25] Created `concept.md` describing the side toolbar mental model: compact outside rail plus transient command popovers over dock content.
- [2026-04-25] Created `plan.md` with phased work for UX contract, runtime layout refactor, styling/accessibility, tests, samples, and docs.
- [2026-04-25] Recorded product decisions from the initial open questions:
  - side rail supports both QAT commands and ribbon tab/action-group entries
  - selected side tabs do not need to remain visually selected after popover close
  - minimized side placement keeps the rail visible but suppresses popover activation
  - text-only rail commands use generated initials/fallback glyphs
  - the Ribbon 9 persistent side command pane should not remain behind an opt-in compatibility flag

## Current Understanding

Ribbon 10 is a design pivot over the Ribbon 9 implementation. The public `RibbonPlacement` API should stay intact, and `TOP`/`BOTTOM` should remain horizontal ribbons. The main change is visual and interaction behavior for `LEFT` and `RIGHT`: replace the persistent wide side command pane with a compact toolbar rail and transient popovers.

The highest value constraint is preserving provider and session compatibility. The highest UX risk is making side placement too icon-only without adequate accessible text, tooltips, or fallback glyphs for commands without icons.

Initial product questions are now answered. The next implementation pass should focus on translating those decisions into a concrete rail ordering, activation policy, and fallback glyph rule set.

## Phase Status

| Phase | Lead | Status | Notes |
| --- | --- | --- | --- |
| Phase 1 - UX Contract And Internal Shape | @core-architect | In progress | Product decisions recorded; implementation details still need ordering/focus rules |
| Phase 2 - Runtime Layout Refactor | @core-architect | Not started | Replace persistent side pane with compact side rail and transient popover |
| Phase 3 - Styling And Accessibility | @core-architect / @ui-ux-designer | Not started | Tokenized IDE-style side toolbar visual treatment |
| Phase 4 - Tests, Samples, Docs | @core-architect | Not started | Update placement tests, sample demo, and docs |

## Next Tasks

1. Review Ribbon 9 side-placement implementation in `Ribbon`, `RibbonTabStrip`, `RibbonGroup`, and `ribbon.css`.
2. Define the rail ordering for QAT commands and ribbon tab/action-group entries.
3. Define minimized activation suppression for mouse and keyboard paths.
4. Define generated initials/fallback glyph rules for text-only rail commands.
5. Implement a small runtime slice behind the existing `LEFT`/`RIGHT` placements and update tests alongside it.

## Validation

No implementation validation has been run for Ribbon 10 yet. This pass created design documents only.

Recommended validation after implementation:

```bash
./mvnw -pl papiflyfx-docking-docks -am '-Dtest=Ribbon*Test,Ribbon*FxTest' -Dsurefire.failIfNoSpecifiedTests=false -Dtestfx.headless=true test
./mvnw -pl papiflyfx-docking-samples -am '-Dtest=*Ribbon*FxTest,SamplesSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false -Dtestfx.headless=true test
git diff --check
```

## Open Risks

- Side toolbar may require generated fallback glyphs or compact text rules for commands without icons.
- Popover focus return needs deterministic TestFX coverage.
- Existing collapsed group popups may need adaptation to avoid nested popovers.
- `@ui-ux-designer` should validate the visual direction before runtime work goes deep.

## Handoff

Lead Agent: @core-architect  
Task Scope: Design Ribbon 10 side toolbar placement as a replacement for the Ribbon 9 vertical side-ribbon visual model  
Impacted Modules: `papiflyfx-docking-docks`, `papiflyfx-docking-samples`, `spec/papiflyfx-docking-docks/2026-04-25-0-ribbon-10`  
Files Changed: `concept.md`, `plan.md`, `progress.md`  
Key Invariants: Keep placement API/session compatibility; keep providers placement-agnostic; keep TOP/BOTTOM unchanged  
Validation Performed: Documentation-only, no automated validation run  
Open Risks / Follow-ups: Rail ordering, minimized activation suppression details, fallback glyph rules, popover focus  
Required Reviewer: @ui-ux-designer, @qa-engineer, @spec-steward
