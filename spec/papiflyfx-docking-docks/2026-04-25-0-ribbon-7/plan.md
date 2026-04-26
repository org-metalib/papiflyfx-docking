# Plan - Ribbon Menu Theme Background

**Lead Agent:** @ui-ux-designer  
**Implementation Support:** @core-architect  
**Validation:** @qa-engineer  
**Spec Steward:** @spec-steward

## Scope

Fix ribbon menu and split-button popup background colors so they honor both `Theme.dark()` and `Theme.light()`.

## Tasks

1. Inspect current ribbon theme propagation and JavaFX popup styling behavior.
2. Add ribbon-specific menu popup CSS using existing `-pf-ui-*` and `-fx-ribbon-*` tokens.
3. Apply `RibbonThemeSupport.themeVariables(...)` to menu popups created by ribbon menu and split-button controls.
4. Ensure live theme switches update already-open menu popups.
5. Add a focused TestFX regression covering dark and light menu popup styling.
6. Run focused docks ribbon tests and `git diff --check`.
7. Record implementation and validation status in `progress.md`.

## Acceptance Criteria

1. Ribbon menu popup background is not out of theme in dark mode.
2. Light theme popup background remains light and readable.
3. Menu item hover, pressed, disabled, text, and icon colors use shared tokens.
4. No public ribbon API changes are introduced.
5. Focused regression tests pass in headless mode.

## Validation Commands

```bash
./mvnw -pl papiflyfx-docking-docks -am -Dtest=RibbonAdaptiveLayoutFxTest -Dtestfx.headless=true test
git diff --check
```
