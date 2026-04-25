# Progress - Ribbon Side Placement

**Status:** Planning complete, implementation not started  
**Lead Agent:** @core-architect  
**Design Support:** @ui-ux-designer  
**Validation:** @qa-engineer  
**Spec Steward:** @spec-steward

## Progress

- [2026-04-25] Reviewed `design.md` for the Ribbon 9 side-placement scope, API shape, layout model, persistence rules, styling requirements, accessibility requirements, and open questions.
- [2026-04-25] Reviewed neighboring ribbon spec plans and progress logs for repository-local planning format.
- [2026-04-25] Created `plan.md` with implementation phases for API/session contract, host/ribbon layout, adaptive styling, accessibility, documentation, and validation.
- [2026-04-25] Recorded this initial progress tracker for handoff into implementation.
- [2026-04-25] Added the `SamplesApp` demo requirement to `design.md` and `plan.md`: a deterministic catalog demo should compare `TOP` and `LEFT` ribbon hosts.
- [2026-04-25] Incorporated the new `concept.md` side-ribbon ideas into `design.md` and `plan.md` after implementation changes were rolled back:
  - side placement should use an edge tab strip plus a separate inner command content pane, not a generic sidebar or a single stacked header
  - `LEFT` and `RIGHT` mirror tab-strip/content-pane ordering so tabs stay on the outside edge
  - side group labels move to top section headers
  - large side commands span the content-pane width, while smaller controls may wrap in a grid
  - side minimized mode keeps the edge tab strip visible and reveals command content transiently on tab activation without changing persisted minimized state
  - accordion-style side group collapse remains an implementation option or follow-up, not a provider SPI requirement
- [2026-04-25] Recorded product decisions for all open questions:
  - placement is host-configurable only
  - side command content-pane width is controlled by a theme token
  - bottom placement mirrors top header ordering
  - minimized side tab activation uses an overlay flyout
  - side group sections use accordion-style collapse in Ribbon 9

## Current Understanding

Ribbon 9 is a docks-runtime feature led by `@core-architect` with UI design support. The intended public surface is a small `RibbonPlacement` enum plus placement properties on `RibbonDockHost` and `Ribbon`. The key compatibility requirement is that existing hosts and saved sessions continue to behave as top-ribbon sessions unless placement is explicitly supplied.

The side-ribbon target shape is now more specific: side placement should render as a compact outside edge tab strip plus an inner command content pane. For `RIGHT`, dock content sits next to the command content pane and the tab strip sits on the far right edge. For `LEFT`, the tab strip sits on the far left edge and the command content pane sits between it and dock content.

The highest-risk areas are session compatibility, axis-aware adaptive layout, focus behavior in vertical placements, overlay flyout behavior, accordion group focus/state behavior, CSS readability for side commands, menus, and split buttons, and keeping the `SamplesApp` comparison demo deterministic without duplicating provider behavior.

## Phase Status

| Phase | Lead | Status | Notes |
| --- | --- | --- | --- |
| Phase 1 - API And Session Contract | @core-architect | Not started | Add placement API and tolerant persistence |
| Phase 2 - Host And Ribbon Layout | @core-architect | Not started | Move host region and add vertical ribbon structure |
| Phase 3 - Adaptive Layout And Styling | @core-architect / @ui-ux-designer | Not started | Make sizing axis-aware and add tokenized CSS for edge strip/content pane side layout |
| Phase 4 - Accessibility, Docs, And Closure | @spec-steward | Not started | Add SamplesApp top/left demo, validate keyboard/focus behavior, and update docs |

## Next Tasks

1. Inspect current `RibbonDockHost`, `Ribbon`, session state contributor, adaptive layout tests, and ribbon CSS.
2. During Phase 2, design the side placement internals around an outside edge tab strip, theme-token content pane, overlay flyout, and accordion group sections.
3. Start Phase 1 with `RibbonPlacement`, placement properties, and session restore compatibility tests.
4. During closure, add a `SamplesApp` catalog demo comparing top and left ribbon hosts plus smoke coverage for registration/build.

## Validation

No implementation validation has been run yet. Planning created docs only.

Recommended validation after implementation:

```bash
./mvnw -pl papiflyfx-docking-docks -am -Dtest=Ribbon*Test,Ribbon*FxTest -Dsurefire.failIfNoSpecifiedTests=false -Dtestfx.headless=true test
./mvnw -pl papiflyfx-docking-samples -am -Dtest=*Ribbon*FxTest,SamplesSmokeTest -Dsurefire.failIfNoSpecifiedTests=false -Dtestfx.headless=true test
git diff --check
```

## Open Risks

- Vertical ribbons need a concrete theme token name and default value for content-pane width before CSS and adaptive sizing can be finalized.
- Overlay flyout positioning and focus return need explicit TestFX coverage.
- Accordion-style side group collapse must be implemented without provider API changes and needs focus/state coverage.
- Focus traversal in minimized side placement requires explicit TestFX coverage to avoid empty command panels or stranded focus.
