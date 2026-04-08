# UI Standardization Research

## Context
The PapiflyFX Docking framework is a multi-module JavaFX project with several content modules (`code`, `tree`, `media`, `hugo`, `github`). While a central `Theme` record handles colors, fonts, and dimensions, individual modules implement their own CSS and UI layouts, leading to potential inconsistencies in spacing, interactions, and visual style.

## Key Findings (Current State)

### 1. Central Theme System
- **Module**: `papiflyfx-docking-api`
- **Mechanism**: `Theme` record (colors, fonts, dimensions) + `ObjectProperty<Theme>` bindings.
- **Support**: Most modules use `bindThemeProperty(...)` or listeners to update programmatic UI.
- **Inconsistencies**: Some modules might not fully bind all properties (e.g., custom dimensions or accent colors).

### 2. Module-Local CSS
- **Affected Modules**: `papiflyfx-docking-code`, `papiflyfx-docking-tree`, `papiflyfx-docking-github`.
- **Inconsistencies**:
    - `github-toolbar.css` uses specific style classes for pills and chips.
    - Code editor and tree view have their own theme-to-color mappers.
    - Risk of "magic numbers" for padding, margins, and corner radii across different CSS files.

### 3. Interaction Patterns
- **Floating Windows**: Handled by `DockManager` but content may not correctly react to window state changes (e.g., focus/blur).
- **Popups**: `GitRefPopup` in `github` module uses a custom controller/view model. Other modules might need similar popup patterns (e.g., search, settings).

### 4. Component Gaps
- **Standard Pills/Chips**: Used in GitHub but could be generalized for other metadata.
- **Status Indicators**: `StatusSlot` in GitHub is transient; other modules lack a unified way to show progress/status within a dock.

## Target Areas for Standardization
- [ ] **Shared CSS Variables**: Define a set of standard CSS variables derived from the `Theme` record (e.g., `-fx-papifly-accent`, `-fx-papifly-bg-subtle`).
- [ ] **Spacing & Layout**: Standardize padding (4px/8px steps) and corner radii (4px/8px).
- [ ] **Common Controls**: Extract `Pill`, `Chip`, and `StatusSlot` patterns into a shared UI utility or API.
- [ ] **Theme Binding**: Ensure 100% coverage of `Theme` property bindings in all content modules.

## Multiagent Workflow for Standardization
- **Lead**: `@ui-ux-designer`
- **Auditor**: `@spec-steward`
- **Reviewers**: `@feature-dev` (content), `@core-architect` (API), `@qa-engineer` (regressions).
