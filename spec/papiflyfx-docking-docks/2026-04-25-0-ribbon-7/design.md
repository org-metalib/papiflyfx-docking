# Design - Ribbon Menu Theme Background

**Lead Agent:** @ui-ux-designer  
**Owning Module:** `papiflyfx-docking-docks`  
**Reviewers:** @core-architect, @qa-engineer, @spec-steward

## Problem

Ribbon buttons, groups, and collapsed group popups already consume the shared `-pf-ui-*` token vocabulary through `RibbonThemeSupport.themeVariables(...)`. Ribbon menu and split-button popups are different: JavaFX renders their menu content in popup scenes, so the popup background can fall back to the platform/default light menu styling while the application is using `Theme.dark()`.

The fix must also preserve `Theme.light()` behavior instead of hardcoding a dark popup color.

## Design Direction

1. Treat ribbon menu popups as ribbon chrome, not generic application context menus.
2. Apply the existing ribbon theme variable block to the `ContextMenu` used by `MenuButton` and `SplitMenuButton`.
3. Keep popup styling tokenized through `RibbonThemeSupport` and `-pf-ui-*` values.
4. Style menu popup backgrounds, borders, text, hover, pressed, disabled, and icon fills from tokens.
5. Update open popups when the ribbon theme changes live.
6. Avoid public API changes; provider descriptors remain `RibbonMenuSpec` and `RibbonSplitButtonSpec`.

## Expected Visual Result

| Surface | Dark Theme | Light Theme |
| --- | --- | --- |
| Menu popup background | Matches ribbon/header surface family, no white popup flash | Light surface with clear border |
| Menu item text | Uses `-pf-ui-text-primary` | Uses `-pf-ui-text-primary` |
| Hover/armed item | Uses shared control hover/pressed tokens | Uses shared control hover/pressed tokens |
| Disabled item | Uses shared disabled text/icon token | Uses shared disabled text/icon token |

## Constraints

- No FXML.
- No new global styling system.
- No hardcoded dark-only palette.
- No public ribbon SPI change.
- Keep the change scoped to ribbon menu/split-button chrome and focused regression coverage.
