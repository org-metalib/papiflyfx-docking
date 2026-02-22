# papiflyfx-docking-code Quality Remediation Progress

Date: 2026-02-22  
Plan reference: `spec/papiflyfx-docking-code-quality1/implementation.md`

## Status Summary

- Implemented remediation across WS1-WS5 with code, tests, and full module validation.
- All requested functional refactors are in place.
- Benchmark-specific validation from section 6/11 is not yet executed in this run.

## Workstream Progress

## WS1: Decompose `CodeEditor` orchestration

Status: `completed` (target LOC reduction partially met)

Implemented:
- Added `EditorCommandRegistry` and moved command registration out of direct executor wiring flow:
  - `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/EditorCommandRegistry.java`
  - `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java`
- Added `OccurrenceSelectionService` and moved next/all-occurrence logic:
  - `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/OccurrenceSelectionService.java`
  - `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java`
- Added `EditorLifecycleService` and moved listener/input binding/unbinding:
  - `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/EditorLifecycleService.java`
  - `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java`
- Added `EditorSearchCoordinator` for search wiring ownership:
  - `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/EditorSearchCoordinator.java`

Notes:
- `CodeEditor` LOC reduced from 1286 to 1160.
- The "<900 LOC" stretch target was not fully reached in this iteration.

## WS2: Remove duplicate search scans

Status: `completed`

Implemented:
- `SearchController.replaceCurrent()` and `replaceAll()` no longer invoke extra `executeSearch()` after model replacement.
- Introduced shared `publishSearchState(...)` to centralize post-search UI updates.
- Search wiring centralized via `EditorSearchCoordinator`.

Files:
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/search/SearchController.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/EditorSearchCoordinator.java`

Validation:
- Added `CodeEditorSearchFlowTest` with a counting `SearchModel` to assert single search scan per replace action:
  - `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/api/CodeEditorSearchFlowTest.java`

## WS3: Viewport invalidation optimization

Status: `completed`

Implemented:
- Added `ViewportInvalidationPlanner` to compute bounded dirty ranges with visible-range intersection.
- `Viewport.onDocumentChanged(...)` now:
  - uses planner output,
  - applies full-redraw fallback for global/unsafe events,
  - skips dirty-line filling when change is outside the visible window.

Files:
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/ViewportInvalidationPlanner.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/Viewport.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/render/ViewportInvalidationPlannerTest.java`

## WS4: Document/line performance path improvements

Status: `completed`

Implemented:
- Extended `EditCommand` with optional incremental index hooks:
  - `applyLineIndex(LineIndex)`
  - `undoLineIndex(LineIndex)`
- Implemented hooks for:
  - `InsertEdit`
  - `DeleteEdit`
  - `ReplaceEdit`
  - `CompoundEdit`
- `Document.undo()` / `redo()` now attempt incremental index updates and fall back to full rebuild when needed.
- Added `Document.endsWithNewline()` and replaced full-text tail checks in `LineEditService`.
- `LineBlock.fromLines(...)` now uses `Document.getSubstring(...)` instead of `document.getText().substring(...)`.

Files:
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/document/EditCommand.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/document/InsertEdit.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/document/DeleteEdit.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/document/ReplaceEdit.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/document/CompoundEdit.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/document/Document.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/command/LineBlock.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/command/LineEditService.java`

Validation:
- Added/updated document tests:
  - `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/document/DocumentTest.java`

## WS5: Consolidate repeated navigation/occurrence helpers

Status: `completed`

Implemented:
- Consolidated word move/select commands into one parameterized helper (`WordDirection`).
- Consolidated delete-word behavior into one shared helper.
- Word-under-caret occurrence selection now reused via `OccurrenceSelectionService`.

File:
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java`

## Validation Executed

Compile:
- `mvn -pl papiflyfx-docking-code -am -DskipTests compile` -> `SUCCESS`

Targeted suites:
- `mvn -pl papiflyfx-docking-code -Dtest=DocumentTest,LineOperationsTest,ViewportInvalidationPlannerTest,CodeEditorSearchFlowTest -Dtestfx.headless=true -Dsurefire.failIfNoSpecifiedTests=false test` -> `SUCCESS`
- `mvn -pl papiflyfx-docking-code -Dtest=CodeEditorIntegrationTest,EditorInputControllerTest,EditorCommandExecutorTest,ViewportTest,SearchModelTest -Dtestfx.headless=true -Dsurefire.failIfNoSpecifiedTests=false test` -> `SUCCESS`

Full module validation:
- `mvn -pl papiflyfx-docking-code -am -Dtestfx.headless=true test` -> `SUCCESS` (`352` tests, `0` failures, `0` errors)

## Remaining Plan Item

- Benchmark-specific command from implementation plan has not yet been executed in this run:
  - `mvn -pl papiflyfx-docking-code -am -Dtestfx.headless=true -Dgroups=benchmark -Dsurefire.excludedGroups= test`

