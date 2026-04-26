# Plan - Ribbon Collapse/Expand Icon

**Lead Agent:** @ui-ux-designer
**Implementation Support:** @core-architect
**Validation:** @qa-engineer
**Spec Steward:** @spec-steward

## Scope

Convert the ribbon header collapse/expand text button to a compact icon-only control.

## Tasks

1. Inspect current ribbon header control construction and minimized-state update path.
2. Replace state text with a state-specific chevron graphic.
3. Add tokenized CSS for sizing, hover, pressed, focus, and icon stroke.
4. Preserve tooltip and accessible text for both states.
5. Keep minimized tab activation from clearing the selected tab's rendered command groups.
6. Add focused TestFX regressions that verify icon-only rendering, accessible state copy, and minimized tab command-panel rendering.
7. Run focused docks ribbon tests and `git diff --check`.
8. Record implementation and validation status in `progress.md`.

## Acceptance Criteria

1. No visible `Collapse` or `Expand` label remains on the header button.
2. The button graphic changes with minimized state.
3. Tooltip and accessible text still describe the action.
4. Existing ribbon session persistence remains compatible.
5. Minimized tab activation displays the selected tab's commands instead of an empty panel.
6. Focused regression tests pass in headless mode.

## Validation Commands

```bash
./mvnw -pl papiflyfx-docking-docks -am -Dtest=RibbonAdaptiveLayoutFxTest -Dtestfx.headless=true test
git diff --check
```
