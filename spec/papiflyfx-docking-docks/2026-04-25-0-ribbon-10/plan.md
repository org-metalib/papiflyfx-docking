# Plan - Ribbon 10 Side Toolbar Placement

**Lead Agent:** @core-architect
**Design Support:** @ui-ux-designer
**Validation:** @qa-engineer
**Spec Steward:** @spec-steward

## Scope

Replace the Ribbon 9 vertical side-ribbon visual model with a compact side toolbar model for `LEFT` and `RIGHT` placements while preserving the Ribbon 9 public placement API, provider contracts, command discovery, contextual tab behavior, minimized state, Quick Access Toolbar ids, and session compatibility.

`TOP` and `BOTTOM` remain horizontal ribbons.

## Goals

1. Keep `RibbonPlacement` and placement properties unchanged.
2. Keep existing sessions compatible, including `LEFT` and `RIGHT` placement values.
3. Replace side placement’s wide persistent command pane with a compact outside toolbar rail.
4. Reveal selected-tab commands through transient popovers/panels over dock content.
5. Keep providers placement-agnostic.
6. Use accessible, readable, non-rotated controls.
7. Preserve QAT, contextual tabs, selected tab, and minimized state semantics.
8. Keep styling tokenized with existing `-pf-ui-*` and `-fx-ribbon-*` variables.

## Non-Goals

1. No provider SPI changes.
2. No TOP/BOTTOM behavior redesign.
3. No full IDE tool-window subsystem.
4. No user-customizable side toolbar schema.
5. No Maven/build wiring changes unless tests require sample selection updates.

## Key Invariants

1. Existing applications that do not set placement still render a `TOP` ribbon.
2. Existing saved sessions without placement still restore successfully as `TOP`.
3. Unknown or malformed placement restores placement only as `TOP` without invalidating minimized state, selected tab id, or QAT ids.
4. `LEFT` and `RIGHT` keep the rail on the outside edge of the host.
5. Ribbon providers remain unaware of placement.
6. Side toolbar commands stay reachable by keyboard and expose accessible text/tooltips.
7. Minimized side activation does not clear the persisted minimized flag.

## Proposed Design

For side placement, `Ribbon` should render:

1. A compact side toolbar rail containing both QAT commands and tab/action-group selectors.
2. A transient command popover anchored from the side rail and opened over dock content.
3. Popover content built from the existing selected `RibbonTabSpec` groups and controls.

The current Ribbon 9 side edge strip can likely be reused as the starting container. The Ribbon 9 persistent `sideContentPane` should be removed from normal side layout or repurposed solely as the popover content root. Ribbon 10 should not keep an opt-in compatibility path for the previous persistent side command pane.

## Decisions

1. Side rail content supports both Quick Access Toolbar commands and ribbon tab/action-group entries.
2. Selected side tabs do not need to remain visually selected after the popover closes.
3. Minimized side placement keeps the rail visible but suppresses popover activation.
4. Text-only side rail commands use generated initials or fallback glyphs.
5. The Ribbon 9 side command pane is not retained as an opt-in compatibility mode.

## Implementation Phases

### Phase 1 - UX Contract And Internal Shape

Lead: @core-architect  
Reviewers: @ui-ux-designer, @spec-steward

Tasks:

1. Define the final ordered rail model for QAT commands plus tab/action-group entries.
2. Define focus order for side rail and popover.
3. Define minimized side suppression rules for hover/click/keyboard activation.
4. Define fallback glyph generation rules for text-only rail commands.

Acceptance:

- The side toolbar interaction model is explicit enough to implement without guessing.
- Public API and provider contracts remain unchanged.

### Phase 2 - Runtime Layout Refactor

Lead: @core-architect  
Reviewers: @ui-ux-designer, @qa-engineer

Tasks:

1. Replace side placement’s persistent command pane with a compact rail-only default layout.
2. Reuse existing command/group rendering for transient popover content.
3. Keep `LEFT` and `RIGHT` rail ordering mirrored so the rail remains outside the dock content.
4. Ensure selected tabs/contextual tabs still update from `RibbonManager`.
5. Preserve minimized flag behavior and session capture/restore.
6. Suppress side popover activation while side placement is minimized.

Acceptance:

- `LEFT` and `RIGHT` render compact side rails by default.
- Activating a side rail entry reveals command content transiently.
- Provider discovery and QAT id behavior are unchanged.

### Phase 3 - Styling And Accessibility

Lead: @core-architect  
Design Support: @ui-ux-designer  
Reviewers: @qa-engineer

Tasks:

1. Add/adjust side-toolbar CSS classes using `-pf-ui-*` and `-fx-ribbon-*` tokens.
2. Ensure text-only actions remain understandable without rotated labels.
3. Add tooltip and accessible text coverage for side rail buttons.
4. Verify focus indicators on rail buttons and popover commands.
5. Avoid nested flyout/popover confusion for collapsed groups.
6. Add generated initials/fallback glyph treatment for text-only rail commands.

Acceptance:

- Side toolbar reads as a compact IDE-style action rail, not as a vertical ribbon.
- Keyboard users can reach rail buttons and popover commands.
- Visual states are tokenized and theme-compatible.

### Phase 4 - Tests, Samples, Docs

Lead: @core-architect  
Reviewers: @qa-engineer, @spec-steward, @ui-ux-designer

Tasks:

1. Update Ribbon 9 placement tests to assert side toolbar layout instead of persistent side pane.
2. Add focused tests for side rail activation, popover visibility, minimized flag preservation, and focus return.
3. Update `RibbonPlacementSample` to demonstrate the toolbar model.
4. Update ribbon docs and status/release notes.
5. Record validation in `progress.md`.

Acceptance:

- Docks ribbon-focused tests pass headlessly.
- Samples smoke/ribbon tests pass headlessly.
- Documentation describes side toolbar placement accurately.

## Suggested Validation

```bash
./mvnw -pl papiflyfx-docking-docks -am '-Dtest=Ribbon*Test,Ribbon*FxTest' -Dsurefire.failIfNoSpecifiedTests=false -Dtestfx.headless=true test
./mvnw -pl papiflyfx-docking-samples -am '-Dtest=*Ribbon*FxTest,SamplesSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false -Dtestfx.headless=true test
git diff --check
```

## Required Reviewer Handoff

- `@ui-ux-designer`: approve compact rail, popover behavior, visual states, spacing, and accessibility ergonomics.
- `@qa-engineer`: approve regression coverage for session compatibility, side activation, focus, and sample registration.
- `@spec-steward`: approve docs/progress closure and definition-of-done alignment.
- `@ops-engineer`: only needed if sample/build wiring changes beyond catalog registration.
