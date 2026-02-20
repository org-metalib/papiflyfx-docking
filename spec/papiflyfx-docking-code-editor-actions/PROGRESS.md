# PapiflyFX Code Editor Actions Progress

**Date:** 2026-02-20
**Status:** Phases 0–3 complete, Phases 4–5 pending

## 1. Summary

This progress report tracks implementation status for action requirements in:

- `spec/papiflyfx-docking-code-editor-actions/README.md`
- `spec/papiflyfx-docking-code-editor-actions/spec.md`

## 2. Completed Phases

### Phase 0: Command Abstraction Baseline (Done)

Introduced a command dispatch layer that replaces the hardcoded `switch(KeyCode)` block in `CodeEditor.handleKeyPressed`.

New files:
- `command/EditorCommand.java` — enum of 38 command IDs covering Profiles A and B.
- `command/KeyBinding.java` — record `(KeyCode, shift, shortcut, alt)` used as map key.
- `command/KeymapTable.java` — static platform-aware keymap. macOS uses Alt for word nav and Cmd+Up/Down for document boundaries; Windows/Linux uses Ctrl.
- `command/WordBoundary.java` — word boundary utility (`findWordLeft`, `findWordRight`) using `[A-Za-z0-9_]` word class with whitespace and punctuation grouping.

Modified files:
- `api/CodeEditor.java` — `handleKeyPressed` now calls `KeymapTable.resolve(event)` → `executeCommand(EditorCommand)`. All existing handler methods are preserved and routed through command IDs.

Tests:
- `command/KeymapTableTest.java` — 13 tests covering all binding categories (navigation, selection, editing, clipboard, undo, search, word nav, doc boundaries, line ops, unmapped keys). Platform-aware assertions.
- `command/WordBoundaryTest.java` — 26 tests covering empty/null input, whitespace-only, underscore word chars, punctuation boundaries, line start/end, and mid-word positions.

### Phase 1: Word + Document Navigation (Done)

Added 8 handler methods to `CodeEditor`:
- `handleMoveWordLeft()` / `handleMoveWordRight()` — cross-line boundary support (jumps to end of previous line or start of next line at boundaries).
- `handleSelectWordLeft()` / `handleSelectWordRight()` — extends selection using word boundaries.
- `handleDeleteWordLeft()` / `handleDeleteWordRight()` — deletes to word boundary; joins lines when at line boundary.
- `handleDocumentStart(shift)` / `handleDocumentEnd(shift)` — jumps to (0,0) or last line/col.

Key bindings:
| Action | macOS | Windows/Linux |
|---|---|---|
| Word left/right | Alt+Left/Right | Ctrl+Left/Right |
| Select word left/right | Alt+Shift+Left/Right | Ctrl+Shift+Left/Right |
| Delete word left/right | Alt+Backspace/Delete | Ctrl+Backspace/Delete |
| Document start/end | Cmd+Up/Down | Ctrl+Home/End |
| Select to doc start/end | Cmd+Shift+Up/Down | Ctrl+Shift+Home/End |

### Phase 2: Line Operations (Done)

Added 6 handler methods to `CodeEditor`:
- `handleDeleteLine()` — deletes current line(s) covered by selection. Handles first, last, and only-line edge cases.
- `handleMoveLineUp()` / `handleMoveLineDown()` — swaps current line block with adjacent line via `document.replace()`. No-op at boundaries.
- `handleDuplicateLineUp()` / `handleDuplicateLineDown()` — copies line block above/below via `document.insert()`.
- `handleJoinLines()` — replaces newline at end of current line with a single space.

Each operation is a single `document.delete/insert/replace` call for one-step undo.

Key bindings:
| Action | macOS | Windows/Linux |
|---|---|---|
| Delete line | Cmd+Shift+K | Ctrl+Shift+K |
| Move line up/down | Alt+Up/Down | Alt+Up/Down |
| Duplicate line up/down | Alt+Shift+Up/Down | Alt+Shift+Up/Down |
| Join lines | Cmd+J | Ctrl+J |

Tests:
- `command/LineOperationsTest.java` — 15 tests covering delete (middle/first/last/only line, undo), move (up/down, undo), duplicate (up/down, undo), and join (basic, last-line no-op, undo, multi-line preservation).

### Phase 3: Multi-Caret Core (Done)

Added multi-caret editing support: selecting next/all occurrences, vertical cursor addition, undo last occurrence, and fan-out editing (typing/deleting at multiple carets simultaneously).

New files:
- `command/CaretRange.java` — immutable record `(anchorLine, anchorColumn, caretLine, caretColumn)` with start/end/offset helpers and `fromSelectionModel()` factory.
- `command/MultiCaretModel.java` — manages primary `SelectionModel` + secondary `List<CaretRange>` + `Deque<CaretRange>` occurrence stack. Supports `addCaret`, `clearSecondaryCarets`, `undoLastOccurrence`, `normalizeAndMerge`.
- `document/CompoundEdit.java` — package-private `EditCommand` that groups sub-edits into a single undo/redo step.

Modified files:
- `command/EditorCommand.java` — added 5 enum values: `SELECT_NEXT_OCCURRENCE`, `SELECT_ALL_OCCURRENCES`, `ADD_CURSOR_UP`, `ADD_CURSOR_DOWN`, `UNDO_LAST_OCCURRENCE`.
- `command/KeymapTable.java` — added 5 bindings (`Cmd/Ctrl+D`, `Cmd/Ctrl+Shift+L`, `Cmd/Ctrl+Alt+Up/Down`, `Cmd/Ctrl+U`).
- `command/WordBoundary.java` — made `isWordChar()` public for use by word-under-caret detection.
- `document/Document.java` — added `beginCompoundEdit()` / `endCompoundEdit()` / `isCompoundEditActive()` with `compoundBuffer` field; `insert/delete/replace` route through `recordEdit()` helper.
- `api/CodeEditor.java` — added `MultiCaretModel` field, 5 handler methods (`handleSelectNextOccurrence`, `handleSelectAllOccurrences`, `handleAddCursorUp/Down`, `handleUndoLastOccurrence`), `executeAtAllCarets()` fan-out helper, multi-caret collapse on single-caret navigation/mouse, fan-out for `handleKeyTyped/Backspace/Delete/Enter/Cut/Paste`.
- `render/Viewport.java` — added `MultiCaretModel` reference; modified `drawSelection`, `drawCaret`, `drawSelectionForLine`, `markSelectionRangeDirty` to render all carets when multi-caret is active.

Key bindings:
| Action | macOS | Windows/Linux |
|---|---|---|
| Select next occurrence | `Cmd+D` | `Ctrl+D` |
| Select all occurrences | `Cmd+Shift+L` | `Ctrl+Shift+L` |
| Add cursor up | `Cmd+Alt+Up` | `Ctrl+Alt+Up` |
| Add cursor down | `Cmd+Alt+Down` | `Ctrl+Alt+Down` |
| Undo last occurrence | `Cmd+U` | `Ctrl+U` |

Tests:
- `command/MultiCaretModelTest.java` — 8 tests (add/clear/undo/merge/sort).
- `command/MultiCaretEditTest.java` — 5 tests (insert/delete/selection at multiple carets, compound undo/redo).
- `document/CompoundEditTest.java` — 6 tests (grouped inserts/deletes/replaces, empty compound, redo).
- `command/KeymapTableTest.java` — added `multiCaretBindings()` test (5 new bindings).

## 3. Implemented (Full List)

From Profile A (unchanged) + Profile B (newly added):

- Undo/redo shortcuts (`Primary+Z`, `Primary+Y`, `Primary+Shift+Z`)
- Copy/cut/paste (`Primary+C/X/V`)
- Select all (`Primary+A`)
- Character and line navigation via arrow keys
- Shift-extended range selection with arrows
- Home/end line navigation
- Backspace/delete/newline edits
- Find overlay (`Primary+F`)
- Go to line (`Primary+G`)
- Close search (`Escape`)
- Mouse click to place caret
- Mouse drag range selection
- Shift+click selection extension
- **Word navigation left/right** (Phase 1)
- **Word selection left/right** (Phase 1)
- **Word deletion left/right** (Phase 1)
- **Document start/end navigation** (Phase 1)
- **Select to document start/end** (Phase 1)
- **Delete line** (Phase 2)
- **Move line up/down** (Phase 2)
- **Duplicate line up/down** (Phase 2)
- **Join lines** (Phase 2)
- **Select next occurrence** (Phase 3)
- **Select all occurrences** (Phase 3)
- **Add cursor up/down** (Phase 3)
- **Undo last occurrence** (Phase 3)
- **Multi-caret fan-out editing** (Phase 3)
- **Compound edit (single-step undo for multi-caret)** (Phase 3)

## 4. Remaining Gaps

### 4.1 Mouse (Profile B/C)

- Double-click word selection
- Triple-click line selection
- Alt/Option+click add-caret
- Box selection drag (modifier+drag)
- Middle-mouse box selection (optional)

### 4.3 Persistence

- Current state is single-caret (`v1`).
- Multi-caret persistence schema (`v2`) not implemented.

## 5. Next Milestones

1. ~~Introduce command abstraction + keymap tables.~~ Done
2. ~~Implement word/document movement and deletion.~~ Done
3. ~~Implement line operations with single-step undo semantics.~~ Done
4. ~~Implement multi-caret model and occurrence commands (Phase 3).~~ Done
5. Implement mouse multi-caret and box selection (Phase 4).
6. Add persistence `v2` migration coverage (Phase 5).

## 6. Test Validation

All 283 tests pass (74 new + 209 existing):

```bash
mvn -pl papiflyfx-docking-code -am -Dtestfx.headless=true test
# Tests run: 283, Failures: 0, Errors: 0, Skipped: 0

# Run only Phase 3 tests
mvn -pl papiflyfx-docking-code -am \
  -Dtest="MultiCaretModelTest,MultiCaretEditTest,CompoundEditTest,KeymapTableTest" \
  -Dsurefire.failIfNoSpecifiedTests=false test
# Tests run: 33, Failures: 0, Errors: 0, Skipped: 0

# Run all command/document tests
mvn -pl papiflyfx-docking-code -am \
  -Dtest="WordBoundaryTest,KeymapTableTest,LineOperationsTest,MultiCaretModelTest,MultiCaretEditTest,CompoundEditTest" \
  -Dsurefire.failIfNoSpecifiedTests=false test
# Tests run: 74, Failures: 0, Errors: 0, Skipped: 0
```

## 7. File Change Summary

| File | Action | Phase |
|---|---|---|
| `command/EditorCommand.java` | **New** — command ID enum (38 values) | 0 |
| `command/KeyBinding.java` | **New** — key combination record | 0 |
| `command/KeymapTable.java` | **New** — platform keymap resolution | 0 |
| `command/WordBoundary.java` | **New** — word boundary utility | 0 |
| `command/package-info.java` | **New** — package docs | 0 |
| `api/CodeEditor.java` | **Modified** — command dispatch + 14 new handlers | 0–2 |
| `command/KeymapTableTest.java` | **New** — 13+1 keymap tests | 0, 3 |
| `command/WordBoundaryTest.java` | **New** — 26 boundary edge-case tests | 1 |
| `command/LineOperationsTest.java` | **New** — 15 line operation tests | 2 |
| `command/package-info.java` (test) | **New** — test package docs | 0 |
| `command/CaretRange.java` | **New** — caret position record | 3 |
| `command/MultiCaretModel.java` | **New** — multi-caret model with occurrence stack | 3 |
| `document/CompoundEdit.java` | **New** — grouped edit command for single-step undo | 3 |
| `command/WordBoundary.java` | **Modified** — `isWordChar()` made public | 3 |
| `document/Document.java` | **Modified** — compound edit API | 3 |
| `render/Viewport.java` | **Modified** — multi-caret rendering | 3 |
| `command/MultiCaretModelTest.java` | **New** — 8 multi-caret model tests | 3 |
| `command/MultiCaretEditTest.java` | **New** — 5 multi-caret edit tests | 3 |
| `document/CompoundEditTest.java` | **New** — 6 compound edit tests | 3 |
