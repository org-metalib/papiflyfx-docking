# Plan - Ribbon Side Placement

**Lead Agent:** @core-architect  
**Design Support:** @ui-ux-designer  
**Validation:** @qa-engineer  
**Spec Steward:** @spec-steward

## Scope

Add host-configurable ribbon placement for `TOP`, `BOTTOM`, `LEFT`, and `RIGHT` while preserving the existing top-ribbon default, provider contracts, command discovery, minimized behavior, contextual tabs, Quick Access Toolbar state, and session compatibility.

## Tasks

1. Inspect `RibbonDockHost`, `Ribbon`, ribbon session contribution, adaptive group sizing, and current CSS class ownership.
2. Add `RibbonPlacement` and placement properties on `RibbonDockHost` and `Ribbon`.
3. Keep `TOP` as the default and ensure existing hosts render exactly as they do today unless a placement is explicitly set.
4. Make `RibbonDockHost` place the ribbon in the correct `BorderPane` region and keep dock content in the center.
5. Make `Ribbon` layout axis-aware so top/bottom remain horizontal and left/right use a vertical header, vertical tab strip, and vertical command scroller.
6. Update adaptive group sizing so extent estimation uses width for horizontal placements and height for vertical placements.
7. Extend ribbon session persistence with optional `placement`, including safe fallback for missing, unknown, or malformed values.
8. Add placement and orientation style classes plus tokenized CSS for side ribbons, focus states, tab selection, group widths, scrolling, and menu/split-button readability.
9. Preserve minimized behavior across all placements, including minimized tab activation that reveals commands without changing the persisted minimized flag.
10. Add a `SamplesApp` catalog demo that shows comparable `TOP` and `LEFT` ribbon hosts using deterministic local sample content.
11. Add focused tests for default compatibility, all four host placements, placement persistence compatibility, axis-aware adaptive sizing, minimized activation, CSS class updates, and sample catalog registration.
12. Update relevant docs and record validation results in `progress.md`.

## Acceptance Criteria

1. Existing hosts that do not set placement still use the top ribbon and remain session-compatible.
2. Applications can set ribbon placement to `TOP`, `BOTTOM`, `LEFT`, or `RIGHT` through the host/ribbon property API.
3. Feature `RibbonProvider`s stay placement-agnostic and require no side-specific code.
4. Ribbon session restore treats missing, unknown, or malformed placement values as placement-only fallback to `TOP`.
5. Minimized state, selected tab, QAT ids, and placement persist together without changing existing restored values.
6. Vertical placements use readable, non-rotated tab and command labels.
7. `SamplesApp` includes a demo that visibly compares top and left ribbon placement.
8. Focus indicators and keyboard traversal remain usable in every placement.
9. Focused headless tests pass.

## Implementation Phases

### Phase 1 - API And Session Contract

Lead: @core-architect  
Reviewers: @qa-engineer, @spec-steward

Tasks:

1. Add `RibbonPlacement` in `papiflyfx-docking-docks`.
2. Add `placementProperty()`, `getPlacement()`, and `setPlacement(...)` to `RibbonDockHost` and `Ribbon`.
3. Bind or synchronize host placement to the contained ribbon without exposing placement through provider SPI.
4. Extend ribbon session state with optional placement serialization and tolerant restore.
5. Add tests for default `TOP`, explicit values, missing placement, unknown placement, and malformed placement.

Acceptance:

- Existing session payloads restore as `TOP`.
- Unknown or malformed placement does not invalidate minimized, selected-tab, or QAT restore.
- No feature-module provider contract changes are required.

### Phase 2 - Host And Ribbon Layout

Lead: @core-architect  
Reviewers: @ui-ux-designer, @qa-engineer

Tasks:

1. Move `RibbonDockHost` ribbon attachment between `BorderPane.top`, `bottom`, `left`, and `right`.
2. Keep dock content stable in the center region during placement changes.
3. Refactor `Ribbon` layout construction/update paths so horizontal and vertical placements share command models but use placement-specific containers.
4. Keep tabs readable in side placements without rotated text.
5. Ensure minimized side ribbons render as compact header columns.

Acceptance:

- All four placements render the same tab/group/command data.
- Placement changes update the host region and ribbon structure without recreating providers.
- Minimized side placement keeps a usable tab activation path.

### Phase 3 - Adaptive Layout And Styling

Lead: @core-architect  
Design Support: @ui-ux-designer  
Reviewers: @qa-engineer

Tasks:

1. Make group extent estimation orientation-aware.
2. Apply the same collapse-order semantics to horizontal width and vertical height constraints.
3. Add placement and orientation style classes.
4. Add tokenized CSS for side rail sizing, spacing, selected/focused tab states, group scroller padding, scroll bars, and command/menu readability.
5. Verify dark and light theme readability for side placement controls.

Acceptance:

- Horizontal placements preserve current adaptive behavior.
- Vertical placements collapse and restore groups using available height.
- CSS uses existing `-pf-ui-*` and `-fx-ribbon-*` token vocabulary.

### Phase 4 - Accessibility, Docs, And Closure

Lead: @spec-steward  
Implementation Support: @core-architect, @ui-ux-designer  
Validation: @qa-engineer

Tasks:

1. Verify focus traversal through QAT, tabs, collapse button, and visible commands for all placements.
2. Ensure selected tabs remain toggle controls in one toggle group.
3. Add a deterministic `SamplesApp` catalog demo that shows `TOP` and `LEFT` ribbon hosts with the same representative ribbon commands.
4. Add or update sample smoke coverage so the demo is registered and builds headlessly.
5. Update ribbon documentation or samples if a public host-placement entry point is added.
6. Record automated validation, manual inspection notes, reviewer handoff, and remaining risks in `progress.md`.

Acceptance:

- Accessibility behavior is explicitly validated or documented as a remaining risk.
- `SamplesApp` provides a top/left placement comparison demo.
- Public docs match implemented placement API and persistence behavior.
- `progress.md` contains validation evidence before closure.

## Validation Commands

```bash
./mvnw -pl papiflyfx-docking-docks -am -Dtest=Ribbon*Test,Ribbon*FxTest -Dsurefire.failIfNoSpecifiedTests=false -Dtestfx.headless=true test
./mvnw -pl papiflyfx-docking-samples -am -Dtest=*Ribbon*FxTest,SamplesSmokeTest -Dsurefire.failIfNoSpecifiedTests=false -Dtestfx.headless=true test
git diff --check
```

Broaden validation if implementation touches shared API modules, feature-module providers, Maven/TestFX configuration, archetype templates, or sample behavior outside ribbon placement.

## Open Questions

1. Should application users be able to change placement through a built-in ribbon command, or should placement remain host-configured only?
2. Should side ribbons use a fixed rail width, a theme token, or derive width from the largest visible group?
3. Should bottom placement mirror top header ordering or place the QAT nearest the content edge?
