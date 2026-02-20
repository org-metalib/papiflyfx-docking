# PapiflyFX Code Editor Actions Progress

**Date:** 2026-02-20  
**Status:** Profile A complete, Profile B/C pending

## 1. Summary

This progress report tracks implementation status for action requirements in:

- `spec/papiflyfx-docking-code-editor-actions/README.md`
- `spec/papiflyfx-docking-code-editor-actions/spec.md`

## 2. Implemented (Current Module Behavior)

From current `CodeEditor` behavior:

- Undo/redo shortcuts (`Primary+Z`, `Primary+Y`, `Primary+Shift+Z`)
- Copy/cut/paste (`Primary+C/X/V`)
- Select all (`Primary+A`)
- Character and line navigation via arrow keys
- Shift-extended range selection with arrows
- Home/end line navigation (baseline key handling)
- Backspace/delete/newline edits
- Find overlay (`Primary+F`)
- Go to line (`Primary+G`)
- Close search (`Escape`)
- Mouse click to place caret
- Mouse drag range selection
- Shift+click selection extension

## 3. Missing vs Requirements

### 3.1 Keyboard Gaps (Profile B)

- Word navigation and word selection expansion
- Word deletion left/right
- Document start/end and select-to-boundary shortcuts
- Delete line
- Move line up/down
- Duplicate line up/down
- Join lines
- Multi-cursor commands:
  - add next occurrence,
  - select all occurrences,
  - add cursor up/down,
  - undo last occurrence

### 3.2 Mouse Gaps (Profile B/C)

- Double-click word selection
- Triple-click line selection
- Alt/Option+click add-caret
- Box selection drag (modifier+drag)
- Middle-mouse box selection (optional)

### 3.3 Persistence Gap

- Current state is single-caret (`v1`).
- Multi-caret persistence schema (`v2`) not implemented.

## 4. Next Milestones

1. Introduce command abstraction + keymap tables.
2. Implement word/document movement and deletion.
3. Implement line operations with single-step undo semantics.
4. Implement multi-caret model and occurrence commands.
5. Implement mouse multi-caret and box selection.
6. Add persistence `v2` migration coverage.

## 5. Validation Baseline

Current module test baseline should continue passing while action work is added:

```bash
mvn -pl papiflyfx-docking-code -am -Dtestfx.headless=true test
```
