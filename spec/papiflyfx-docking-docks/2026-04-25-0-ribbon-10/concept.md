# Concept - Ribbon 10 Side Toolbar Placement

**Lead Agent:** @core-architect
**Design Support:** @ui-ux-designer
**Validation:** @qa-engineer
**Spec Steward:** @spec-steward

## Problem

Ribbon 9 added host-configurable side placement using a vertical ribbon model: an outside edge tab strip plus an inner command content pane and minimized flyout. The behavior preserves ribbon provider contracts, but the side visual language is heavier than desired and does not match the compact side-toolbar style common in IDEs such as IntelliJ IDEA.

Ribbon 10 should pivot side placement from a vertical ribbon surface to a compact side toolbar/tool-window stripe model while preserving the public placement API and session compatibility added in Ribbon 9.

## Target UX

For `LEFT` and `RIGHT` placement, the side chrome should feel like a docked IDE action stripe:

1. A narrow outside rail hosts compact tab/action buttons.
2. The rail is always visible for side placement.
3. Commands appear through lightweight popovers or transient panels, not through a wide persistent command pane by default.
4. Labels are not rotated. Buttons are icon-first or compact text where no icon exists, with full accessible text and tooltips.
5. Groups become toolbar action groups or popover sections. Provider metadata remains unchanged.
6. The dock content keeps as much horizontal space as possible.

The goal is not to copy IntelliJ exactly. The goal is to adopt the same ergonomic shape: compact, fast to scan, tool-window-like, and less visually dominant than a vertical ribbon.

## Mental Model

Ribbon 9 side placement:

```text
LEFT:  [tab strip] [wide command pane] [dock content]
RIGHT: [dock content] [wide command pane] [tab strip]
```

Ribbon 10 side toolbar:

```text
LEFT:  [compact toolbar rail] [dock content]
RIGHT: [dock content] [compact toolbar rail]

On activation:
LEFT:  [compact toolbar rail] [popover over dock content]
RIGHT: [popover over dock content] [compact toolbar rail]
```

## Behavior

### Side Rail

The side rail should contain:

- Quick Access Toolbar commands, rendered as compact icon-first buttons.
- Ribbon tab selectors or action-group buttons derived from the same `RibbonTabSpec` data.
- Both QAT commands and tab/action-group entries in one side rail model.
- A collapse/expand affordance only if it has a useful state in the new model.

Because the side rail itself is already compact, minimized side placement should keep the rail visible but suppress popover activation behavior. The persisted minimized flag still round-trips for compatibility.

### Popovers

When a side tab/action group is activated:

- A transient popover opens over dock content.
- The popover renders selected tab groups as compact sections.
- The popover auto-hides on escape, outside click, or focus loss where JavaFX supports that deterministically.
- Activating a side tab while minimized must not clear the minimized flag.
- A selected side tab does not need to remain visually selected after the popover closes.

### Groups And Commands

Groups should map to toolbar-friendly sections:

- Large commands become compact icon/text buttons or full-width popover rows.
- Medium/small controls can wrap in a compact grid inside the popover.
- Menu and split-button controls remain available.
- Collapsed group popups should not create nested flyout confusion; prefer one popover surface per activation.
- Text-only side rail commands should use generated initials or fallback glyphs instead of visible long labels in the rail.

### Horizontal Placements

`TOP` and `BOTTOM` keep the Ribbon 9 horizontal ribbon model unchanged.

## API And Session Compatibility

Ribbon 10 should keep:

- `RibbonPlacement.TOP`
- `RibbonPlacement.LEFT`
- `RibbonPlacement.RIGHT`
- `RibbonPlacement.BOTTOM`
- `RibbonDockHost#placementProperty()`
- `RibbonDockHost#getPlacement()`
- `RibbonDockHost#setPlacement(...)`
- `Ribbon#placementProperty()`
- `Ribbon#getPlacement()`
- `Ribbon#setPlacement(...)`
- `extensions.ribbon.placement`

No `RibbonProvider` changes should be required.

Existing saved sessions with `LEFT` or `RIGHT` should restore as side toolbar placements. Missing, unknown, or malformed placement values should continue to restore placement as `TOP` without dropping minimized state, selected tab id, or QAT ids.

Ribbon 10 does not need an opt-in compatibility path for the Ribbon 9 persistent side command pane. `LEFT` and `RIGHT` should move fully to the side toolbar model.

## Styling Direction

Use the existing token vocabulary:

- `-pf-ui-*`
- `-fx-ribbon-*`

Expected style classes may include:

- Existing placement/orientation classes from Ribbon 9.
- `.pf-ribbon-side-toolbar`
- `.pf-ribbon-side-toolbar-button`
- `.pf-ribbon-side-toolbar-popover`
- `.pf-ribbon-side-toolbar-section`

The design should avoid a wide permanent side pane as the default visual state.

## Non-Goals

- New provider SPI for side-specific command metadata.
- Full IntelliJ tool-window lifecycle such as pinned/unpinned windows, drag reorder, or custom tool-window registration.
- Per-user toolbar customization.
- Keytips.
- Reworking `TOP` or `BOTTOM` ribbon behavior.

## Decisions

1. Side rail buttons should support both QAT commands and ribbon tab/action-group entries.
2. A selected side tab does not need to remain visually selected after its popover closes.
3. Minimized side placement keeps the compact rail visible but suppresses popover activation behavior.
4. Text-only side rail commands use generated initials or fallback glyphs instead of long visible labels.
5. Ribbon 10 should not retain the Ribbon 9 persistent side command pane behind an opt-in compatibility flag.

## Open Questions

No open product questions remain from the initial Ribbon 10 concept pass.
