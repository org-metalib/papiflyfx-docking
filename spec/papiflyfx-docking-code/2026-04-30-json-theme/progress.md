# JSON Theme Highlighting Progress

Last updated: 2026-04-30  
Lead: `@spec-steward`

## Overall Status

- Phase 0: `completed`
- Phase 1: `pending`
- Phase 2: `pending`
- Phase 3: `pending`
- Phase 4: `pending`

## Phase 0 - Analysis and Planning

- [x] Reviewed current JSON highlighting implementation.
- [x] Identified that JSON keys and string values both use `TokenType.STRING`.
- [x] Confirmed `TextPass` does not map punctuation to a dedicated color.
- [x] Confirmed `CodeEditorTheme` does not expose JSON-key-specific color.
- [x] Created implementation plan and acceptance criteria.

## Planned Implementation Progress

### Phase 1 - Token Model

- [ ] Add `JSON_KEY` to `TokenType`.
- [ ] Update `JsonLexer` to classify object-key strings.
- [ ] Add lexer regression coverage.

### Phase 2 - Theme and Rendering

- [ ] Add `jsonKeyColor` to `CodeEditorTheme`.
- [ ] Update default dark and light palettes.
- [ ] Update `CodeEditorThemeMapper`.
- [ ] Update `TextPass` token color routing.
- [ ] Add theme mapper regression coverage.

### Phase 3 - Optional Punctuation Polish

- [ ] Decide whether to add `punctuationColor` in this rollout.
- [ ] Implement punctuation rendering only if accepted by `@ui-ux-designer`.

### Phase 4 - Validation

- [ ] Run focused code-module tests.
- [ ] Run full headless `papiflyfx-docking-code` tests.
- [ ] Record validation results here.

## Validation

Not run. This task created planning artifacts only.

## Notes

Recommended first implementation slice: `TokenType.JSON_KEY` plus `JsonLexer` classification and `TextPass` rendering. Punctuation color should remain optional until the visual review confirms it improves readability without adding noise.
