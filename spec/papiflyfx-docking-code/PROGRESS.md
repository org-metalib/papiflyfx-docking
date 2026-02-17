# PapiflyFX Code - Progress Report

**Date:** 2026-02-17
**Status:** Phase 5 complete; Review 5 (Codex) fixes applied; hardening work in progress

## Summary
- Specification and implementation plan were updated to target a separate module: `papiflyfx-docking-code`.
- New Maven module scaffold was created and wired into the root aggregator.
- Phase 0 starter classes were implemented:
  - `CodeEditor` (placeholder editor node with state capture/apply)
  - `CodeEditorFactory` (`ContentFactory` for `factoryId = "code-editor"`)
  - `CodeEditorStateAdapter` (`ContentStateAdapter`, version `1`)
  - `EditorStateData` and `EditorStateCodec`
- Phase 1 document core was implemented:
  - `TextSource`, `LineIndex`, `Document`
  - edit command primitives (`InsertEdit`, `DeleteEdit`, `ReplaceEdit`)
  - undo/redo and line/column mapping behavior
- Unit tests now cover state codec and document core logic.
- Follow-up hardening fixes were implemented for review findings:
  - restored caret state now applies to runtime `SelectionModel`,
  - undo/redo no longer resets caret to `0:0`,
  - persisted scroll offset is synchronized with actual viewport clamp,
  - version-aware state restore path added to `CodeEditorStateAdapter`,
  - disposal hooks added to `CodeEditor` and `Viewport`.
- Phase 3 incremental lexer pipeline was implemented:
  - token model (`Token`, `TokenType`, `LexState`, `TokenMap`) and per-line cache,
  - language lexers for Java, JSON, JavaScript, and plain-text fallback,
  - incremental re-lex engine with line-entry/exit state propagation,
  - debounced async lexer pipeline with revision-safe FX-thread token apply,
  - viewport token-aware rendering and editor language wiring.
- Phase 4 gutter, markers, and navigation was implemented:
  - line number gutter (`GutterView`) with dynamic width and active-line highlighting,
  - marker lane with `MarkerModel` integration (error, warning, info, breakpoint, bookmark),
  - search/replace model (`SearchModel`) supporting plain text and regex modes,
  - search/replace UI overlay (`SearchController`) with navigation and mode toggles,
  - go-to-line action via `Ctrl/Cmd+G` dialog and programmatic `goToLine(int)`,
  - search highlight rendering in `Viewport` with current-match distinction,
  - gutter scroll synchronization with viewport,
  - keyboard shortcuts: `Ctrl/Cmd+F` (search), `Ctrl/Cmd+G` (go-to-line), `Escape` (close search).

## Update Log
- **2026-02-17:** Applied Review 5 (Codex) fixes:
  - **HIGH** — Fixed typing caret advancement: `handleKeyTyped` now computes new offset after insert and calls `moveCaretToOffset()` instead of broken `moveCaretRight()`. Removed unused `moveCaretRight` method.
  - **HIGH** — Fixed DockLeaf content disposal: added `DisposableContent` interface in `papiflyfx-docking-docks`, `DockLeaf.dispose()` now calls `DisposableContent.dispose()` on content nodes. `CodeEditor` implements `DisposableContent`.
  - **HIGH** — Fixed restore fallback order in `LayoutFactory.buildLeaf()`: when adapter is absent for a typeKey, factory creation is attempted before placeholder fallback (previously a placeholder was created immediately, blocking factory).
  - **MEDIUM** — Fixed state rehydration: `CodeEditorStateAdapter.restore()` now loads file content from `filePath` when readable; falls back to empty document with metadata preserved for missing/unreadable files (spec §6).
  - Issues 5 (per-edit O(n)) and 6 (dirty-region redraw) deferred to Phase 8 benchmarks.
  - Added 4 regression tests in `CodeEditorIntegrationTest` (typing caret, DockLeaf dispose, file rehydration, missing file fallback).
  - Added 3 tests in `DockLeafTest` (dispose with DisposableContent, non-disposable, null).
  - Added 1 test in `LayoutFactoryFxTest` (adapter-missing factory fallback order).
  - Test suite now 186 code-module + 41 docks-module tests passing.
- **2026-02-17:** Completed Phase 5 theme composition and mapping:
  - Added `CodeEditorTheme` record with 30 palette fields (spec core 10 + syntax/gutter/search/overlay colors).
  - Added `CodeEditorThemeMapper` that maps docking `Theme` to `CodeEditorTheme` via composition (dark/light detection from background brightness).
  - Refactored `Viewport` to render all colors from `CodeEditorTheme` instead of hardcoded constants.
  - Refactored `GutterView` to use `CodeEditorTheme` for background, line numbers, and markers.
  - Refactored `SearchController` to use `CodeEditorTheme` for overlay styling with runtime refresh.
  - Added `CodeEditor.bindThemeProperty(ObjectProperty<Theme>)` to observe `DockManager.themeProperty()` changes.
  - Added `CodeEditor.setEditorTheme(CodeEditorTheme)` for direct palette control.
  - Dispose unbinds theme listener; no inheritance from docking `Theme` record.
  - Added 9 mapper unit tests and 7 integration tests. Test suite now 182 passing.
- **2026-02-16:** Completed Phase 4 gutter, markers, and navigation:
  - Added `gutter/` package: `MarkerType`, `Marker`, `MarkerModel`, `GutterView`.
  - Added `search/` package: `SearchMatch`, `SearchModel`, `SearchController`.
  - Modified `Viewport` to render search match highlights with current-match distinction.
  - Modified `CodeEditor` to integrate gutter (BorderPane layout), marker model, search controller (StackPane overlay), go-to-line dialog, and keyboard shortcuts (Ctrl+F, Ctrl+G, Escape).
  - Added 36 new tests (15 MarkerModel, 16 SearchModel, 6 GutterView integration). Test suite now 158 passing.
- **2026-02-16:** Applied Review 3 (Codex) fixes:
  - **HIGH** — Fixed `IncrementalLexerEngine` stale-lines bug: early-stop optimization now validates all remaining baseline lines text-match before copying tail; non-contiguous edits with unchanged line count no longer produce stale tokens.
  - **MEDIUM** — Fixed `MarkdownLexer` ordered list detection for numbers >= 10: replaced single-digit `startsWith(". ", 1)` check with arbitrary digit-span parser; marker length is now computed dynamically.
  - **MEDIUM** — Moved line snapshot creation off UI/change thread in `IncrementalLexerPipeline`: caller now captures lightweight `document.getText()` string; `splitLines()` runs on worker thread.
  - Added 5 regression tests (1 engine non-contiguous edit, 4 Markdown ordered list including negative case). Test suite now 122 passing.
- **2026-02-16:** Added MarkdownLexer with support for headlines, list items, and code blocks. Updated TokenType and Viewport with Markdown-specific categories and colors. Added MarkdownLexerTest. Test suite now 117 passing.
- **2026-02-16:** Completed Phase 3 incremental lexer pipeline. Added lexer model/engine/pipeline, Java/JSON/JavaScript lexers, tokenized viewport rendering, and language-driven async syntax updates in `CodeEditor`. Added 14 Phase 3 tests (language lexers, incremental engine/pipeline, and editor integration). Test suite now 113 passing.
- **2026-02-16:** Applied Review 2 fixes: caret-state restore wiring, undo/redo caret behavior, scroll-state sync, adapter version-aware restore fallback, and disposal APIs (`CodeEditor.dispose`, `Viewport.dispose`). Added 7 new integration tests. Test suite now 99 passing.
- **2026-02-15:** Completed Phase 2 viewport and rendering — `GlyphCache`, `RenderLine`, `SelectionModel`, `Viewport` (canvas-based virtualized renderer), full keyboard/mouse input in `CodeEditor`, headless FX test infrastructure. 92 tests passing.
- **2026-02-14:** Completed Phase 1 core model implementation (`TextSource`, `LineIndex`, `Document`, edit commands) and added document-focused unit tests.
- **2026-02-14:** Completed Phase 0 module bootstrap, integration starter classes, and initial codec test.

## Phase Status
| Phase | Description | Status |
| --- | --- | --- |
| 0 | Module bootstrap + integration skeleton | ✅ Complete |
| 1 | Document core and editing | ✅ Complete |
| 2 | Viewport and rendering | ✅ Complete |
| 3 | Incremental lexer pipeline | ✅ Complete |
| 4 | Gutter, markers, navigation | ✅ Complete |
| 5 | Theme composition and mapping | ✅ Complete |
| 6 | Persistence hardening/migration | 🟡 In progress (version-aware restore hooks + file rehydration added) |
| 7 | Failure handling and disposal | 🟡 In progress (`dispose()` hooks + `DisposableContent` + DockLeaf integration) |
| 8 | Benchmarks and documentation hardening | ⏳ Not started |

## Implemented Files (Highlights)

### Module Wiring
- `pom.xml` (aggregator includes `papiflyfx-docking-code`)
- `papiflyfx-docking-code/pom.xml`
- `papiflyfx-docking-code/README.md`

### Phase 0 Source
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditorFactory.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditorStateAdapter.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/state/EditorStateData.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/state/EditorStateCodec.java`

### Phase 1 Source
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/document/TextSource.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/document/LineIndex.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/document/Document.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/document/InsertEdit.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/document/DeleteEdit.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/document/ReplaceEdit.java`

### Phase 2 Source
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/GlyphCache.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/RenderLine.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/SelectionModel.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/Viewport.java`

### Phase 2 Modified
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java` (replaced placeholder with Document + Viewport + input handling)
- `papiflyfx-docking-code/pom.xml` (added headless TestFX surefire config)

### Phase 3 Source
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/TokenType.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/Token.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/LexState.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/LexResult.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/Lexer.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/LineTokens.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/TokenMap.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/PlainTextLexer.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/JavaLexer.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/JsonLexer.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/JavaScriptLexer.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/MarkdownLexer.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/LexerRegistry.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/IncrementalLexerEngine.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/IncrementalLexerPipeline.java`

### Phase 3 Modified
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java` (async lexer pipeline integration + language listener wiring)
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/Viewport.java` (token-map rendering support)
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/RenderLine.java` (tokenized line payload)

### Phase 4 Source
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/gutter/MarkerType.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/gutter/Marker.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/gutter/MarkerModel.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/gutter/GutterView.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/search/SearchMatch.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/search/SearchModel.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/search/SearchController.java`

### Phase 4 Modified
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java` (gutter + search + go-to-line integration, BorderPane layout)
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/Viewport.java` (search highlight rendering)

### Phase 5 Source
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/theme/CodeEditorTheme.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/theme/CodeEditorThemeMapper.java`

### Phase 5 Modified
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/Viewport.java` (replaced hardcoded colors with `CodeEditorTheme` palette)
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/gutter/GutterView.java` (replaced hardcoded colors with `CodeEditorTheme` palette)
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/search/SearchController.java` (replaced hardcoded colors with `CodeEditorTheme` palette + runtime refresh)
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java` (theme binding, `bindThemeProperty`, `setEditorTheme`, dispose unbind)

### Review 5 Fixes (cross-module)
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/layout/DisposableContent.java` (new interface)
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/core/DockLeaf.java` (dispose calls `DisposableContent.dispose()`)
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/layout/LayoutFactory.java` (fixed restore fallback order)
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java` (implements `DisposableContent`, fixed typing caret, removed `moveCaretRight`)
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditorStateAdapter.java` (file rehydration + missing-file fallback)

### Post-Phase 2 Hardening (2026-02-16)
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java`
  - state application now drives caret model,
  - undo/redo caret behavior improved,
  - scroll offset synchronization with viewport clamp,
  - disposal lifecycle hook.
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditorStateAdapter.java`
  - version-aware restore path with safe fallback.
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/Viewport.java`
  - disposal lifecycle hook and listener cleanup.

### Tests
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/state/EditorStateCodecTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/document/TextSourceTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/document/LineIndexTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/document/DocumentTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/render/SelectionModelTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/render/RenderLineTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/render/ViewportTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/api/CodeEditorIntegrationTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/lexer/JavaLexerTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/lexer/JsonLexerTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/lexer/JavaScriptLexerTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/lexer/IncrementalLexerEngineTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/lexer/IncrementalLexerPipelineTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/lexer/MarkdownLexerTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/gutter/MarkerModelTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/gutter/GutterViewTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/search/SearchModelTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/theme/CodeEditorThemeMapperTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/theme/CodeEditorThemeIntegrationTest.java`
- `papiflyfx-docking-docks/src/test/java/org/metalib/papifly/fx/docks/core/DockLeafTest.java`

## Validation Results
- `mvn -pl papiflyfx-docking-code -am compile` -> ✅ success
- `mvn -pl papiflyfx-docking-code -am -Dtestfx.headless=true test` -> ✅ success (186 code-module + 41 docks-module tests, 0 failures)
- `mvn -pl papiflyfx-docking-code test` -> expected failure without `-am` because local `papiflyfx-docking-docks` artifact is not pre-installed

## Notes / Known Issues
- Existing project warning remains in parent build config: duplicate `maven-release-plugin` declaration in root `pom.xml` pluginManagement.
- Review 5 issues 5 (per-edit O(n) line-index rebuild) and 6 (dirty-region redraw) are deferred to Phase 8 benchmarks.
- Remaining hardening phases are still pending MVP completion.

## Next Recommended Step
1. Start Phase 6 by completing persistence hardening and migration (full `EditorStateData` v1 save/restore contract, migration hooks).
