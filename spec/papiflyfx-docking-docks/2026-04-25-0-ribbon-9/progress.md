# Progress - Ribbon Side Placement

**Status:** Design documented
**Lead Agent:** @core-architect
**Design Support:** @ui-ux-designer
**Validation:** @qa-engineer
**Spec Steward:** @spec-steward

## Progress

- [2026-04-25] Created Ribbon 9 `README.md` for side-placement scope and document index.
- [2026-04-25] Created `design.md` covering top, bottom, left, and right ribbon placement semantics.
- [2026-04-25] Created `implementation.md` covering phased runtime, persistence, sample, and test work.
- [2026-04-25] Clarified that applications can specify which side hosts the ribbon: top, bottom, left, or right.
- [2026-04-25] Documented that omitted placement defaults to top for compatibility.
- [2026-04-25] Added the `SamplesApp` refactor requirement so available samples can be shown and launched through the ribbon interface.
- [2026-04-25] Added `prompt.md` to start Ribbon 9 implementation from the documented design and implementation plan.

## Validation

- Passed: `git diff --check -- spec/papiflyfx-docking-docks/2026-04-25-0-ribbon-9`
- Passed: `git diff --cached --check -- spec/papiflyfx-docking-docks/2026-04-25-0-ribbon-9`

## Open Implementation Work

- Add `RibbonPlacement` runtime model and host/ribbon properties.
- Teach `RibbonDockHost` to place the ribbon in top, bottom, left, or right regions.
- Make ribbon header, tab strip, group scroller, and adaptive layout orientation-aware.
- Persist placement in ribbon session state with top as the compatibility fallback.
- Add samples and TestFX coverage for all four placements.
- Refactor `SamplesApp` to surface available samples through ribbon tabs, groups, menus, or commands while reusing `SampleCatalog` metadata.

## Open Risks

- Vertical placements may expose width-only assumptions in adaptive layout and group caching.
- JavaFX menu and split-button internals need explicit side-placement styling to keep labels and arrows readable.
- Collapsed group popup anchoring needs side-aware positioning to avoid opening off-screen.
