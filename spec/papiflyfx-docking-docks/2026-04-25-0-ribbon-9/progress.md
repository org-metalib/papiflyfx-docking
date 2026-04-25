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

## Current Understanding

Ribbon 9 is a docks-runtime feature led by `@core-architect` with UI design support. The intended public surface is a small `RibbonPlacement` enum plus placement properties on `RibbonDockHost` and `Ribbon`. The key compatibility requirement is that existing hosts and saved sessions continue to behave as top-ribbon sessions unless placement is explicitly supplied.

The highest-risk areas are session compatibility, axis-aware adaptive layout, focus behavior in vertical placements, CSS readability for side-rail commands, menus, and split buttons, and keeping the `SamplesApp` comparison demo deterministic without duplicating provider behavior.

## Phase Status

| Phase | Lead | Status | Notes |
| --- | --- | --- | --- |
| Phase 1 - API And Session Contract | @core-architect | Not started | Add placement API and tolerant persistence |
| Phase 2 - Host And Ribbon Layout | @core-architect | Not started | Move host region and add vertical ribbon structure |
| Phase 3 - Adaptive Layout And Styling | @core-architect / @ui-ux-designer | Not started | Make sizing axis-aware and add tokenized CSS |
| Phase 4 - Accessibility, Docs, And Closure | @spec-steward | Not started | Add SamplesApp top/left demo, validate keyboard/focus behavior, and update docs |

## Next Tasks

1. Confirm answers or implementation defaults for the three open questions in `design.md`.
2. Inspect current `RibbonDockHost`, `Ribbon`, session state contributor, adaptive layout tests, and ribbon CSS.
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

- Vertical ribbons may need a width budget decision before CSS and adaptive sizing can be finalized.
- Bottom placement header ordering may need a product/design choice if users expect command chrome nearest the content edge.
- Focus traversal in minimized side placement requires explicit TestFX coverage to avoid empty command panels or stranded focus.
