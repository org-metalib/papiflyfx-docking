# Prompt - Execute Ribbon Menu Theme Background Plan

Use this prompt to start or resume the implementation session for `spec/papiflyfx-docking-docks/2026-04-25-0-ribbon-7`.

```text
As @ui-ux-designer, lead this PapiflyFX Docking ribbon chrome fix.

Read these first:
- AGENTS.md
- spec/agents/README.md
- spec/agents/playbook.md
- spec/agents/ui-ux-designer.md
- spec/agents/core-architect.md
- spec/agents/qa-engineer.md
- spec/papiflyfx-docking-docks/2026-04-25-0-ribbon-7/design.md
- spec/papiflyfx-docking-docks/2026-04-25-0-ribbon-7/plan.md
- spec/papiflyfx-docking-docks/2026-04-25-0-ribbon-7/progress.md

Task:
Fix ribbon menu and split-button popup background colors so they honor both `Theme.dark()` and `Theme.light()`.

Priority:
P2 (Normal)

Implementation lead:
@ui-ux-designer

Implementation support:
@core-architect

Required reviewers:
@core-architect, @qa-engineer, @spec-steward

Impacted modules:
- papiflyfx-docking-docks
- spec/papiflyfx-docking-docks/2026-04-25-0-ribbon-7

Expected implementation:
1. Inspect `Ribbon`, `RibbonGroup`, `RibbonControlFactory`, `RibbonControlStrategies`, `RibbonThemeSupport`, and `ribbon.css`.
2. Confirm how `RibbonThemeSupport.themeVariables(...)` reaches the main ribbon and collapsed group popups.
3. Add ribbon-specific menu popup CSS using existing `-pf-ui-*` and `-fx-ribbon-*` tokens.
4. Apply `RibbonThemeSupport.themeVariables(...)` to JavaFX `ContextMenu` popups created by ribbon `MenuButton` and `SplitMenuButton` controls.
5. Ensure already-open menu popups update when the ribbon theme switches between dark and light.
6. Add focused TestFX coverage that opens a ribbon menu popup and verifies dark and light theme variables.
7. Update `progress.md` with implementation notes, validation results, and remaining risks.

Key invariants:
- No FXML.
- No hardcoded dark-only colors.
- No new global styling system.
- No public ribbon API/SPI changes.
- Use the shared token vocabulary already emitted by `RibbonThemeSupport` and `UiCommonThemeSupport`.
- Keep the fix scoped to ribbon menu and split-button popup chrome.
- Preserve existing ribbon command, adaptive layout, collapsed popup, and provider behavior.

Validation expectations:
- `./mvnw -pl papiflyfx-docking-docks -am -Dtest=RibbonAdaptiveLayoutFxTest -Dsurefire.failIfNoSpecifiedTests=false -Dtestfx.headless=true test`
- `git diff --check`

Acceptance criteria:
- Ribbon menu popup background is not out of theme in dark mode.
- Light theme popup background remains light and readable.
- Menu item hover, pressed, disabled, text, and icon states use shared tokens.
- Open menu popups update when the active ribbon theme changes.
- No public ribbon API changes are introduced.
- Focused headless regression tests pass.

Close with:
1. Files changed
2. Validation commands and results
3. Any remaining risks
4. Handoff block using the repository handoff contract
```
