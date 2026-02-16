# PapiflyFX Code - Progress Report

**Date:** 2026-02-15
**Status:** Phase 2 Complete (viewport and rendering)

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

## Update Log
- **2026-02-15:** Completed Phase 2 viewport and rendering — `GlyphCache`, `RenderLine`, `SelectionModel`, `Viewport` (canvas-based virtualized renderer), full keyboard/mouse input in `CodeEditor`, headless FX test infrastructure. 92 tests passing.
- **2026-02-14:** Completed Phase 1 core model implementation (`TextSource`, `LineIndex`, `Document`, edit commands) and added document-focused unit tests.
- **2026-02-14:** Completed Phase 0 module bootstrap, integration starter classes, and initial codec test.

## Phase Status
| Phase | Description | Status |
| --- | --- | --- |
| 0 | Module bootstrap + integration skeleton | ✅ Complete |
| 1 | Document core and editing | ✅ Complete |
| 2 | Viewport and rendering | ✅ Complete |
| 3 | Incremental lexer pipeline | ⏳ Not started |
| 4 | Gutter, markers, navigation | ⏳ Not started |
| 5 | Theme composition and mapping | ⏳ Not started |
| 6 | Persistence hardening/migration | ⏳ Not started |
| 7 | Failure handling and disposal | ⏳ Not started |
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

### Tests
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/state/EditorStateCodecTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/document/TextSourceTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/document/LineIndexTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/document/DocumentTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/render/SelectionModelTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/render/RenderLineTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/render/ViewportTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/api/CodeEditorIntegrationTest.java`

## Validation Results
- `mvn -pl papiflyfx-docking-code -am compile` -> ✅ success
- `mvn -pl papiflyfx-docking-code -am -Dtestfx.headless=true test` -> ✅ success (92 tests, 0 failures)
- `mvn -pl papiflyfx-docking-code test` -> expected failure without `-am` because local `papiflyfx-docking-docks` artifact is not pre-installed

## Notes / Known Issues
- Existing project warning remains in parent build config: duplicate `maven-release-plugin` declaration in root `pom.xml` pluginManagement.
- Lexer/syntax highlighting is pending Phase 3.

## Next Recommended Step
1. Start Phase 3 by implementing the incremental lexer pipeline for syntax coloring.
