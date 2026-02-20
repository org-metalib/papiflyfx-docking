# PapiflyFX Code Editor Actions Implementation Plan

Scope: deliver keyboard/mouse action parity defined in `spec/papiflyfx-docking-code-editor-actions/spec.md` for module `papiflyfx-docking-code`.

## 1. Goals

- Preserve current stable editing behavior.
- Add missing word/line actions.
- Add multi-caret and rectangular selection support.
- Keep rendering/performance and docking persistence contracts intact.

## 2. Target Areas

Primary classes/packages expected to change:

- `org.metalib.papifly.fx.code.api.CodeEditor`
- `org.metalib.papifly.fx.code.render.SelectionModel` (or multi-caret successor model)
- `org.metalib.papifly.fx.code.document.Document`
- `org.metalib.papifly.fx.code.state.EditorStateData`
- `org.metalib.papifly.fx.code.state.EditorStateCodec`
- `org.metalib.papifly.fx.code.api.CodeEditorStateAdapter`

## 3. Delivery Phases

## Phase 0: Command Abstraction Baseline

Tasks:
- Introduce command IDs for all Profile A/B actions.
- Add platform keymap tables (Windows, macOS).
- Route existing input handling through command dispatch.

Exit criteria:
- Existing behavior unchanged.
- Tests pass with command-based dispatch enabled.

## Phase 1: Word + Document Navigation

Tasks:
- Add word navigation and word selection expansion.
- Add word deletion left/right.
- Add document start/end and select-to-boundary shortcuts.

Exit criteria:
- All Profile B word/document commands are functional.
- Unit tests cover token/whitespace/punctuation edge cases.

## Phase 2: Line Operations

Tasks:
- Add delete line, move line up/down, duplicate line up/down, join lines.
- Ensure operations work with and without selection.
- Ensure each action is single-step undo/redo.

Exit criteria:
- Line actions match expected behavior across single and multi-line selections.
- Undo/redo tests pass for each action.

## Phase 3: Multi-Caret Core

Tasks:
- Extend selection model to track multiple carets/selections.
- Implement add-next-occurrence, select-all-occurrences, add-cursor-up/down, undo-last-occurrence.
- Normalize overlapping carets/ranges deterministically.

Exit criteria:
- Multi-caret edit fan-out works for insert/delete/replace.
- Deterministic order and collapse rules are covered by tests.

## Phase 4: Mouse Multi-Caret + Box Selection

Tasks:
- Add `Alt/Option+Click` secondary caret creation.
- Add rectangular selection via modifier+drag.
- Add double-click word selection and triple-click line selection.

Exit criteria:
- Mouse gesture matrix passes headless TestFX tests.
- Box selection edits are undo-safe and deterministic.

## Phase 5: Persistence v2

Tasks:
- Extend `EditorStateData` and codec for multi-caret state.
- Add adapter migration from `v1` single-caret state.
- Keep tolerant restore for missing/invalid fields.

Exit criteria:
- Round-trip tests for `v2` pass.
- `v1` restore compatibility remains green.

## Phase 6: Hardening and Performance

Tasks:
- Re-run typing/scroll benchmarks with advanced actions enabled.
- Add regressions for input handler disposal and listener cleanup.
- Ensure no docking restore regressions.

Exit criteria:
- Latency and memory remain within existing module thresholds.
- Full module test suite passes in headless mode.

## 4. Risk Notes

- Multi-caret can increase complexity in text mutation ordering.
- Box selection can conflict with viewport coordinate conversion logic.
- Persistence expansion must not break existing saved sessions.

Mitigations:
- Normalize edit ranges before mutating document.
- Keep command tests independent of rendering tests.
- Maintain strict adapter version gates and fallback behavior.

## 5. Validation Commands

```bash
# Module tests
mvn -pl papiflyfx-docking-code -am -Dtestfx.headless=true test

# Docks + code integration coverage
mvn -pl papiflyfx-docking-code,papiflyfx-docking-docks -am -Dtestfx.headless=true test
```
