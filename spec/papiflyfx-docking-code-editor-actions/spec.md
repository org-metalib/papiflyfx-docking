# PapiflyFX Code Editor Actions Specification

This document defines the keyboard and mouse action requirements for the `papiflyfx-docking-code` module, normalized from `spec/papiflyfx-docking-code-editor-actions/README.md`.

## 1. Objective

- Define a clear, testable command set for text editing interactions.
- Align behavior with common modern editor conventions (VS Code / Sublime style).
- Keep platform parity between Windows and macOS.

## 2. Scope

### 2.1 In Scope

- Caret navigation and selection commands.
- Editing shortcuts (undo/redo, line actions, word actions).
- Multi-cursor and rectangular selection actions.
- Mouse click/drag gestures that affect editor state.

### 2.2 Out of Scope

- IDE semantic navigation (`go to definition`, symbol index).
- Custom keybinding UI.
- Language-server behavior and refactor actions.

## 3. Command Model

Platform modifiers:

| Logical Modifier | Windows | macOS |
| --- | --- | --- |
| `Primary` | `Ctrl` | `Cmd` |
| `Word` | `Ctrl` | `Option` |
| `MultiCursor` | `Alt` | `Option` |

The editor should bind shortcuts to command IDs, then execute behavior by command ID (not by hardcoded key-branches only).

## 4. Keyboard Actions

### 4.1 Core Actions (Profile A)

| Command | Windows | macOS | Status (2026-02-20) |
| --- | --- | --- | --- |
| Undo | `Ctrl+Z` | `Cmd+Z` | Implemented |
| Redo | `Ctrl+Y`, `Ctrl+Shift+Z` | `Shift+Cmd+Z` | Implemented |
| Copy | `Ctrl+C` | `Cmd+C` | Implemented |
| Cut | `Ctrl+X` | `Cmd+X` | Implemented |
| Paste | `Ctrl+V` | `Cmd+V` | Implemented |
| Select all | `Ctrl+A` | `Cmd+A` | Implemented |
| Move by char | `Left/Right` | `Left/Right` | Implemented |
| Move by line | `Up/Down` | `Up/Down` | Implemented |
| Extend selection | `Shift+Arrows` | `Shift+Arrows` | Implemented |
| Line start/end | `Home/End` | `Cmd+Left/Right` | Partial |
| Find | `Ctrl+F` | `Cmd+F` | Implemented |
| Go to line | `Ctrl+G` | `Cmd+G` | Implemented |
| Close find | `Escape` | `Escape` | Implemented |

### 4.2 Required Actions (Profile B)

| Command | Windows | macOS |
| --- | --- | --- |
| Move by word | `Ctrl+Left/Right` | `Option+Left/Right` |
| Select by word | `Ctrl+Shift+Left/Right` | `Option+Shift+Left/Right` |
| Delete word left/right | `Ctrl+Backspace/Delete` | `Option+Delete` / `Option+Fn+Delete` |
| Document start/end | `Ctrl+Home/End` | `Cmd+Up/Down` |
| Select to doc bounds | `Ctrl+Shift+Home/End` | `Shift+Cmd+Up/Down` |
| Delete current line | `Ctrl+Shift+K` | `Cmd+Shift+K` |
| Move line up/down | `Alt+Up/Down` | `Option+Up/Down` |
| Duplicate line up/down | `Shift+Alt+Up/Down` | `Shift+Option+Up/Down` |
| Join lines | `Ctrl+J` | `Cmd+J` |
| Select next occurrence | `Ctrl+D` | `Cmd+D` |
| Select all occurrences | `Ctrl+Shift+L` | `Cmd+Shift+L` |
| Add cursor up/down | `Ctrl+Alt+Up/Down` | `Cmd+Option+Up/Down` |
| Undo last occurrence | `Ctrl+U` | `Cmd+U` |

### 4.3 Optional Actions (Profile C)

| Command | Windows | macOS |
| --- | --- | --- |
| Skip occurrence in sequence | `Ctrl+K Ctrl+D` | `Cmd+K Cmd+D` |
| Expand/shrink semantic selection | `Shift+Alt+Right/Left` | `Ctrl+Shift+Cmd+Right/Left` |
| Zoom with wheel | `Ctrl+Wheel` | `Cmd+Wheel` |

## 5. Mouse Actions

| Action | Windows | macOS | Status (2026-02-20) |
| --- | --- | --- | --- |
| Set caret | Click | Click | Implemented |
| Drag range selection | Drag | Drag | Implemented |
| Extend selection | `Shift+Click` | `Shift+Click` | Implemented |
| Select word | Double click | Double click | Planned |
| Select line | Triple click | Triple click | Planned |
| Add caret at point | `Alt+Click` | `Option+Click` | Planned |
| Box selection drag | `Shift+Alt+Drag` | `Shift+Option+Drag` | Planned |
| Box selection by middle mouse | Middle-drag | N/A | Planned |

## 6. Behavioral Rules

- Shift-extended operations must preserve anchor stability.
- Multi-caret edits must be deterministic for overlapping carets/selections.
- Line operations act on complete line spans when selection exists.
- Word boundaries must be deterministic (`[A-Za-z0-9_]` word class baseline).
- Undo/redo should treat each line action as a single history step.

## 7. Persistence Impact

Current persisted state is single-caret (`EditorStateData` v1).  
For full multi-caret support, define `v2` state with:

- primary caret position,
- secondary caret positions,
- optional per-caret selection ranges.

`v1` payload restore must remain supported.

## 8. Acceptance Criteria

| ID | Criterion |
| --- | --- |
| AC-1 | Profile A behavior remains stable and fully tested in headless CI. |
| AC-2 | Profile B keyboard shortcuts behave correctly on Windows and macOS mappings. |
| AC-3 | Profile B mouse gestures for multi-caret and box selection are deterministic. |
| AC-4 | Undo/redo correctness is preserved after multi-caret and line operations. |
| AC-5 | Session save/restore remains backward-compatible (`v1`) and supports new action state (`v2`). |

## 9. Verification Strategy

- Unit tests for word boundaries, line action semantics, and multi-caret normalization.
- JavaFX/TestFX integration tests for key/mouse gesture matrices.
- Docking integration tests for state round-trip and fallback order (adapter -> factory -> placeholder).
