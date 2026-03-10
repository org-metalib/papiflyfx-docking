# GitHub Toolbar Color Theme Harmonization Plan

## 1. Goal

Bring `papiflyfx-docking-github` onto the same visual level as the stronger in-repo components, especially:

- `papiflyfx-docking-code` search / go-to overlays
- `papiflyfx-docking-hugo` toolbar grouping and action hierarchy
- the shared docking `Theme`

This plan focuses on visual design, theming architecture, and UX polish for the GitHub toolbar first, then the dialogs it opens so the experience stays coherent end-to-end.

Assumption: the review note's `huge` reference means `HugoPreviewToolbar`.

## 2. Reviewed Artifacts

- `spec/papiflyfx-docking-github/review0-color-theme/README.md`
- `papiflyfx-docking-github/src/main/java/org/metalib/papifly/fx/github/api/GitHubToolbar.java`
- `papiflyfx-docking-github/src/main/java/org/metalib/papifly/fx/github/ui/GitHubToolbarViewModel.java`
- `papiflyfx-docking-github/src/main/java/org/metalib/papifly/fx/github/ui/dialog/CommitDialog.java`
- `papiflyfx-docking-github/src/main/java/org/metalib/papifly/fx/github/ui/dialog/TokenDialog.java`
- `papiflyfx-docking-api/src/main/java/org/metalib/papifly/fx/docking/api/Theme.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/search/SearchController.java`
- `papiflyfx-docking-code/src/main/resources/org/metalib/papifly/fx/code/search/search-overlay.css`
- `papiflyfx-docking-code/src/main/resources/org/metalib/papifly/fx/code/api/go-to-line-overlay.css`
- `papiflyfx-docking-hugo/src/main/java/org/metalib/papifly/fx/hugo/api/HugoPreviewToolbar.java`
- `papiflyfx-docking-github/src/test/java/org/metalib/papifly/fx/github/ui/GitHubToolbarFxTest.java`
- `papiflyfx-docking-samples/src/main/java/org/metalib/papifly/fx/samples/github/GitHubToolbarSample.java`

## 3. Current Baseline

### 3.1 What the current toolbar does well

- The toolbar already binds to a `Theme` property.
- The data and command model is cleanly separated in `GitHubToolbarViewModel`.
- IDs already exist for most controls, so targeted FX tests and CSS hooks are practical.

### 3.2 Current visual problems

1. `GitHubToolbar` is built as one long row of stock JavaFX controls and literal `"|"` separators.
   - See `GitHubToolbar.java:72-173` and `GitHubToolbar.java:303-307`.
2. `applyTheme(...)` only styles the container background, border, status text, dirty dot, and error text.
   - See `GitHubToolbar.java:309-321`.
3. Buttons, combo box, hyperlink, and progress indicator are still mostly Modena defaults, so they do not read as part of the docking chrome.
4. Hard-coded values dominate spacing, padding, widths, and colors.
   - Examples: `setSpacing(8)`, `setPadding(new Insets(6, 10, 6, 10))`, fixed combo width `180`, hard-coded error color `#d9534f`.
5. Shared `Theme` tokens such as `headerHeight`, `cornerRadius`, `buttonSpacing`, `headerFont`, `contentFont`, `buttonHoverBackground`, and `buttonPressedBackground` are not used.
   - See `Theme.java:35-118`.
6. The toolbar exposes very little of the repository state visually.
   - `RepoStatus` already has `aheadCount`, `behindCount`, and per-file dirty buckets, but the view model reduces this mostly to `dirty`, branch, and plain text status.
   - See `GitHubToolbarViewModel.java:43-56` and `GitHubToolbarViewModel.java:94-147`.
7. Remote-only mode disables several local actions but keeps the whole dead control set visible.
   - This is functionally correct but visually noisy.
8. The dialogs opened from the toolbar are also stock JavaFX dialog panes, so even a redesigned toolbar would currently drop the user into an inconsistent visual language.
   - See `CommitDialog.java:13-37` and `TokenDialog.java:13-36`.
9. Existing tests verify behavior and enable/disable rules, but they do not protect the visual contract.
   - See `GitHubToolbarFxTest.java:45-95`.

### 3.3 Stronger reference patterns already present in the repo

1. The code search overlay uses a good pattern:
   - static CSS classes in a stylesheet
   - runtime CSS variables set from theme data
   - programmatic color application only where JavaFX CSS needs help
   - See `SearchController.java:552-603` and `search-overlay.css`.
2. The Hugo toolbar uses better composition:
   - grouped surfaces instead of pipe separators
   - clear action hierarchy
   - shaped controls with intent
   - See `HugoPreviewToolbar.java:20-330`.

## 4. Design Direction

The GitHub toolbar should feel like application chrome, not a default form row.

Recommended target characteristics:

- Theme-derived in both dark and light modes
- Compact grouped surfaces similar to a professional IDE toolbar
- Rounded controls and badges that visually relate to the code search overlays
- Clear action weighting instead of every button looking equivalent
- Repository state shown as small chips / badges rather than plain text only
- No literal text separators
- No hard-coded dark palette copied from Hugo

Important design rule:

- Borrow Hugo's grouping and action hierarchy, not its exact colors.
- Borrow the code search overlay's theme-variable architecture, not its exact layout.

## 5. Proposed Technical Approach

### 5.1 Add a GitHub-specific derived theme layer

Create a local theme model for the GitHub UI instead of expanding `Theme` immediately.

Recommended new types:

- `org.metalib.papifly.fx.github.ui.theme.GitHubToolbarTheme`
- `org.metalib.papifly.fx.github.ui.theme.GitHubToolbarThemeMapper`

Why this is the right first step:

- `Theme` is intentionally small and shared across modules.
- The GitHub toolbar needs more semantic tokens than `Theme` currently exposes.
- `papiflyfx-docking-code` already proves the mapper pattern works well via `CodeEditorThemeMapper`.

Recommended derived tokens:

- `toolbarBackground`
- `toolbarBorder`
- `groupBackground`
- `groupBorder`
- `controlBackground`
- `controlBackgroundHover`
- `controlBackgroundPressed`
- `controlBorder`
- `focusBorder`
- `textPrimary`
- `textMuted`
- `textDisabled`
- `linkText`
- `accent`
- `success`
- `warning`
- `danger`
- `badgeBackground`
- `badgeBorder`
- `statusBackground`
- `errorBackground`
- `busyTrack`
- `busyIndicator`
- `shadow`
- `cornerRadius`
- `compactRadius`
- `toolbarHeight`
- `buttonHeight`
- `contentPadding`
- `groupGap`

Mapping rules:

- Base surface should come from `Theme.headerBackground()`.
- Accent should flow from `Theme.accentColor()`.
- Hover and pressed surfaces should derive from `Theme.buttonHoverBackground()` and `Theme.buttonPressedBackground()`.
- Text and borders should derive from `Theme.textColor()`, `Theme.textColorActive()`, and `Theme.borderColor()`.
- Success / warning / danger should be derived locally from light-or-dark mode, not added to `Theme` yet.

Recommendation:

- Keep `Theme` unchanged in this review.
- Revisit shared semantic status colors only if another module needs the same tokens after this work.

### 5.2 Move the toolbar to stylesheet-driven rendering

Add:

- `papiflyfx-docking-github/src/main/resources/org/metalib/papifly/fx/github/ui/github-toolbar.css`
- optionally `papiflyfx-docking-github/src/main/resources/org/metalib/papifly/fx/github/ui/github-dialog.css`

Implementation pattern should match the code search overlay:

1. Load a stylesheet once.
2. Apply stable style classes to nodes.
3. Set CSS custom properties on the toolbar root from the mapped theme.
4. Use programmatic color assignment only for cases JavaFX CSS does not handle well, such as SVG icon fills or progress graphics.

This should replace the current narrow `applyTheme(...)` implementation.

Recommended root style variables:

- `-pf-github-toolbar-bg`
- `-pf-github-toolbar-border`
- `-pf-github-group-bg`
- `-pf-github-group-border`
- `-pf-github-control-bg`
- `-pf-github-control-hover-bg`
- `-pf-github-control-pressed-bg`
- `-pf-github-control-border`
- `-pf-github-focus-border`
- `-pf-github-text`
- `-pf-github-muted-text`
- `-pf-github-disabled-text`
- `-pf-github-link`
- `-pf-github-accent`
- `-pf-github-success`
- `-pf-github-warning`
- `-pf-github-danger`
- `-pf-github-badge-bg`
- `-pf-github-badge-border`
- `-pf-github-error-bg`
- `-pf-github-shadow`

## 6. Toolbar UX Redesign

### 6.1 Replace the flat row with grouped surfaces

Recommended visual grouping:

1. Repository group
   - repo icon / link
   - current branch badge
   - dirty badge
   - mode badge: `Local` or `Remote only`
2. Branch group
   - branch combo
   - `Checkout`
   - `New Branch`
3. Change group
   - `Commit`
   - `Rollback`
4. Remote group
   - `Push`
   - `Create PR`
   - `Token`
5. Status group
   - busy spinner
   - ahead / behind chips
   - status text
   - error badge when needed

This directly removes the current dependence on `|` separators and gives the toolbar the same intentional structure that the Hugo toolbar already has.

### 6.2 Action hierarchy

Do not render every action with the same visual weight.

Recommended weights:

- Neutral buttons: `Checkout`, `New Branch`, `Token`
- Primary-accent button: `Push`
- Accent-outline button: `Create PR`
- Standard commit action: `Commit`
- Danger-outline action: `Rollback`

If desired later, `Commit` can become the primary-accent action only when the repo is dirty, but that should be treated as an optional refinement, not part of the first implementation pass.

### 6.3 Replace the dirty dot with richer chips

Current state:

- dirty state is only a single colored dot

Recommended replacement:

- `Dirty` badge when there are local changes
- optional compact file-count badge if useful
- `Ahead N` and `Behind N` badges from `RepoStatus`
- `Default branch` badge when on default branch
- `Detached HEAD` badge when applicable

This uses data the module already has instead of relying on color alone.

### 6.4 Remote-only mode should be visually deliberate

Recommended behavior:

- Hide local-only action groups when there is no local clone
- Show a compact `Remote only` badge in the repository group
- Keep `Create PR` and `Token` available

Reason:

- disabled dead controls make the toolbar look broken
- a reduced layout will read as intentional and cleaner

If product requirements insist on preserving discoverability, keep disabled controls only in a fallback variant, but the primary recommendation is to collapse them.

### 6.5 Improve branch and repo presentation

Recommended UI details:

- Give the repo link a contained surface instead of bare hyperlink styling
- Add a simple SVG branch icon next to the combo box or branch badge
- Use `OverrunStyle.LEADING_ELLIPSIS` or constrained width rules on repo text where needed
- Prefer branch chip + combo surface treatment over a plain default combo box sitting directly on the toolbar background

## 7. View Model Changes Needed For The Visual Design

The redesign needs slightly richer state from `GitHubToolbarViewModel`.

Recommended additions:

- `aheadCountProperty`
- `behindCountProperty`
- `detachedHeadProperty`
- `defaultBranchActiveProperty`
- `dirtyCountProperty`
- `authenticatedProperty` already exists and should be surfaced visually
- optional `remoteOnlyProperty`

Notes:

- Do not move presentation logic into `GitRepository`.
- Keep the view model as the place where raw repo state becomes UI state.
- Continue keeping slow work off the FX thread.

Suggested behavior changes tied to the design:

- differentiate warning text from error text
- expose a concise status summary string for the trailing status region
- prefer semantic chips over a long trailing sentence when state is available structurally

## 8. Dialog Harmonization

The toolbar redesign will feel incomplete if the dialogs stay pure Modena.

Recommended scope for the same review:

- `CommitDialog`
- `NewBranchDialog`
- `RollbackDialog`
- `PullRequestDialog`
- `TokenDialog`
- `DirtyCheckoutAlert`

Recommended approach:

- Reuse the same `GitHubToolbarThemeMapper`
- Add a shared GitHub dialog stylesheet
- Style `DialogPane`, buttons, labels, text fields, combo boxes, and warning text
- Keep dialog layout simple, but use the same surface, border, and focus treatment as the toolbar

Recommended priority:

- Phase 1 must include toolbar restyling
- Phase 2 should include dialog alignment

## 9. Concrete Implementation Phases

## Phase A - Theme foundation

- [ ] Add `GitHubToolbarTheme` record.
- [ ] Add `GitHubToolbarThemeMapper`.
- [ ] Add light/dark detection logic based on background brightness.
- [ ] Add mapper unit tests similar to `CodeEditorThemeMapperTest`.
- [ ] Keep `Theme` unchanged for this pass.

## Phase B - CSS infrastructure

- [ ] Add `github-toolbar.css`.
- [ ] Load the stylesheet from `GitHubToolbar`.
- [ ] Replace the existing `applyTheme(...)` node-by-node styling with CSS variable assignment on the toolbar root.
- [ ] Add stable style classes and ids for groups, badges, action types, and status area.
- [ ] Add simple SVG icon support if needed, without new runtime dependencies.

## Phase C - Toolbar layout refactor

- [ ] Replace literal separators with grouped containers.
- [ ] Split the toolbar into repository, branch, changes, remote, and status groups.
- [ ] Convert the repo link into a contained surface.
- [ ] Introduce badges / chips for dirty, remote-only, default-branch, ahead, and behind states.
- [ ] Refine button hierarchy and spacing using theme-derived radii and heights.
- [ ] Make remote-only mode collapse local groups instead of just disabling them.

## Phase D - View model enrichment

- [ ] Preserve current behavior but expose richer read-only properties for UI state.
- [ ] Surface ahead / behind / detached / default-branch state.
- [ ] Compute a compact dirty count.
- [ ] Keep command enablement logic unchanged unless required by the visual redesign.

## Phase E - Dialog alignment

- [ ] Add `github-dialog.css` or a shared GitHub UI stylesheet.
- [ ] Theme the GitHub dialogs with the same derived palette.
- [ ] Style destructive and primary dialog actions consistently with the toolbar.
- [ ] Ensure focus rings and disabled states still read clearly in both dark and light themes.

## Phase F - Tests and validation

- [ ] Add `GitHubToolbarThemeMapperTest`.
- [ ] Add `GitHubToolbarThemeIntegrationTest` modeled after `CodeEditorThemeIntegrationTest`.
- [ ] Verify dark theme variables are applied after binding.
- [ ] Verify switching to `Theme.light()` changes the toolbar root style string.
- [ ] Verify remote-only mode collapses the local groups if that recommendation is accepted.
- [ ] Verify badges appear for dirty / ahead / behind / error states.
- [ ] Verify dialogs receive the same theme treatment.
- [ ] Update `GitHubToolbarFxTest` to assert style classes or high-level structure, not exact pixel colors.

## Phase G - Sample and manual review

- [ ] Update `GitHubToolbarSample` to showcase both remote-only and local-clone states if practical.
- [ ] Manually compare the toolbar against:
  - code search overlay in dark and light themes
  - go-to-line overlay in dark and light themes
  - Hugo toolbar grouping and action weight
- [ ] Capture screenshots for the review folder if the team wants a visual record.

## 10. Acceptance Criteria

The review should be considered complete only when all of the following are true:

1. The toolbar no longer depends on stock Modena visuals for its primary controls.
2. The toolbar updates correctly for both `Theme.dark()` and `Theme.light()`.
3. The root, groups, buttons, combo box, hyperlink surface, badges, and status area all derive from the same mapped theme.
4. The toolbar uses grouped surfaces instead of literal pipe separators.
5. Dirty, remote-only, ahead, behind, error, and default-branch states are visible without relying on color alone.
6. Remote-only mode reads as intentional rather than disabled / broken.
7. GitHub dialogs no longer feel visually disconnected from the toolbar.
8. Tests protect theme mapping and theme switching behavior.

## 11. Risks And How To Contain Them

1. JavaFX `ComboBox` skin styling can be brittle across platforms.
   - Prefer class-based styling of the root, display area, and arrow button.
   - Test structure and variable application instead of exact skin internals.
2. Expanding shared `Theme` too early could create unnecessary API churn.
   - Keep GitHub-specific semantic colors local for now.
3. Hugo uses a hard-coded palette that is visually strong but not theme-derived.
   - Reuse layout ideas, not literal Hugo colors.
4. It is easy to over-design a toolbar and reduce clarity.
   - Keep labels explicit.
   - Use icons only as support, not as the sole meaning carrier.

## 12. Recommended Implementation Order

1. Theme mapper and CSS variables
2. Toolbar structure and grouped surfaces
3. Semantic badges and remote-only layout cleanup
4. Dialog theming
5. Theme integration tests
6. Sample update and manual visual comparison

## 13. Recommendation Summary

The right fix is not "pick better colors" inside `GitHubToolbar.applyTheme(...)`.

The right fix is:

- create a GitHub-specific theme mapper
- move the toolbar to stylesheet-driven theming
- redesign the toolbar into grouped surfaces with semantic chips
- expose richer repo state visually
- align the dialogs with the same palette
- add theme-focused tests so the polish does not regress
