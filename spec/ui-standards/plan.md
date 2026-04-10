# UI Standardization Plan

## Objective
The goal is to provide a unified look and feel across all PapiflyFX Docking components by standardizing CSS, spacing, 
and interaction patterns through a multiagent workflow.

## Phased Approach

### Phase 1: Audit & Standards Definition (Lead: @ui-ux-designer)
- [x] Audit existing UI/UX across all modules.
- [x] Research and define a set of shared CSS variables in `papiflyfx-docking-api` or a common UI module.
- [x] Document the standard spacing system (e.g., 4px grid) and corner radii (e.g., 4px).
- [x] Define standard visual states for common components (Hover, Active, Focused, Selected).

### Phase 2: Shared Component Extraction (Lead: @ui-ux-designer, Contributor: @feature-dev)
- [x] Identify common UI patterns (Pills, Chips, Status Slots, Popups).
- [x] Move reusable UI components to a shared location (e.g., `papiflyfx-docking-api` if light, or a new `papiflyfx-docking-ui-common` module).
- [x] Update components to use the new shared CSS variables.

### Phase 3: Module-Level Refactoring (Lead: @feature-dev, Reviewer: @ui-ux-designer)
- [x] **GitHub Module**: Align `github-toolbar.css` with the new shared standards.
- [x] **Code Module**: Update `CodeEditorTheme` and canvas mappers to follow the new color/spacing guidelines.
- [x] **Tree Module**: Update `TreeViewTheme` and canvas mappers.
- [x] **Media/Hugo Modules**: Ensure full `Theme` property binding and UI layout alignment.

### Phase 4: Validation & Testing (Lead: @qa-engineer, Reviewer: @ui-ux-designer)
- [x] Update existing `FxTest` classes to include visual/layout assertions.
- [x] Verify that theme switching works correctly in both light and dark modes across all modules.
- [x] Ensure that layout density settings (e.g., compact vs. spacious) are respected.

### Phase 5: Documentation & Guidelines Update (Lead: @spec-steward)
- [x] Update the root `README.md` and module-level docs to include the new UI standards.
- [x] Update `spec/agents/playbook.md` with the finalized multiagent UI workflow.

## Closure Status
- [x] Planned Phase 1-5 rollout is complete.
- [x] Final polish sweep completed for standardized UI chrome, overlays, shared primitives, spacing, and theme propagation.

## Verification Notes
- Editor syntax palettes in `CodeEditorTheme` remain module-owned defaults and are not part of the shared chrome token set.
- Final verification included targeted FX regressions plus a full `./mvnw -Dtestfx.headless=true test` pass.

## Multiagent Workflow

| Phase | Lead | Reviewers |
| --- | --- | --- |
| 1. Standards Definition | `@ui-ux-designer` | `@core-architect`, `@spec-steward` |
| 2. Shared Components | `@ui-ux-designer` | `@feature-dev`, `@core-architect` |
| 3. Feature Refactoring | `@feature-dev` | `@ui-ux-designer`, `@qa-engineer` |
| 4. Validation | `@qa-engineer` | `@ui-ux-designer`, `@ops-engineer` |
| 5. Documentation | `@spec-steward` | `@ui-ux-designer`, `@ops-engineer` |

## Acceptance Criteria
- [x] No hardcoded colors or off-grid spacing remain in standardized module UI chrome, overlays, dialogs, settings panes, or shared controls.
- [x] All UI components react consistently to `Theme` changes (colors, fonts, dimensions).
- [x] Shared components (Pills, Chips, Status) are used across at least two different modules.
- [x] UI test suite passes in headless mode with high coverage for theme/layout states.
