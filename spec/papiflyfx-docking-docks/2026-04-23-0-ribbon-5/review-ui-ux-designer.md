# Ribbon 5 Review — UI/UX Designer Perspective

**Priority:** P1 (High)  
**Lead Agent:** `@ui-ux-designer`  
**Required Reviewers:** `@core-architect`, `@qa-engineer`, `@spec-steward`  
**Workflow:** review-only; emit findings into the `Findings` section at the bottom of this file.

## Goal

Audit the ribbon's visual system, adaptive layout behavior, token usage, and interaction-state coverage. Establish whether the ribbon feels coherent with the rest of the shared UI vocabulary (`org.metalib.papifly.fx.ui`, `ui-common.css`) and whether it meets PapiflyFX's standards for legibility, theme parity, and accessibility.

## Scope

### In scope

1. `papiflyfx-docking-docks/src/main/resources/org/metalib/papifly/fx/docks/ribbon/ribbon.css`.
2. `org.metalib.papifly.fx.ui` package in `papiflyfx-docking-api` (shared primitives and `-pf-ui-*` tokens) as they relate to ribbon controls.
3. Adaptive sizing classes referenced by `Ribbon` and `RibbonGroup` (`.pf-ribbon-group-{large,medium,small,collapsed}`, `.pf-ribbon-control-{large,medium,small}`).
4. `RibbonControlFactory` — text/icon arrangement, graphic-text gap, tooltip wiring.
5. `RibbonTabStrip` — tab selection affordance, focus ring, contextual-tab accent.
6. `QuickAccessToolbar` — button size, spacing, hover/active state.
7. `RibbonIconLoader` — icon resolution, fallback glyph policy, theme reaction.
8. Comparison with existing shared components: `UiChip`, `UiChipToggle`, `UiChipLabel`, `UiPillButton`, `UiStatusSlot`, `UiMetrics`, `UiStyleSupport`, `UiCommonStyles`.

### Out of scope

1. API/runtime redesign (`@core-architect`).
2. Feature-module command choices (`@feature-dev`).
3. Test harness color/DPI behavior (`@qa-engineer`).

## Review Questions

### A. Token coverage and visual coherence

1. Does `ribbon.css` exclusively use `-pf-ui-*` tokens from `ui-common.css` for color, spacing, radius, elevation, and focus? List any hard-coded hex/px that should be tokenized.
2. Does the ribbon share metrics with `UiMetrics` (button height, control gap, padding) or redefine them locally? Propose consolidation where drift exists.
3. Are hover/active/focus/disabled states applied uniformly across button, toggle, split-button, and menu controls? Check `RibbonControlFactory` for missing state classes.
4. Does the QAT visually belong to the ribbon family, or does it look imported from another context? Compare against `UiPillButton`/`UiChipToggle`.
5. Contextual tabs — do they have a visible accent (e.g., tinted background or accent underline) distinct from permanent tabs? Is the accent derivable from the active theme or hard-coded?

### B. Typography and clipping

1. The `ribbon-3` plan identified clipping of labels (`Paste`, `Copy`, etc.) and group captions (`Clipboard`). Confirm the fix landed and no descender clipping remains at:
   - default `Ribbon Shell` geometry in SamplesApp,
   - narrow widths during adaptive collapse,
   - collapsed-group popup content.
2. Is line-wrapping on `LARGE` controls deterministic for labels up to N characters (propose N)? What happens on languages with longer words (de-DE, fr-FR)?
3. Are `MEDIUM` and `SMALL` mode text/icon arrangements visually distinct and ergonomically right, or is `MEDIUM` just a smaller `LARGE`?

### C. Adaptive layout behavior

1. `RibbonGroupSizeMode` transitions (`LARGE` → `MEDIUM` → `SMALL` → `COLLAPSED`) — is the threshold policy principled (per-group priority, see `collapseOrder`) or ad-hoc? Is there visible flicker when the Ribbon is resized by small increments?
2. When a group collapses, its popup should relay out controls. Confirm:
   - popup anchors correctly near its button,
   - popup theme matches the active theme,
   - keyboard dismissal works (Esc),
   - focus returns to the collapsed group button after dismissal.
3. If two groups collapse simultaneously, does the rendering order remain deterministic?
4. Does adaptive layout account for the QAT width? If QAT pushes the tab strip, does the strip still reach a sensible minimum before it scrolls?

### D. Accessibility

1. Tab order — can a keyboard user reach every ribbon command via Tab/Shift-Tab, including collapsed popups? Identify missing focus traversal.
2. Focus ring — is it visible on dark theme as well as light theme? Cross-check with `UiStyleSupport` focus token.
3. Tooltips — do all ribbon controls have tooltips, and are the tooltip texts localizable (see coordination with `review-feature-dev.md` G.1)?
4. Color contrast — verify AA-level contrast for:
   - tab label on tab background,
   - control label on control background,
   - QAT button label on QAT background,
   - disabled-state text.
5. Screen reader affordances — does JavaFX provide sensible `accessibleText` via `PapiflyCommand.label` and `tooltip`? Confirm there is no double-announcement when the node graphic includes the label.

### E. Theme behavior

1. Does `RibbonThemeSupport` propagate theme changes to all ribbon nodes without flicker or stale colors?
2. Are icon colors theme-reactive? If icons are SVG, is there a tinting pipeline (see `ribbon-2/adr-0001-svg-icons.md`)? Confirm the ADR was honored.
3. Does the collapsed-group popup inherit the theme of the parent ribbon at the moment it opens, or can it open with a stale theme if the user toggled theme while the popup was ready to show?

### F. Interaction ergonomics

1. Is the tab click target comfortable (>=28px)? Is the caret on split buttons wide enough to click without misfires?
2. Do menus and split-button menus follow consistent alignment (below the trigger, left-aligned to trigger), matching the rest of the app?
3. Is there a visible affordance distinguishing a disabled command from an enabled one, beyond color (label weight, cursor)?
4. Does "minimize ribbon" hide the tab body but keep the tab strip accessible, and does a tab click re-expand temporarily or permanently? Both are valid; whichever is chosen must match the session-restored state.

### G. Comparison to other app chrome

1. Does the ribbon sit well with `MinimizedBar`? Both are horizontal UI chrome; confirm they do not collide visually.
2. Does the ribbon respect the shared color language used by `UiChip`/`UiChipLabel` elsewhere in the app (e.g., status slots)?
3. If a user floats a dock window, does the floating window also show a ribbon, or only the main window? Confirm the intended behavior is consistent (coordinate with `review-core-architect.md` C.1).

## Review Procedure

1. Launch the SamplesApp locally if possible: `./mvnw javafx:run -pl papiflyfx-docking-samples`. Otherwise inspect screenshots from `ribbon-3`.
2. Read `ribbon.css` and the shared `ui-common.css` side by side.
3. Walk through `RibbonControlFactory` to map each spec record to its node structure and state classes.
4. For each review question, record observations and attach small before/after mockups only if the finding is a proposed redesign.

## Deliverable

Populate the `Findings` section below using the common template:

```md
### F-<NN>: <short title>
**Severity:** P0|P1|P2|P3  
**Area:** <Tokens | Typography | Adaptive layout | Accessibility | Theme | Interaction | Comparison>  
**Evidence:** <file:line citations or screenshot reference>  
**Risk:** <user-visible impact>  
**Suggested follow-up:** <lead role, rough cost S/M/L>
```

## Validation

No automated validation is required. For regressions identified by this review, note the corresponding or missing `*FxTest`; if none exists, flag it in `review-qa-engineer.md` rather than here.

## Findings

_Not yet started._

## Handoff Snapshot

Lead Agent: `@ui-ux-designer`  
Task Scope: visual/UX review of the ribbon shell, tokens, adaptive layout, theme, accessibility  
Impacted Modules: `spec/**` only  
Files Changed: this file (on completion)  
Key Invariants:

- no production code or CSS changes
- findings must cite files and lines, and reference shared UI primitives where applicable
- accessibility findings take priority

Validation Performed: visual inspection via SamplesApp (or ribbon-3 snapshots)  
Open Risks / Follow-ups: recorded as numbered findings  
Required Reviewer: `@core-architect`, `@qa-engineer`, `@spec-steward`
