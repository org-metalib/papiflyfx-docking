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

Placement is host-configurable for Ribbon 9. The built-in ribbon chrome should not add a user-facing placement switch command in this slice.

## Layout Model

| Placement | Host Region | Ribbon Orientation | Primary Scroll Axis |
| --- | --- | --- | --- |
| `TOP` | `BorderPane.top` | Horizontal | Horizontal |
| `BOTTOM` | `BorderPane.bottom` | Horizontal | Horizontal |
| `LEFT` | `BorderPane.left` | Vertical | Vertical |
| `RIGHT` | `BorderPane.right` | Vertical | Vertical |

Top and bottom placements keep the current ribbon structure: QAT, tabs, collapse button, then command groups. Bottom placement mirrors top header ordering instead of moving QAT nearest the content edge.

Left and right placements should preserve the ribbon concept rather than becoming a generic sidebar. The side ribbon is split into two vertical surfaces:

1. **Edge tab strip:** a narrow strip on the outside edge of the host. For `RIGHT`, the strip sits at the far right; for `LEFT`, it sits at the far left. It contains the QAT affordance, tab toggles, and collapse/expand control.
2. **Inner content pane:** a wider panel between dock content and the edge tab strip. It renders the selected tab's command groups.

For `RIGHT`, the visual order is dock content, command content pane, then edge tab strip. For `LEFT`, the order mirrors that: edge tab strip, command content pane, then dock content.

Side placement behavior:

1. Tabs are arranged vertically in the edge strip, but labels must remain readable and non-rotated. Use icon-first or icon-plus-short-label tab buttons with tooltips and accessible names; do not require provider-specific tab icon APIs for this slice.
2. Groups stack vertically in the inner content pane and scroll on the Y axis when content exceeds available height.
3. Group titles move from the bottom footer pattern used by horizontal ribbons to the top of each side group, reading as section headers or dividers such as `Front Matter` and `Shortcodes`.
4. Large command buttons span the available content-pane width. Smaller controls may wrap in a `FlowPane` or `TilePane` grid inside the group.
5. Adaptive group reduction uses available height for vertical placements instead of width.
6. The side content pane should use a theme token for constrained width with responsive min/pref/max values, while the edge tab strip remains compact.

### Side Ribbon Collapsed Mode

Side placement collapse should reduce only the inner content pane and keep the edge tab strip visible. This preserves a compact side rail while freeing horizontal workspace.

When minimized and the user activates a tab in the edge strip, the selected tab's content pane should appear as a temporary overlay flyout over the dock content. This activation must not flip the persisted minimized flag.

This is the side-placement equivalent of Ribbon 8 minimized tab activation: users can inspect and invoke selected-tab commands transiently while the saved minimized state remains `true`.

## Minimized Behavior

Minimized state remains one boolean across all placements.

| Placement | Minimized Chrome |
| --- | --- |
| `TOP` | Header row only |
| `BOTTOM` | Header row only at bottom |
| `LEFT` | Edge tab strip only; command content appears transiently on tab activation |
| `RIGHT` | Edge tab strip only; command content appears transiently on tab activation |

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

Vertical placements should prefer `MEDIUM` and `SMALL` controls that avoid text clipping rather than forcing large cards into a narrow rail. Group rendering should be orientation-aware: horizontal ribbons keep group labels in the existing footer location, while side ribbons place group labels at the top of the group.

Accordion-style group collapse is in scope for side placement because vertical content can grow long. It should collapse group sections without requiring provider-specific metadata or side-specific provider SPI.

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

1. Edge tab strip width, spacing, selected/focused states, contextual accents, and tooltip affordances.
2. Inner content-pane width budgets with min/pref/max constraints.
3. Vertical group scroller padding and scroll-bar policy.
4. Side group header styling at the top of each group.
5. Full-width large command buttons and wrapped grid treatment for smaller controls.
6. Menu/split-button label and arrow contrast in side placement.

## Accessibility

1. The selected tab remains a toggle in the same toggle group.
2. Focus order should move through QAT, tabs, collapse button, then visible commands for the active placement. In side placement, the edge tab strip is first-class keyboard chrome and must remain reachable while the content pane is collapsed.
3. Focus indicators must remain visible on all four sides.
4. No labels should be rotated; screen-reader text and visible text should stay aligned. Icon-first tabs require accessible text and tooltips that expose the full tab label.
5. Minimized tab activation must not strand focus in an empty command panel.

## SamplesApp Demo

Add a `SamplesApp` catalog demo that makes the placement behavior easy to inspect without custom setup.

The demo should show two ribbon hosts side by side or through a compact switcher:

1. A default `TOP` ribbon host that demonstrates compatibility with the existing ribbon shell.
2. A `LEFT` ribbon host using the same sample provider data so users can compare orientation, tab readability, command grouping, QAT behavior, and minimized activation.

The demo should stay deterministic and local-only. It should not require external services, authentication, or network access. The top and left examples should use the same ribbon tabs and representative command controls so placement differences are visible without provider-specific behavior.

Sample validation should include `SamplesSmokeTest` coverage that the demo is registered in the catalog and can be built headlessly. Add focused sample assertions if the implementation exposes stable node ids or placement properties that can be checked without brittle visual assumptions.

## Decisions

1. Placement is host-configurable only for Ribbon 9. No built-in ribbon command switches placement.
2. Side ribbon command content pane width is controlled by a theme token, with responsive min/pref/max constraints.
3. Bottom placement mirrors top header ordering.
4. Minimized side tab activation reveals command content through an overlay flyout.
5. Side group sections use accordion-style collapse in Ribbon 9.
