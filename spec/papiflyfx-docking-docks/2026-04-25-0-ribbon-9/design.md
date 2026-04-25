# Design - Ribbon Side Placement

**Lead Agent:** @core-architect
**Design Support:** @ui-ux-designer
**Validation:** @qa-engineer
**Spec Steward:** @spec-steward

## Problem

`RibbonDockHost` currently treats the ribbon as top chrome. This is a good default for desktop productivity layouts, but it prevents applications from reserving horizontal space for persistent command surfaces, using bottom command bars, or matching host-specific ergonomics where the dock content is wider than it is tall.

Ribbon placement must become a host-level layout choice without forcing feature modules or `RibbonProvider`s to know where the ribbon is rendered.

## Goals

1. Support `TOP`, `LEFT`, `RIGHT`, and `BOTTOM` ribbon placements.
2. Let each application host specify which side should contain the ribbon: top, bottom, left, or right.
3. Keep `TOP` as the default and preserve current behavior for existing hosts.
4. Keep ribbon providers placement-agnostic; tabs, groups, controls, QAT ids, and contextual visibility remain unchanged.
5. Preserve session state for minimized flag, selected tab, Quick Access Toolbar ids, and the new placement value.
6. Keep orientation styling tokenized through `-pf-ui-*` and `-fx-ribbon-*` variables.
7. Maintain keyboard accessibility and readable labels in each placement.

## Non-Goals

- New ribbon provider SPI for side-specific tabs.
- Separate command definitions for vertical ribbons.
- Customizable per-tab placement.
- Detachable/floating ribbon windows.
- Gallery controls or keytips.

## Samples App Direction

`SamplesApp` should be refactored to use the ribbon interface as a first-class sample discovery surface. The application should be able to expose available samples as ribbon tabs, groups, menus, or commands while still preserving the existing catalog data model.

Expected behavior:

1. Sample categories map naturally to ribbon tabs or groups.
2. Individual samples are launched through ribbon commands.
3. The ribbon placement selector can be demonstrated from the samples app.
4. The existing sample list/catalog remains available as a fallback or secondary navigation aid until the ribbon workflow is proven.
5. Sample command labels, menu labels, and placement controls remain readable in all four placements.

## Public API Shape

Add a small enum in `papiflyfx-docking-docks`:

```java
public enum RibbonPlacement {
    TOP,
    LEFT,
    RIGHT,
    BOTTOM
}
```

Expose placement on both the host and the ribbon:

```java
public ObjectProperty<RibbonPlacement> placementProperty();
public RibbonPlacement getPlacement();
public void setPlacement(RibbonPlacement placement);
```

`RibbonDockHost` owns where the ribbon sits relative to the dock content. `Ribbon` owns how its header, tab strip, group scroller, and command groups adapt to the selected placement.

Applications may specify the host placement before showing the scene, through configuration, or through an application-level preference. When specified, the host should render the ribbon on that side; when omitted, the host renders the ribbon on top.

## Layout Model

| Placement | Host Region | Ribbon Orientation | Primary Scroll Axis |
| --- | --- | --- | --- |
| `TOP` | `BorderPane.top` | Horizontal | Horizontal |
| `BOTTOM` | `BorderPane.bottom` | Horizontal | Horizontal |
| `LEFT` | `BorderPane.left` | Vertical | Vertical |
| `RIGHT` | `BorderPane.right` | Vertical | Vertical |

Top and bottom placements keep the current ribbon structure: QAT, tabs, collapse button, then command groups.

Left and right placements should use a vertical command surface:

1. Header area stacks QAT, tab strip, and collapse button in a compact vertical column.
2. Tabs are arranged vertically, not rotated text.
3. Groups stack vertically and scroll on the Y axis.
4. Command controls keep readable horizontal labels where space permits; do not rotate text.
5. Adaptive group reduction uses available height for vertical placements instead of width.

## Minimized Behavior

Minimized state remains one boolean across all placements.

| Placement | Minimized Chrome |
| --- | --- |
| `TOP` | Header row only |
| `BOTTOM` | Header row only at bottom |
| `LEFT` | Header column only |
| `RIGHT` | Header column only |

Activating a tab while minimized should reveal the selected tab command panel without flipping the persisted minimized flag, matching the Ribbon 8 behavior.

## Adaptive Layout

The current width estimator becomes axis-aware:

```java
double estimateExtent(RibbonGroupSizeMode mode, Orientation orientation)
```

For horizontal placements, the extent is width. For vertical placements, the extent is height. The same `collapseOrder` semantics apply in all placements:

1. Lower `collapseOrder` collapses first.
2. Ties collapse from higher visual order down.
3. Restore happens in the reverse order.

Vertical placements should prefer `MEDIUM` and `SMALL` controls that avoid text clipping rather than forcing large cards into a narrow rail.

## Persistence

Extend ribbon session payload with an optional placement field:

```json
{
  "minimized": false,
  "selectedTabId": "home",
  "quickAccessCommandIds": ["save"],
  "placement": "TOP"
}
```

Compatibility rules:

1. Missing placement restores as `TOP`.
2. Unknown placement restores as `TOP` and does not invalidate the rest of the ribbon state.
3. Malformed non-string placement invalidates only the placement field, not minimized, selected tab, or QAT ids.
4. Existing saved sessions remain valid.

## Styling

Add orientation classes:

- `.pf-ribbon-placement-top`
- `.pf-ribbon-placement-bottom`
- `.pf-ribbon-placement-left`
- `.pf-ribbon-placement-right`
- `.pf-ribbon-orientation-horizontal`
- `.pf-ribbon-orientation-vertical`

The side placements need explicit CSS for:

1. Header column spacing and alignment.
2. Vertical tab strip width, selected/focused states, and contextual accents.
3. Vertical group scroller padding and scroll-bar policy.
4. Group width budgets for side rails.
5. Menu/split-button label and arrow contrast in side placement.

## Accessibility

1. The selected tab remains a toggle in the same toggle group.
2. Focus order should move through QAT, tabs, collapse button, then visible commands for the active placement.
3. Focus indicators must remain visible on all four sides.
4. No labels should be rotated; screen-reader text and visible text should stay aligned.
5. Minimized tab activation must not strand focus in an empty command panel.

## Open Questions

1. Should placement be user-changeable through a built-in ribbon command or only host-configurable?
2. Should vertical side ribbons default to a fixed width or derive width from the largest visible group?
3. Should bottom placement render QAT before tabs, matching top, or nearest the content edge?
4. Should samples expose placement switching in a toolbar, settings panel, separate sample scene, or the new samples ribbon itself?
5. Should `SamplesApp` eventually replace the existing catalog navigation with ribbon navigation, or keep both permanently?
