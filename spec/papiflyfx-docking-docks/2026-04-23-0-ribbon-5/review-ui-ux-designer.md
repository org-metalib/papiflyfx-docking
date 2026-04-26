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

### F-01: Ribbon sizing and spacing still drift from the shared UI token model
**Severity:** P2  
**Area:** Tokens  
**Evidence:** `papiflyfx-docking-docks/src/main/resources/org/metalib/papifly/fx/docks/ribbon/ribbon.css:12`, `papiflyfx-docking-docks/src/main/resources/org/metalib/papifly/fx/docks/ribbon/ribbon.css:37`, `papiflyfx-docking-docks/src/main/resources/org/metalib/papifly/fx/docks/ribbon/ribbon.css:90`, `papiflyfx-docking-docks/src/main/resources/org/metalib/papifly/fx/docks/ribbon/ribbon.css:97`, `papiflyfx-docking-docks/src/main/resources/org/metalib/papifly/fx/docks/ribbon/ribbon.css:128`, `papiflyfx-docking-docks/src/main/resources/org/metalib/papifly/fx/docks/ribbon/ribbon.css:209`, `papiflyfx-docking-docks/src/main/resources/org/metalib/papifly/fx/docks/ribbon/ribbon.css:217`, `papiflyfx-docking-docks/src/main/resources/org/metalib/papifly/fx/docks/ribbon/ribbon.css:228`, `papiflyfx-docking-docks/src/main/resources/org/metalib/papifly/fx/docks/ribbon/ribbon.css:238`, `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonControlFactory.java:35`, `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonControlFactory.java:42`, `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonGroup.java:38`, `papiflyfx-docking-api/src/main/java/org/metalib/papifly/fx/ui/UiMetrics.java:8`, `papiflyfx-docking-api/src/main/java/org/metalib/papifly/fx/ui/UiMetrics.java:20`, `papiflyfx-docking-api/src/main/java/org/metalib/papifly/fx/ui/UiStyleSupport.java:36`  
**Risk:** The ribbon uses shared color tokens for most paints, but its key dimensions, radii, gaps, control widths, and adaptive estimates are duplicated between CSS and Java instead of flowing from `UiMetrics` / `-pf-ui-*` variables. Future shared-density changes can make `ui-common.css` controls, `UiChip`/`UiPillButton`, and ribbon controls diverge visually, and CSS sizing can drift from `RibbonGroup#estimateWidth(...)` collapse math.  
**Suggested follow-up:** `@ui-ux-designer` lead with `@core-architect` review, M. Add ribbon-specific metric constants only where the shared model is missing, emit them as CSS variables, and make `RibbonControlFactory` / `RibbonGroup` consume the same constants used by `ribbon.css`.

### F-02: MEDIUM mode is a compressed LARGE layout rather than a distinct adaptive presentation
**Severity:** P2  
**Area:** Typography  
**Evidence:** `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonControlFactory.java:186`, `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonControlFactory.java:190`, `papiflyfx-docking-docks/src/main/resources/org/metalib/papifly/fx/docks/ribbon/ribbon.css:217`, `papiflyfx-docking-docks/src/main/resources/org/metalib/papifly/fx/docks/ribbon/ribbon.css:224`, `papiflyfx-docking-docks/src/test/java/org/metalib/papifly/fx/docks/ribbon/RibbonAdaptiveLayoutFxTest.java:173`, `spec/papiflyfx-docking-docks/2026-04-23-0-ribbon-3/progress.md:37`  
**Risk:** The ribbon-3 fix protects sample labels such as `Paste`, `Copy`, `Duplicate`, `Pin Preview`, `Clipboard`, and `Layout` in LARGE and forced MEDIUM modes, but MEDIUM still uses the same top-stacked icon/text pattern as LARGE with a narrower `72px` budget. There is no deterministic label-length budget or long-word strategy for de-DE/fr-FR style strings, so localized command labels can wrap unpredictably or force uncomfortable vertical density before the group reaches SMALL/COLLAPSED.  
**Suggested follow-up:** `@ui-ux-designer` lead with `@feature-dev` review, M. Define label budgets for LARGE and MEDIUM, make MEDIUM a horizontal icon+text or two-row compact pattern, and add a visual/test fixture with long localized labels plus collapsed-popup content.

### F-03: Keyboard focus is invisible on several ribbon entry points
**Severity:** P1  
**Area:** Accessibility  
**Evidence:** `papiflyfx-docking-docks/src/main/resources/org/metalib/papifly/fx/docks/ribbon/ribbon.css:194`, `papiflyfx-docking-docks/src/main/resources/org/metalib/papifly/fx/docks/ribbon/ribbon.css:198`, `papiflyfx-docking-docks/src/main/resources/org/metalib/papifly/fx/docks/ribbon/ribbon.css:61`, `papiflyfx-docking-docks/src/main/resources/org/metalib/papifly/fx/docks/ribbon/ribbon.css:139`, `papiflyfx-docking-docks/src/main/resources/org/metalib/papifly/fx/docks/ribbon/ribbon.css:238`, `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonTabStrip.java:130`, `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/QuickAccessToolbar.java:52`, `papiflyfx-docking-api/src/main/resources/org/metalib/papifly/fx/ui/ui-common.css:145`, `papiflyfx-docking-api/src/main/resources/org/metalib/papifly/fx/ui/ui-common.css:198`  
**Risk:** Command, toggle, split-button, and menu controls get a `-pf-ui-border-focus` border, but tabs, QAT buttons, group launchers, and collapsed-group buttons do not define a focused state. A keyboard user can tab into visible ribbon chrome without a reliable focus indicator, especially in dark theme where hover/selection colors are close to the surrounding header.  
**Suggested follow-up:** `@ui-ux-designer` lead, S. Add focused selectors for `.pf-ribbon-tab`, `.pf-ribbon-qat-button`, `.pf-ribbon-group-launcher`, and `.pf-ribbon-group-collapsed-button`, reusing the shared focus token and preserving selected/contextual accents.

### F-04: Collapsed group popups do not restore focus after dismissal
**Severity:** P1  
**Area:** Accessibility  
**Evidence:** `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonGroup.java:273`, `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonGroup.java:284`, `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonGroup.java:301`, `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonGroup.java:304`, `papiflyfx-docking-docks/src/test/java/org/metalib/papifly/fx/docks/ribbon/RibbonAdaptiveLayoutFxTest.java:86`, `papiflyfx-docking-docks/src/test/java/org/metalib/papifly/fx/docks/ribbon/RibbonAdaptiveLayoutFxTest.java:96`  
**Risk:** The popup correctly enables `autoHide` and `hideOnEscape`, and it updates theme before showing, but there is no `onHidden`/`onAutoHide` path that calls `collapsedButton.requestFocus()`. After Esc, outside click, or command activation inside the popup, keyboard focus can fall back to the scene root or another control, making repeated keyboard navigation through collapsed groups unpredictable. Existing coverage opens the popup and clicks a command; it does not assert Esc dismissal or focus return.  
**Suggested follow-up:** `@core-architect` lead with `@ui-ux-designer` and `@qa-engineer` review, S. Restore focus to the collapsed trigger on popup hide and add a TestFX regression for Esc and command-click dismissal.

### F-05: Icon-only ribbon commands lack an explicit accessible-name contract
**Severity:** P1  
**Area:** Accessibility  
**Evidence:** `papiflyfx-docking-api/src/main/java/org/metalib/papifly/fx/api/ribbon/PapiflyCommand.java:22`, `papiflyfx-docking-api/src/main/java/org/metalib/papifly/fx/api/ribbon/PapiflyCommand.java:59`, `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonControlFactory.java:66`, `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonControlFactory.java:192`, `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonControlFactory.java:195`, `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonControlFactory.java:247`, `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonControlFactory.java:249`, `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonControlFactory.java:256`  
**Risk:** `PapiflyCommand` always has a label/tooltip, but the factory only assigns `accessibleText` to collapsed group buttons. SMALL controls and QAT commands become graphic-only when icons are present, so their visible `text` can be empty and their accessible name depends on JavaFX fallback behavior rather than an explicit ribbon rule. That creates screen-reader ambiguity and makes it hard to guarantee there is no double announcement for visible text modes.  
**Suggested follow-up:** `@ui-ux-designer` lead with `@core-architect` review, S. Set `accessibleText` from command/menu/group labels whenever visible text is suppressed, and document whether tooltips are descriptive help or the primary accessible label.

### F-06: Disabled command state is color-only and does not mute icons
**Severity:** P2  
**Area:** Interaction  
**Evidence:** `papiflyfx-docking-docks/src/main/resources/org/metalib/papifly/fx/docks/ribbon/ribbon.css:201`, `papiflyfx-docking-docks/src/main/resources/org/metalib/papifly/fx/docks/ribbon/ribbon.css:205`, `papiflyfx-docking-docks/src/main/resources/org/metalib/papifly/fx/docks/ribbon/ribbon.css:206`, `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonIconLoader.java:99`, `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonIconLoader.java:100`, `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonIconLoader.java:194`, `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonIconLoader.java:195`, `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonControlFactory.java:167`  
**Risk:** Disabled controls set only text fill to `-pf-ui-text-disabled` and keep opacity at `1.0`. SVG and octicon paths are styled inline with `-pf-ui-text-primary`, and raster icons cannot be tinted, so disabled icon-heavy controls can still look enabled aside from label color. This fails the review question's requirement for an affordance beyond color and weakens disabled-state contrast parity.  
**Suggested follow-up:** `@ui-ux-designer` lead, S. Add disabled-state selectors for icon wrappers/paths, consider a subtle disabled background or border treatment, and decide whether raster icons require opacity reduction when their owning control is disabled.

### F-07: Contextual tab accent is too easy to lose in selected and focus states
**Severity:** P2  
**Area:** Theme  
**Evidence:** `papiflyfx-docking-api/src/main/java/org/metalib/papifly/fx/api/ribbon/RibbonTabSpec.java:13`, `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonTabStrip.java:133`, `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonTabStrip.java:134`, `papiflyfx-docking-docks/src/main/resources/org/metalib/papifly/fx/docks/ribbon/ribbon.css:47`, `papiflyfx-docking-docks/src/main/resources/org/metalib/papifly/fx/docks/ribbon/ribbon.css:52`, `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonThemeSupport.java:44`, `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonThemeSupport.java:53`  
**Risk:** Contextual tabs receive a theme-derived accent background, but selected tabs use the normal selected background and underline. There is no `.pf-ribbon-tab-contextual:selected` or contextual focus treatment, so a contextual tab can become visually indistinguishable from permanent tabs exactly when the user is interacting with it.  
**Suggested follow-up:** `@ui-ux-designer` lead with `@feature-dev` review, S. Add contextual selected/focused styles using the theme accent, and verify light/dark contrast for tab labels on both contextual and selected backgrounds.

### F-08: The header has no overflow strategy when QAT grows
**Severity:** P2  
**Area:** Adaptive layout  
**Evidence:** `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/Ribbon.java:46`, `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/Ribbon.java:107`, `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/Ribbon.java:108`, `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/Ribbon.java:111`, `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/QuickAccessToolbar.java:52`, `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonTabStrip.java:23`, `papiflyfx-docking-docks/src/main/resources/org/metalib/papifly/fx/docks/ribbon/ribbon.css:27`, `papiflyfx-docking-docks/src/main/resources/org/metalib/papifly/fx/docks/ribbon/ribbon.css:56`  
**Risk:** Adaptive collapse is applied to the group scroller, but the header is a `BorderPane` with an unbounded QAT on the left, a plain `HBox` tab strip in the center, and no tab scrolling/overflow/minimum policy. A large restored QAT can steal width from the tab strip before tabs have a sensible overflow behavior, which makes selected/contextual tabs harder to discover even though the command groups below still adapt.  
**Suggested follow-up:** `@core-architect` lead with `@ui-ux-designer` review, M. Define a QAT maximum/overflow menu or a scrollable/clipped tab strip minimum, then add a fixture with many pinned QAT commands and multiple contextual tabs.

## Handoff Snapshot

Lead Agent: `@ui-ux-designer`  
Task Scope: visual/UX review of the ribbon shell, tokens, adaptive layout, theme, accessibility  
Impacted Modules: `spec/**` only  
Files Changed: this file (on completion)  
Key Invariants:

- no production code or CSS changes
- findings must cite files and lines, and reference shared UI primitives where applicable
- accessibility findings take priority

Validation Performed: source and test inspection; SamplesApp was not launched interactively in this environment  
Open Risks / Follow-ups: recorded as numbered findings  
Required Reviewer: `@core-architect`, `@qa-engineer`, `@spec-steward`
