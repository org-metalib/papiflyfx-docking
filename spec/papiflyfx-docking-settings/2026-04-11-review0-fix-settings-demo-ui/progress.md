# Progress — Fix Settings Demo UI Regression

**Priority:** P1
**Lead Agent:** @ops-engineer
**Status:** Dark-Mode Follow-up Complete

## Phase Tracking

### Phase 1: SettingsPanel Token Injection
- **Status:** Complete
- **Target files:** `SettingsPanel.java`
- **Summary:** Added `applyThemeTokens(Theme)` and `buildPalette(Theme)` methods. The constructor now calls `applyThemeTokens()` after loading stylesheets, and the `themeListener` triggers `applyThemeTokens(newTheme)` on every theme change. The palette is built from `UiCommonThemeSupport` helper methods (same pattern as `TreeSearchOverlay` and `GitHubToolbar`).

### Phase 2: SettingsToolbar and SettingsSearchBar Alignment
- **Status:** Complete
- **Target files:** `SettingsToolbar.java`, `SettingsSearchBar.java`
- **Summary:** Replaced ad-hoc `Button` instances with `UiPillButton`. Wrapped dirty/status labels in `UiStatusSlot`. Added `pf-ui-compact-field` style class to `SettingsSearchBar` search field. Action buttons in `setActions()` also use `UiPillButton`.

### Phase 3: SamplesApp Theme-Awareness
- **Status:** Complete
- **Target files:** `SamplesApp.java`
- **Summary:** Removed static `NAVIGATION_THEME` constant (19 hardcoded dark colors). Added `applyTheme(Theme)` method that derives all colors from `UiCommonThemeSupport` and applies them to top bar, content area, placeholder label, buttons, and navigation tree. Navigation tree theme now uses `TreeViewThemeMapper.map(theme)`. Category cell renderer uses `context.theme().background()` and `context.theme().connectingLineColor()` instead of hardcoded hex values. Theme change listener updates all surfaces on toggle.

## Validation Log

| Check | Result | Date |
|-------|--------|------|
| Compile (settings + samples) | PASS — 14/14 modules | 2026-04-12 |
| Headless tests (settings) | PASS — 17/17 | 2026-04-12 |
| Headless tests (samples) | PASS — 12/12 | 2026-04-12 |
| Full build | Not run (media fork issue pre-existing) | — |
| Manual visual verification | Pending reviewer | — |
| Session restore round-trip | Pending reviewer | — |

## Notes

- The `papiflyfx-docking-media` module has a pre-existing Surefire fork startup error unrelated to this task. Running tests on the target modules individually confirms no regressions.
- No new `-pf-ui-*` tokens were introduced; only the existing vocabulary is consumed.
- Settings runtime, persistence, and session restore contracts are untouched.

## @ui-ux-designer Review Follow-up

**Date:** 2026-04-12

### Finding 1 (Medium) — Accent button text contrast: FIXED
- In `SamplesApp.applyTheme(...)`, replaced `textPrimaryCss` with a contrast-on-accent color computed via `UiCommonThemeSupport.isDark(accent) ? "white" : "black"` for `loginDemoButton` and `themeToggle`.
- This ensures WCAG AA contrast (white text on dark accent, black text on light accent).

### Finding 2 (Low) — Toolbar buttons lack hover/focus states: ACCEPTED
- Accepted as a known demo-app limitation. Inline `setStyle(...)` overrides CSS pseudo-class rules. This is not a regression — the prior hardcoded buttons had the same behavior.

### Finding 3 (Low) — Fully-qualified `Color.BLACK` in `buildPalette`: FIXED
- Added `import javafx.scene.paint.Color;` to `SettingsPanel.java` and replaced `javafx.scene.paint.Color.BLACK` with `Color.BLACK`.

### Validation

| Check | Result | Date |
|-------|--------|------|
| Compile (settings + samples) | PASS | 2026-04-12 |
| Headless tests (settings) | PASS — 17/17 | 2026-04-12 |
| Headless tests (samples) | PASS | 2026-04-12 |

## Dark-Mode Follow-up

**Date:** 2026-04-12

### Finding 1 (Medium) — Inactive settings-group readability in dark mode: FIXED
- `SettingsCategoryList` now tags the rendered row and visible category `Label` with explicit settings style classes instead of relying on `ListCell` text styling.
- `settings.css` now applies token-driven label text and background treatment for default, hover, selected, and selected-but-unfocused states using `-pf-ui-surface-selected` and `-pf-ui-surface-selected-inactive`.
- Selected category rows keep their explicit token background on hover instead of falling back to the generic hover surface.

### Finding 2 (Medium) — Dark-mode settings inputs still using default Modena chrome: FIXED
- Added `SettingsUiStyles.applyCompactField(...)` so settings text/password editors consistently attach the shared `pf-ui-compact-field` class.
- Wired the shared field class into `SettingsSearchBar`, the generic `StringSettingControl`, `NumberSettingControl`, `PathSettingControl`, and `SecretSettingControl`, plus the custom text/password inputs in `KeyboardShortcutsCategory`, `McpServersCategory`, and `SecurityCategory`.
- Replaced remaining settings-form inline label/status warning colors in the touched categories with existing token-driven settings CSS classes.

### Regression Coverage
- `SettingsPanelFxTest.selectedCategoryUsesExplicitInactiveTokensWhenListLosesFocus()` verifies the selected category keeps the dark-theme primary text and swaps between active/inactive selection tokens when focus moves between the list and search field.
- `SettingsPanelFxTest.settingsEditorsReuseSharedCompactFieldStyleClass()` audits the touched settings controls/categories and fails if a text/password editor drops the shared `pf-ui-compact-field` styling path.

### Validation

| Check | Result | Date |
|-------|--------|------|
| `./mvnw -pl papiflyfx-docking-settings -am compile` | PASS | 2026-04-12 |
| `./mvnw -pl papiflyfx-docking-settings -am -Dtestfx.headless=true test` | PASS — settings `19/19`, upstream docks `55/55` | 2026-04-12 |
