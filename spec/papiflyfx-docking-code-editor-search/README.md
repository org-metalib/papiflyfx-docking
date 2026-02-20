# PapiflyFX Code Editor Search/Replace Review

Reference UI targets:

- `spec/papiflyfx-docking-code-editor-search/text-search.png` (collapsed find row)
- `spec/papiflyfx-docking-code-editor-search/text-search-replace.png` (expanded replace row)

Reviewed source:

- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/search/SearchController.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/search/SearchModel.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/command/EditorCommand.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/command/KeymapTable.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/search/SearchModelTest.java`

## Findings (Ordered by Severity)

## 1. Search overlay can cover the whole editor area (text appears to disappear on Cmd+F)

Severity: Critical  
User-observed behavior: pressing `Cmd+F` shows search UI but editor text disappears behind it.

Evidence:

- Search UI is added as a `StackPane` child overlay and aligned top-center: `CodeEditor.java:137-139`.
- Search overlay is a resizable `VBox` with opaque background fill: `SearchController.java:33`, `SearchController.java:56-67`, `SearchController.java:180-187`.
- Overlay is toggled to managed+visible on open without bounding its size to preferred dimensions: `SearchController.java:224-229`.
- There is no `setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE)` / width clamp / clip policy on `SearchController`.

Impact:

- Search panel can occupy the full editor layer instead of just a compact top strip.
- Core editing visibility is blocked, making find/replace practically unusable.

Solution:

- Keep overlay as floating compact panel:
  - set `searchController.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE)`,
  - set `searchController.setPrefWidth(...)` and do not allow fill-to-parent expansion,
  - keep only panel background visible (not full-layer paint).
- Alternative: host search UI in a separate non-filling container (`BorderPane` top slot) instead of overlay `StackPane`.
- Add UI integration test asserting that opening search does not occlude viewport content area.

## 2. Missing whole-word search (`W`) support

Severity: High  
Target mismatch: screenshot shows `W` toggle.

Evidence:

- Only regex + case toggles are implemented in UI: `SearchController.java:92-104`.
- No whole-word state/logic in model: `SearchModel.java:18-79`, `SearchModel.java:226-276`.

Impact:

- Cannot match VS Code-like `Whole Word` behavior from target UI.

Solution:

- Add `wholeWord` flag to `SearchModel`.
- Add `W` toggle in `SearchController`.
- Enforce boundary check in both plain-text and regex modes before accepting a match.

## 3. Replace row is always visible; no collapsed/expanded mode

Severity: High  
Target mismatch: screenshot shows collapsed single-row find state and expandable replace state.

Evidence:

- Both rows are always added and visible together: `SearchController.java:106-127`.
- No expand/collapse control state exists.

Impact:

- Current UX cannot reproduce screenshot behavior.
- Consumes extra vertical space permanently.

Solution:

- Add an explicit `replaceMode` state.
- Add left chevron toggle button.
- Keep replace row `managed=false, visible=false` by default; show only in replace mode.

## 4. No shortcut for “Open Replace” mode (`Ctrl+H` / `Cmd+Option+F`)

Severity: High  
Target mismatch: replace mode should be directly accessible via keyboard.

Evidence:

- `EditorCommand` has only `OPEN_SEARCH` and `GO_TO_LINE` for search-related global commands: `EditorCommand.java:42-43`.
- Keymap maps only `Primary+F` and `Primary+G`: `KeymapTable.java:72-75`.
- No replace-open command in `CodeEditor` dispatch: `CodeEditor.java:397-400`.

Impact:

- Keyboard-only workflow is incomplete.

Solution:

- Add `OPEN_REPLACE` command.
- Map:
  - Windows/Linux: `Ctrl+H`
  - macOS: `Cmd+Option+F`
- Implement `CodeEditor.openReplace()` to open overlay in expanded replace mode and focus replace field.

## 5. Search results can become stale after document edits while overlay is open

Severity: High  

Evidence:

- `SearchController` recomputes only on query/toggle/replace actions: `SearchController.java:129-155`, `SearchController.java:273-315`.
- `CodeEditor` wires document listener for gutter width only, not search refresh: `CodeEditor.java:110-125`.
- `Viewport` renders cached `searchMatches` until explicitly replaced: `Viewport.java:148-152`, `Viewport.java:572-635`.

Impact:

- Highlight positions and match counts can diverge from current document content.

Solution:

- Re-run search on document change while search overlay is open.
- Preserve user context by selecting nearest match to current caret (`SearchModel.selectNearestMatch(...)`) instead of resetting to index 0 every time.
- Debounce refresh to avoid unnecessary churn while typing quickly.

## 6. Regex replace does not support capture-group substitution

Severity: Medium

Evidence:

- Replacement is inserted literally for both `replaceCurrent` and `replaceAll`: `SearchModel.java:187-214`.
- No `Matcher.appendReplacement`/group expansion logic exists.

Impact:

- In regex mode, common replace patterns like `$1_$2` will not behave as expected.

Solution:

- When `regexMode` is enabled, execute replacements using matcher-based substitution semantics.
- Keep literal replacement path for plain-text mode.

## 7. Replace controls are not disabled when no matches

Severity: Medium  
Target mismatch: screenshot shows disabled Replace/Replace All when no results.

Evidence:

- Buttons are always active; no disable binding: `SearchController.java:116-123`, `SearchController.java:304-315`.

Impact:

- UI allows no-op actions and does not communicate state clearly.

Solution:

- Disable `Replace` and `Replace All` when `matchCount == 0` or query is empty.
- Keep visual state synced with results count.

## 8. Missing “preserve case” option in replace row (`Aa`) and missing scope/filter controls

Severity: Medium  
Target mismatch: screenshot shows extra options in expanded mode.

Evidence:

- No preserve-case toggle in replace row (`SearchController.java:109-127`).
- Model has no preserve-case flag (`SearchModel.java:18-79`).
- No include/exclude scope/filter actions in controller/model.

Impact:

- Cannot match advanced behavior indicated by target UI.

Solution:

- Add optional `preserveCase` toggle and transform replacement casing based on matched token shape.
- If required by product scope, add scope controls (document/selection and include/exclude filters).

## 9. Replace-field Enter key has no replace behavior

Severity: Medium

Evidence:

- `replaceField` key handler only supports Escape: `SearchController.java:150-155`.

Impact:

- Slower keyboard workflow than expected for a text editor find/replace panel.

Solution:

- Map `Enter` in replace field to `replaceCurrent()`.
- Optionally map `Shift+Enter` to replace + previous navigation, or to `replaceAll()`.

## 10. Limited automated coverage for search UI integration

Severity: Low

Evidence:

- Only model tests exist in `search` package: `SearchModelTest.java` (no `SearchController` FX tests).
- No dedicated tests for search/replace keybinding behavior (`Ctrl+H`, expanded mode, button disable states).

Impact:

- Regression risk for UI and keyboard behavior.

Solution:

- Add `SearchControllerFxTest` for:
  - collapsed/expanded states,
  - toggle behavior (`Cc`, `W`, `.*`, optional `Aa`),
  - Enter/Escape semantics,
  - disabled button states.
- Extend `KeymapTableTest`/integration tests for replace-open shortcuts.

## Recommended Implementation Shape (To Match Screenshots)

1. Overlay state model:
   - `findMode` (single row)
   - `replaceMode` (two rows)
2. Find row controls:
   - query field
   - toggles: case (`Cc`), whole word (`W`), regex (`.*`)
   - count + next/previous + close
3. Replace row controls:
   - replacement field
   - optional preserve case (`Aa`)
   - `Replace`, `Replace All` (disabled when no matches)
4. Keyboard:
   - `Primary+F` -> open find
   - `Ctrl+H` (win/linux), `Cmd+Option+F` (mac) -> open replace
   - `Enter`/`Shift+Enter` for next/previous in find field
   - `Enter` in replace field -> replace current
5. Model:
   - add `wholeWord` (required)
   - add regex group-aware replacement (required for regex mode)
   - optional `preserveCase`
6. Live sync:
   - refresh search results on document changes while overlay is open.
7. Layout safety:
   - keep search panel non-filling and limited to compact preferred size.

## Conclusion

The current implementation provides a functional baseline find/replace overlay, but it does not yet match the expected UX and behavior shown in the reference screenshots.  
The highest-priority gaps are:

1. overlay currently hiding editor content on search open,
2. whole-word support,
3. collapsible replace mode,
4. replace-open shortcut,
5. live resync after document edits.
