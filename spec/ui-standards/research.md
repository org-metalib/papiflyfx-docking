# UI Standardization Research

## Phase 1 Scope
- Lead: `@ui-ux-designer`
- Required reviewers: `@core-architect`, `@spec-steward`
- Objective: audit the current UI standards surface in `papiflyfx-docking-code`, `papiflyfx-docking-tree`, and `papiflyfx-docking-github`, then define a shared token model for later refactoring phases.

## Baseline Constraints
- The current runtime binding model is sound: `CodeEditor`, `TreeView`, and `GitHubToolbar` already react to `ObjectProperty<Theme>` changes via `bindThemeProperty(...)`.
- The existing `Theme` record provides only foundation-level colors, fonts, and dimensions: background, header/background-active, accent, text, border/divider, drop-hint, fonts, corner radius, border width, header/tab heights, content padding, button states, and minimized-bar metrics.
- Phase 1 must preserve compatibility with that model. Because `Theme` is a Java record and is already used as a shared contract, the standardization path should be additive:
  - keep `ObjectProperty<Theme>` as the single binding source
  - add shared token emitters and derived helpers in the `papiflyfx-docking-api` module
  - avoid widening the `Theme` record signature as part of the first rollout

## Audit Findings

### 1. Every module has theme plumbing, but each defines its own token vocabulary
- `papiflyfx-docking-github` maps `Theme` into `GitHubToolbarTheme`, then emits module-local CSS variables in `GitHubThemeSupport.themeVariables(...)`.
- `papiflyfx-docking-code` emits a different token set inline from `SearchController.applyThemeColors(...)` and `GoToLineController.applyThemeColors(...)`.
- `papiflyfx-docking-tree` emits a third token set inline from `TreeSearchOverlay.setTheme(...)`.
- Result: the same UI concepts exist three times under different names, which blocks reuse and makes audits slower.

### 2. Hardcoded semantic colors still appear in CSS and Java
- GitHub CSS still hardcodes accent/status fills with inline `rgba(...)` values for the brand badge, ref kind pills, chip variants, popup selection rows, and danger buttons instead of routing them through shared semantic tokens.
  - Examples: `papiflyfx-docking-github/src/main/resources/org/metalib/papifly/fx/github/ui/github-toolbar.css:14-15`, `:89`, `:94`, `:99`, `:104`, `:108`, `:134`, `:139`, `:144`, `:149`, `:257`
  - Example: `papiflyfx-docking-github/src/main/resources/org/metalib/papifly/fx/github/ui/github-dialog.css:74`, `:84`
- Code overlays duplicate default colors in two places:
  - CSS defaults in `search-overlay.css` and `go-to-line-overlay.css`
  - Java fallbacks in `SearchController.applyThemeColors(...)` and `GoToLineController.applyThemeColors(...)`
- Tree search overlay also keeps its own fallback palette and shadow in `TreeSearchOverlay.setTheme(...)`, separate from code search.

### 3. Spacing, radii, and control sizing drift across modules
- GitHub uses several radius systems in one module:
  - pill radius `999`
  - action radius `8`
  - field/dialog radius `5`
  - popup radius `10` and `14`
- Code search/go-to-line overlays use:
  - panel radius `8`
  - field radius `5`
  - chip/action/icon radius `4`
  - row spacing `2`
- Tree search overlay uses:
  - panel radius `8`
  - field radius `5`
  - icon button radius `4`
  - row spacing `4`
- Java layout code adds more independent numbers:
  - code search width `520/620/760`, padding `2/4`, icon inset `20`, overlay offsets `6/16`
  - go-to-line width `260/300/360`, padding `4/6`
  - tree search width `380`, compact threshold `260`, top margin `8`, max-width inset `16`
  - GitHub popup width `392`, popup max height `520`, popup vertical offset `6`, dialog padding `12`
- Result: similar components render with different density even when bound to the same base theme.

### 4. Theme coverage is partial in the code and tree render stacks
- `CodeEditorThemeMapper` only maps a small part of the editor surface from the shared `Theme`:
  - background comes from `Theme.background()`
  - accent is reused for a few bookmark/focus/search states
  - most editor syntax colors, search overlay colors, and scrollbar colors remain dark/light baked-in defaults
- `TreeViewThemeMapper` has the same pattern:
  - background, accent, text, divider, font, and row height come from `Theme`
  - row striping, hover state, disclosure color, scrollbars, indent width, and icon size stay module-local
- Result: theme switching works, but the modules do not yet share the same semantic state palette or density model.

### 5. Canvas geometry also contains magic numbers outside the theme API
- Code canvas geometry:
  - `Viewport` scrollbar constants define width, padding, radius, and thumb minimum outside the shared theme contract
  - `GutterView` hardcodes marker lane width, fold lane width, and line-number padding
- Tree canvas geometry:
  - `TreeViewport` hardcodes scrollbar width/radius, info-toggle size, and margins
  - `TreeContentPass` hardcodes icon offsets, disclosure size caps, line widths, and toggle corner radii
  - `GlyphCache` falls back to `Font.font("System", 13)` independently from the shared theme font scale
- Result: even if CSS tokens are standardized, render-path density will still diverge until later phases move those numbers behind shared metrics.

## Module Notes

### GitHub Module
- Strengths:
  - Best existing theme adapter of the three audited modules.
  - Uses a dedicated mapper (`GitHubToolbarThemeMapper`) and stylesheet helper (`GitHubThemeSupport`).
  - Toolbar root, popup, overflow menu, and dialogs all react to theme changes.
- Inconsistencies:
  - `GitHubToolbarThemeMapper` still invents module-local density with `Math.max(...)` rules for padding, radii, toolbar height, button height, and group gaps.
  - `github-toolbar.css` mixes shared looked-up colors with inline semantic `rgba(...)` accents.
  - Dialogs and popup layout use separate fixed gaps and padding in Java (`8`, `12`, `392`, `520`, `6`) instead of shared metrics.

### Code Module
- Strengths:
  - `CodeEditor.bindThemeProperty(...)` cascades updates into viewport, gutter, search, and go-to-line overlays.
  - Search and go-to-line overlays already use scoped stylesheets and pseudo-classes for validation states.
- Inconsistencies:
  - Search and go-to-line define almost the same overlay language twice.
  - CSS defaults and Java fallbacks duplicate the same palette.
  - `CodeEditorTheme` is color-heavy but metric-light, so spacing/radius/height rules still live in constructors and CSS.
  - Overlay placement (`updateOverlayMargins`) depends on unshared constants rather than theme metrics.

### Tree Module
- Strengths:
  - `TreeView.bindThemeProperty(...)` updates both canvas rendering and the search overlay.
  - Search overlay is compact and already isolated in its own stylesheet.
- Inconsistencies:
  - `TreeViewThemeMapper` only partially derives from the shared `Theme`.
  - Search overlay duplicates code-search patterns but with different spacing and missing states such as disabled/error/pressed.
  - Canvas geometry uses several internal constants that are not connected to the theme API.

## Standards Definition

### Non-Breaking Theme API Direction
Phase 1 should add a shared token layer to the `Theme` API without replacing the existing `Theme` record as the binding source.

Recommended additive API shape:
- `ThemeCssVariables` (new API helper): converts a `Theme` into a canonical set of shared looked-up CSS values.
- `ThemeSpacingScale` or derived helpers on `ThemeDimensions`: exposes the 4px spacing grid and standard radii/control heights from one place.
- Module mappers may keep local records (`GitHubToolbarTheme`, `CodeEditorTheme`, `TreeViewTheme`) during migration, but they should consume the shared token layer instead of redefining semantics independently.

This preserves the current property binding model:
- `ObjectProperty<Theme>` remains the runtime source of truth.
- JavaFX nodes continue to listen to a single theme property.
- CSS variables become a projection of `Theme`, not a second theme system.

### Proposed Shared CSS Variable Tokens
Canonical prefix for shared tokens: `-pf-ui-*`

#### Surface Tokens
- `-pf-ui-surface-canvas`
- `-pf-ui-surface-panel`
- `-pf-ui-surface-panel-subtle`
- `-pf-ui-surface-overlay`
- `-pf-ui-surface-control`
- `-pf-ui-surface-control-hover`
- `-pf-ui-surface-control-pressed`
- `-pf-ui-surface-selected`
- `-pf-ui-surface-selected-inactive`

#### Text Tokens
- `-pf-ui-text-primary`
- `-pf-ui-text-muted`
- `-pf-ui-text-disabled`
- `-pf-ui-text-link`
- `-pf-ui-text-on-accent`

#### Border and Divider Tokens
- `-pf-ui-border-default`
- `-pf-ui-border-subtle`
- `-pf-ui-border-focus`
- `-pf-ui-divider`

#### Accent and Status Tokens
- `-pf-ui-accent`
- `-pf-ui-accent-subtle`
- `-pf-ui-success`
- `-pf-ui-success-subtle`
- `-pf-ui-warning`
- `-pf-ui-warning-subtle`
- `-pf-ui-danger`
- `-pf-ui-danger-subtle`
- `-pf-ui-drop-hint`

#### Effect Tokens
- `-pf-ui-shadow-overlay`

#### Typography Tokens
- `-pf-ui-font-family`
- `-pf-ui-font-size-xs`
- `-pf-ui-font-size-sm`
- `-pf-ui-font-size-md`
- `-pf-ui-font-weight-strong`

#### Metric Tokens
- `-pf-ui-space-1`
- `-pf-ui-space-2`
- `-pf-ui-space-3`
- `-pf-ui-space-4`
- `-pf-ui-space-5`
- `-pf-ui-space-6`
- `-pf-ui-radius-sm`
- `-pf-ui-radius-md`
- `-pf-ui-radius-lg`
- `-pf-ui-radius-pill`
- `-pf-ui-control-height-compact`
- `-pf-ui-control-height-regular`
- `-pf-ui-toolbar-height`

### Proposed Spacing System
Base unit: `4px`

Standard scale:
- `space-1 = 4px`
- `space-2 = 8px`
- `space-3 = 12px`
- `space-4 = 16px`
- `space-5 = 20px`
- `space-6 = 24px`

Standard usage rules:
- Inline icon/text gaps: `space-1` or `space-2`
- Compact row gaps: `space-1`
- Standard control groups: `space-2`
- Overlay and popup padding: `space-2` or `space-3`
- Major toolbar section separation: `space-3` or `space-4`

Standard radius rules:
- `radius-sm = 4px` for chips, icon buttons, and compact actions
- `radius-md = 8px` for standard controls and panels
- `radius-lg = 12px` for popups/dialog containers
- `radius-pill = 999px` for badges and ref pills only

Standard height rules:
- `control-height-compact = 24px` for search fields, compact icon buttons, and inline chips
- `control-height-regular = 28px` for standard toolbar buttons and dialogs
- `toolbar-height = 44px` minimum for toolbars that include grouped metadata and actions

### Standard Visual States
- Default: use surface/control tokens with stable geometry.
- Hover: change background only; do not increase padding, width, or radius.
- Pressed/Active: use `surface-control-pressed` or `accent-subtle`; keep border width stable.
- Focused: use `border-focus`; avoid module-specific focus colors.
- Selected: use `surface-selected` plus contrast-safe text (`text-on-accent` when needed).
- Disabled: keep `opacity: 1.0` and switch to `text-disabled`; do not fade entire controls unpredictably.
- Error/Validation: use `danger` border plus `danger-subtle` background.
- Success/Warning: use semantic status tokens rather than inline `rgba(...)` values.

## Phase 2 and 3 Implications
- GitHub local variables should become aliases over `-pf-ui-*` first, then shrink away.
- Code search and go-to-line should share one overlay token vocabulary and one compact-control metric set.
- Tree search should adopt the same compact overlay tokens as code search instead of keeping a separate search surface language.
- Canvas modules should migrate scrollbar geometry, disclosure sizing, gutter widths, and icon metrics into shared theme-derived metrics after the CSS token layer lands.

## Phase 2 Extraction Decision
- Lead: `@ui-ux-designer`
- Required reviewers: `@feature-dev`, `@core-architect`
- Target shared module: `papiflyfx-docking-api`

Rationale:
- The extracted pieces are lightweight JavaFX primitives plus shared CSS/token helpers.
- `code`, `tree`, and `github` already depend on `papiflyfx-docking-api`, so a new `papiflyfx-docking-ui-common` module would add module churn without isolating a heavier runtime concern.
- This keeps the rollout additive and preserves the existing `ObjectProperty<Theme>` binding flow in each feature module.

### Components Identified For Extraction
- Popups / overlay surfaces:
  - Shared CSS surface and compact-control classes in `org.metalib.papifly.fx.ui` (`ui-common.css`) plus shared token helpers in `UiStyleSupport`.
  - First adopters: `SearchController`, `GoToLineController`, `TreeSearchOverlay`, and `GitRefPopup`.
- Chips:
  - Shared semantic chip label `UiChipLabel` and `UiChipVariant`.
  - First adopters: GitHub secondary chips and the GitHub error chip.
  - Shared chip-toggle styling is also used by code search toggles so compact chips now share one base treatment across modules.
- Pills:
  - Shared `UiPillButton` extracted for rounded trigger pills.
  - First adopters: GitHub repository pill and ref pill (`RefPill` now extends the shared base).
- Status slots:
  - Shared `UiStatusSlot` extracted as the compact inline container for busy / status / error content.
  - First adopter: GitHub toolbar status area.

### Phase 2 Compatibility Notes
- Functional behavior stays in the feature modules; only visual primitives and styling helpers moved into `papiflyfx-docking-api`.
- Theme switching still flows from the module-owned theme property. Shared controls consume projected `-pf-ui-*` variables rather than creating a second theme source.
- Existing module selectors and ids were retained where tests and feature logic depend on them, so the extracted controls remain drop-in replacements during Phase 3 cleanup.

## Audit Inputs
- `papiflyfx-docking-api/src/main/java/org/metalib/papifly/fx/docking/api/Theme.java`
- `papiflyfx-docking-api/src/main/java/org/metalib/papifly/fx/docking/api/ThemeColors.java`
- `papiflyfx-docking-api/src/main/java/org/metalib/papifly/fx/docking/api/ThemeDimensions.java`
- `papiflyfx-docking-github/src/main/java/org/metalib/papifly/fx/github/api/GitHubToolbar.java`
- `papiflyfx-docking-github/src/main/java/org/metalib/papifly/fx/github/ui/theme/GitHubToolbarThemeMapper.java`
- `papiflyfx-docking-github/src/main/java/org/metalib/papifly/fx/github/ui/theme/GitHubThemeSupport.java`
- `papiflyfx-docking-github/src/main/java/org/metalib/papifly/fx/github/ui/popup/GitRefPopup.java`
- `papiflyfx-docking-github/src/main/java/org/metalib/papifly/fx/github/ui/dialog/*.java`
- `papiflyfx-docking-github/src/main/resources/org/metalib/papifly/fx/github/ui/github-toolbar.css`
- `papiflyfx-docking-github/src/main/resources/org/metalib/papifly/fx/github/ui/github-dialog.css`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/theme/CodeEditorTheme.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/theme/CodeEditorThemeMapper.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/search/SearchController.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/GoToLineController.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/gutter/GutterView.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/ScrollbarPass.java`
- `papiflyfx-docking-code/src/main/resources/org/metalib/papifly/fx/code/search/search-overlay.css`
- `papiflyfx-docking-code/src/main/resources/org/metalib/papifly/fx/code/api/go-to-line-overlay.css`
- `papiflyfx-docking-tree/src/main/java/org/metalib/papifly/fx/tree/api/TreeView.java`
- `papiflyfx-docking-tree/src/main/java/org/metalib/papifly/fx/tree/theme/TreeViewTheme.java`
- `papiflyfx-docking-tree/src/main/java/org/metalib/papifly/fx/tree/theme/TreeViewThemeMapper.java`
- `papiflyfx-docking-tree/src/main/java/org/metalib/papifly/fx/tree/search/TreeSearchOverlay.java`
- `papiflyfx-docking-tree/src/main/java/org/metalib/papifly/fx/tree/render/TreeViewport.java`
- `papiflyfx-docking-tree/src/main/java/org/metalib/papifly/fx/tree/render/TreeScrollbarPass.java`
- `papiflyfx-docking-tree/src/main/java/org/metalib/papifly/fx/tree/render/TreeContentPass.java`
- `papiflyfx-docking-tree/src/main/java/org/metalib/papifly/fx/tree/util/GlyphCache.java`
- `papiflyfx-docking-tree/src/main/resources/org/metalib/papifly/fx/tree/search/tree-search-overlay.css`
