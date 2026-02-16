# PapiflyFX Code - Progress Report

**Date:** 2026-02-16
**Status:** Phase 3 complete; hardening work in progress (persistence/lifecycle/failure handling)

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

## Update Log
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
| 4 | Gutter, markers, navigation | ⏳ Not started |
| 5 | Theme composition and mapping | ⏳ Not started |
| 6 | Persistence hardening/migration | 🟡 In progress (version-aware restore hooks added) |
| 7 | Failure handling and disposal | 🟡 In progress (`dispose()` hooks added for editor/viewport) |
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

## Validation Results
- `mvn -pl papiflyfx-docking-code -am compile` -> ✅ success
- `mvn -pl papiflyfx-docking-code -am -Dtestfx.headless=true test` -> ✅ success (122 tests, 0 failures)
- `mvn -pl papiflyfx-docking-code test` -> expected failure without `-am` because local `papiflyfx-docking-docks` artifact is not pre-installed

## Notes / Known Issues
- Existing project warning remains in parent build config: duplicate `maven-release-plugin` declaration in root `pom.xml` pluginManagement.
- Gutter/markers/search/theme mapping are still pending MVP completion phases.

## Next Recommended Step
1. Start Phase 4 by implementing gutter, markers, and navigation/search features.
