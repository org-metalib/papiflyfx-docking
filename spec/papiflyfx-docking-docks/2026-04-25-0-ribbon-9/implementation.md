# Implementation - Ribbon Side Placement

**Lead Agent:** @core-architect
**Design Support:** @ui-ux-designer
**Validation:** @qa-engineer
**Spec Steward:** @spec-steward

## Scope

Implement host-configurable ribbon placement for top, left, right, and bottom sides while keeping existing top placement behavior compatible. Applications must be able to specify which side hosts the ribbon; the dock host applies that placement and falls back to top when no placement is specified.

## Affected Modules

- `papiflyfx-docking-docks`
  - `Ribbon`
  - `RibbonDockHost`
  - `RibbonGroup`
  - `RibbonTabStrip`
  - `RibbonControlFactory`
  - `RibbonSessionCodec`
  - `RibbonSessionData`
  - `ribbon.css`
  - ribbon TestFX suites
- `papiflyfx-docking-samples`
  - sample scene or sample controls that demonstrate placement switching
  - `SamplesApp` refactor so the ribbon can expose available samples
- `spec/papiflyfx-docking-docks`
  - update provider-authoring/session docs after implementation

## Phase 1 - Placement Model

1. Add `RibbonPlacement` enum in `org.metalib.papifly.fx.docks.ribbon`.
2. Add `placementProperty()`, `getPlacement()`, and `setPlacement(...)` to `Ribbon`.
3. Add matching placement property to `RibbonDockHost`.
4. Add a constructor or builder path that lets an application specify the initial `RibbonPlacement` when creating the host.
5. Bind or forward host placement into the hosted `Ribbon`.
6. Default null or omitted placement to `RibbonPlacement.TOP`.

Acceptance:

- Existing constructors still render top placement.
- Applications can explicitly request top, bottom, left, or right placement from host setup.
- Existing ribbon tests pass without source changes outside expected assertions.

## Phase 2 - Host Layout

1. Update `RibbonDockHost` so placement maps to the correct `BorderPane` region.
2. Move the ribbon node between `top`, `bottom`, `left`, and `right` without replacing the `Ribbon` instance.
3. Keep dock content in `center`.
4. Ensure placement changes preserve selected tab, minimized state, QAT contents, theme binding, and provider context.

Acceptance:

- Changing placement at runtime does not rebuild the `RibbonManager`.
- Active tab and QAT state survive each placement change.

## Phase 3 - Ribbon Orientation

1. Add placement and orientation style classes in `Ribbon`.
2. Make `Ribbon` choose horizontal or vertical header/group structure from placement.
3. For side placements:
   - stack QAT, tabs, and collapse button vertically;
   - make tabs vertical buttons with readable horizontal labels;
   - set group scroller vertical policy to `AS_NEEDED`;
   - disable horizontal scrolling unless needed for exceptional content.
4. Keep top and bottom using the existing horizontal structure.

Acceptance:

- Labels remain readable in all placements.
- Collapse/expand icon remains visible and correctly describes the action.
- Menu button labels and arrows remain readable in dark and light themes.

## Phase 4 - Adaptive Axis

1. Replace width-only adaptive calculations with an axis-aware extent calculation.
2. Teach `RibbonGroup` to estimate horizontal width or vertical height depending on orientation.
3. Apply the same collapse and restore ordering across both axes.
4. Keep group/control caches keyed by placement or orientation where node structure differs.
5. Revalidate collapsed group popup anchoring from side placements.

Acceptance:

- Top/bottom adaptive behavior is unchanged.
- Left/right adaptive behavior collapses groups when vertical space is constrained.
- No command becomes unreachable after placement changes or resize.

## Phase 5 - Persistence

1. Add `placement` to `RibbonSessionData`.
2. Update `RibbonSessionCodec` encode/decode.
3. Preserve compatibility for sessions without placement.
4. Ignore unknown placement strings by falling back to `TOP`.
5. Add codec tests for missing, valid, unknown, and malformed placement payloads.

Acceptance:

- Existing session fixtures still restore.
- Placement round-trips through capture/restore.
- Invalid placement does not discard unrelated ribbon state.

## Phase 6 - Samples

1. Add a sample placement switcher to `RibbonShellSample` or create a focused side-placement sample.
2. Include all four placements.
3. Include a menu button, split button, contextual tab, QAT commands, minimized mode, and theme switching in the sample.
4. Refactor `SamplesApp` so available samples can be surfaced through ribbon tabs, groups, menus, or commands.
5. Reuse `SampleCatalog`/`SampleScene` metadata as the source of truth for ribbon sample commands.
6. Keep the existing catalog navigation available until the ribbon sample workflow has parity.
7. Keep the sample programmatic; no FXML.

Acceptance:

- A reviewer can manually inspect top, bottom, left, and right placements from the samples app.
- The sample demonstrates minimized tab activation on each side.
- Available samples are discoverable and launchable from the ribbon interface.
- The samples ribbon reflects the same available sample set as the existing catalog.

## Test Plan

Add or extend focused tests:

1. `RibbonPlacementFxTest`
   - default placement is top;
   - host region changes for top/bottom/left/right;
   - selected tab and QAT state survive runtime placement changes.
2. `RibbonAdaptiveLayoutFxTest`
   - vertical placements render non-empty command panels;
   - side-placement menu labels and arrows are readable;
   - minimized tab activation works in left/right/bottom placements.
3. `RibbonSessionPersistenceFxTest`
   - placement round-trip;
   - missing placement defaults to top;
   - unknown placement falls back to top.
4. Existing tests
   - keep current top-placement tests passing.
5. `SamplesSmokeTest`
   - available samples can still be enumerated;
   - sample ribbon commands are present for representative categories;
   - launching a sample through the ribbon uses the same sample scene path as catalog navigation.

## Validation Commands

```bash
./mvnw -pl papiflyfx-docking-docks -am -Dtestfx.headless=true test
./mvnw -pl papiflyfx-docking-samples -am -Dtest=SamplesSmokeTest -Dsurefire.failIfNoSpecifiedTests=false -Dtestfx.headless=true test
git diff --check
```

## Risks

1. Vertical placement can expose hidden assumptions that all ribbon extents are widths.
2. JavaFX `MenuButton` and `SplitMenuButton` skin internals need explicit label/arrow styling per placement.
3. Moving the same ribbon node between `BorderPane` regions can disturb focus if not done on the FX thread.
4. Collapsed group popups need side-aware anchoring so they do not open off-screen.
5. Session compatibility must avoid rejecting older layouts.

## Definition Of Done

1. Four placements are available through host/ribbon properties.
2. Existing top placement remains compatible.
3. Placement persists and restores.
4. All four placements are covered by focused FX tests.
5. Samples expose the feature for manual review.
6. `SamplesApp` can use the ribbon interface to show and launch available samples.
7. Provider-authoring/session docs are updated.
