# Design - Ribbon Collapse/Expand Icon

**Lead Agent:** @ui-ux-designer
**Implementation Support:** @core-architect
**Validation:** @qa-engineer
**Spec Steward:** @spec-steward

## Problem

The ribbon header collapse/expand affordance is the only header action that presents as a full text button. The text changes between `Collapse` and `Expand`, which creates a wider visual target than needed and can shift the header action area when the minimized state toggles.

## Design Direction

1. Keep the existing `Ribbon#minimizedProperty()` and `RibbonSessionData.minimized()` contracts unchanged.
2. Render the control as an icon-only compact action.
3. Use an upward chevron while expanded to indicate collapse, and a downward chevron while minimized to indicate expand.
4. Keep the accessible name and tooltip descriptive: `Collapse ribbon` and `Expand ribbon`.
5. Style the control from existing shared `-pf-ui-*` tokens for border, hover, pressed, focus, and icon stroke.
6. Keep selected tab groups rendered even while minimized, and reveal the command panel when a minimized tab is activated.
7. Keep implementation local to ribbon chrome with no public SPI changes.

## Expected Result

| State | Visible control | Accessible name |
| --- | --- | --- |
| Ribbon expanded | Up chevron icon | `Collapse ribbon` |
| Ribbon minimized | Down chevron icon | `Expand ribbon` |

When minimized, activating a tab keeps `Ribbon#isMinimized()` true but displays the selected tab's body so commands remain reachable. Expanding through the icon still leaves minimized mode permanently.

## Constraints

- No FXML.
- No new external icon dependency.
- No new public API.
- No text label inside the button.
- Maintain deterministic headless TestFX coverage.
