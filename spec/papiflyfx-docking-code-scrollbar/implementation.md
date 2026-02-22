# PapiflyFX Code Scrollbar + Wrap Implementation Plan

Date: 2026-02-22  
Scope: implement `spec/papiflyfx-docking-code-scrollbar/design.md` in module `papiflyfx-docking-code`.

## 1. Objectives

- Add canvas-rendered vertical and horizontal scrollbars.
- Add soft wrap (`wordWrap`) with wrap-aware rendering/hit-testing.
- Keep model coordinates logical (no document mutation for wrapping).
- Enforce behavior gates:
  - `wordWrap=true` hides horizontal scrollbar.
  - `wordWrap=true` forces `horizontalScrollOffset=0.0`.
  - `wordWrap=true` disables box selection gestures.
- Preserve existing editor behavior (search, go-to-line, multi-caret, theme switching, persistence compatibility).

## 2. Baseline (Current Code)

Current implementation status in `papiflyfx-docking-code`:

- Vertical scroll only:
  - `CodeEditor.verticalScrollOffset` exists.
  - `Viewport` has only `scrollOffset` (vertical).
  - `EditorPointerController.handleScroll` consumes only `deltaY`.
- No horizontal offset/state:
  - No `horizontalScrollOffset` property on `CodeEditor`, `Viewport`, or persistence DTO/codec.
- No word wrap:
  - `RenderLine` is 1:1 with logical document lines.
  - `Viewport.getLineAtY(...)` + `getColumnAtX(...)` assume unwrapped lines.
- No scrollbar rendering:
  - Render passes currently: `BackgroundPass`, `SearchPass`, `SelectionPass`, `TextPass`, `CaretPass`.
- Box selection always available:
  - `Shift+Alt+Drag` and middle mouse triggers rectangular selection in `EditorPointerController`.
- Persistence contract is v2:
  - `EditorStateData` and `CodeEditorStateAdapter.VERSION = 2`.

## 3. Implementation Strategy

Execution order is intentionally incremental to keep behavior testable at each stage:

1. Add horizontal scroll data model and rendering offsets first (no wrap yet).
2. Add canvas scrollbar rendering and interaction.
3. Add `wordWrap` flag behavior gates.
4. Add full soft-wrap rendering model (`WrapMap`) and wrap-aware geometry.
5. Migrate persistence to v3.
6. Harden performance and regressions.

This order avoids coupling wrap complexity with initial scrollbar bring-up.

## 4. Phase Plan

## Phase 0: Safety Baseline

Goal: establish regression guardrails before changing core rendering/input paths.

Tasks:
- [ ] Run baseline compile/tests and record counts:
  - `mvn compile -pl papiflyfx-docking-code -am`
  - `mvn -pl papiflyfx-docking-code -Dtestfx.headless=true test`
- [ ] Record benchmark baseline from `CodeEditorBenchmarkTest` (scroll-related scenarios).
- [ ] Add TODO markers in this plan with a checkoff section for each phase.

Exit criteria:
- [ ] Baseline is green and captured in PR notes.

## Phase 1: Horizontal Scroll State + X-Offset Rendering (No Wrap)

Goal: introduce horizontal scrolling pipeline without changing line layout model.

Primary files:
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/Viewport.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/RenderContext.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/EditorPointerController.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/EditorCaretCoordinator.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/TextPass.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/SelectionPass.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/SearchPass.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/CaretPass.java`

Tasks:
- [ ] Add `horizontalScrollOffset` property to `CodeEditor` (default `0.0`).
- [ ] Add matching state to `Viewport` with clamping via `computeMaxHorizontalScrollOffset()`.
- [ ] Extend `RenderContext` with `horizontalScrollOffset`.
- [ ] Subtract horizontal offset from all x-coordinate paint paths in render passes.
- [ ] Update hit-testing to account for horizontal offset (unwrapped mode).
- [ ] Update wheel/trackpad handling:
  - `deltaX` updates horizontal offset.
  - `Shift+wheel` maps vertical wheel to horizontal when wrap is off.
- [ ] Add horizontal caret-visibility helper (`ensureCaretVisibleHorizontally`) and invoke on caret moves/edits.

Tests:
- [ ] Update `render/ViewportTest` for x-offset hit-testing and clamping.
- [ ] Update `api/CodeEditorIntegrationTest` to verify horizontal panning via wheel/trackpad.
- [ ] Add/extend tests for caret horizontal visibility after long-line navigation.

Exit criteria:
- [ ] Horizontal scrolling works in unwrapped mode.
- [ ] Existing vertical behavior remains unchanged.

## Phase 2: Canvas Scrollbar Rendering + Effective Text Area

Goal: draw vertical/horizontal scrollbars as overlay render pass, and reserve effective text area.

Primary files:
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/ScrollbarPass.java` (new)
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/Viewport.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/RenderContext.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/theme/CodeEditorTheme.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/theme/CodeEditorThemeMapper.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java`

Tasks:
- [ ] Add scrollbar constants and geometry computation in `Viewport`:
  - `SCROLLBAR_WIDTH`, `SCROLLBAR_THUMB_PAD`, `MIN_THUMB_SIZE`, `SCROLLBAR_RADIUS`.
- [ ] Add effective area calculations:
  - `effectiveTextWidth`
  - `effectiveTextHeight`
- [ ] Rebase visible range/max offset math to effective dimensions.
- [ ] Add `ScrollbarPass` as the last pass in render pipeline.
- [ ] Extend `CodeEditorTheme` with scrollbar paints:
  - track, thumb, thumb-hover, thumb-active.
- [ ] Map new colors in `CodeEditorThemeMapper`.
- [ ] Expose vertical/horizontal scrollbar visibility and geometry getters from `Viewport`.
- [ ] Increase search/go-to-line top-right margin when vertical scrollbar is visible.

Tests:
- [ ] Add `render/ScrollbarPassTest` for geometry, min-thumb behavior, and visibility rules.
- [ ] Update `theme/CodeEditorThemeMapperTest` for new fields.
- [ ] Update `api/CodeEditorIntegrationTest` overlay margin assertions.

Exit criteria:
- [ ] Scrollbars render correctly and track current offsets.
- [ ] Text does not paint under scrollbars.

## Phase 3: Scrollbar Mouse Interaction + Event Consumption

Goal: support drag, track-click jump, and hover/active visuals through raw mouse events.

Primary files:
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/EditorPointerController.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/Viewport.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/RenderContext.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/ScrollbarPass.java`

Tasks:
- [ ] Add scrollbar hover/drag state model (`NONE`, `VERTICAL_THUMB`, `HORIZONTAL_THUMB`, etc.).
- [ ] Detect scrollbar hit regions before text selection handling.
- [ ] Consume press/drag events when scrollbar interaction starts.
- [ ] Implement:
  - track click jump,
  - thumb drag proportional scrolling,
  - hover color transitions.
- [ ] Keep wheel behavior consistent when pointer is over scrollbars.

Tests:
- [ ] Add integration coverage in `api/CodeEditorIntegrationTest` for drag and track-click.
- [ ] Add pointer regression tests in `api/MouseGestureTest` to ensure scrollbar clicks do not move caret.

Exit criteria:
- [ ] Scrollbar interactions are functional and isolated from text selection.

## Phase 4: `wordWrap` Flag + Behavior Gates (Without Full Wrap Yet)

Goal: introduce the wrap property and enforce mode gates immediately.

Primary files:
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/EditorPointerController.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/Viewport.java`

Tasks:
- [ ] Add `BooleanProperty wordWrap` to `CodeEditor` (`default=false`).
- [ ] Wire `wordWrap` into `Viewport` render state.
- [ ] Enforce mode rules:
  - `wordWrap=true` => horizontal scrollbar hidden.
  - `wordWrap=true` => horizontal offset forced to `0.0`.
  - Horizontal offset setter becomes no-op except normalization in wrap mode.
- [ ] Gate box selection gestures in `EditorPointerController` using `wordWrapSupplier`.
- [ ] Guard active box selection on drag if wrap toggles on during interaction.

Tests:
- [ ] Update `api/MouseGestureTest` to assert box selection disabled in wrap mode.
- [ ] Update `api/CodeEditorIntegrationTest` for horizontal reset/hide behavior.

Exit criteria:
- [ ] Wrap mode policy is correct before visual wrapping is introduced.

## Phase 5: Soft Wrap Core (`WrapMap`) + Wrap-Aware Rendering

Goal: render logical lines as visual rows using a dedicated wrap mapping model.

Primary files:
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/WrapMap.java` (new)
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/RenderLine.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/RenderContext.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/Viewport.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/BackgroundPass.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/TextPass.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/SelectionPass.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/SearchPass.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/CaretPass.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/SelectionGeometry.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/gutter/GutterView.java`

Tasks:
- [ ] Add `WrapMap` with prefix-sum indexing:
  - full rebuild on document/width/font changes,
  - line-to-visual and visual-to-line mapping,
  - `VisualRow(lineIndex,startColumn,endColumn)`.
- [ ] Extend `RenderLine` with `startColumn`/`endColumn`.
- [ ] Refactor `Viewport.buildRenderLines()` to produce visual rows in wrap mode.
- [ ] Add wrap-aware unified hit-testing method:
  - `HitPosition getHitPosition(double x, double y)`.
- [ ] Update all passes to use visual-row slices (selection/search/caret splitting across wrapped rows).
- [ ] Update current-line highlight logic for multiple visual rows of a single logical line.
- [ ] Update `GutterView`:
  - line numbers only on first visual row of each logical line,
  - continuation row marker (optional lightweight indicator).

Tests:
- [ ] Add `render/WrapMapTest` for mapping correctness and rebuild triggers.
- [ ] Update `render/RenderLineTest` for slice semantics.
- [ ] Expand `render/ViewportTest` with wrapped visible range, hit-testing, caret/selection/search splits.
- [ ] Update `gutter/GutterViewTest` for first-row-only numbering in wrap mode.

Exit criteria:
- [ ] Wrapped rendering works without document mutation.
- [ ] Selection, caret, and search remain correct across row boundaries.

## Phase 6: Navigation + Caret Visibility in Wrap Mode

Goal: make caret movement and page navigation reliable with visual rows.

Primary files:
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/EditorCaretCoordinator.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/EditorNavigationController.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/Viewport.java`

Tasks:
- [ ] Make `ensureCaretVisible()` wrap-aware via visual-row lookup.
- [ ] Update page move delta in wrap mode to count visual rows.
- [ ] Keep Up/Down/Home/End behavior at phase-1 contract from design (logical-line semantics) unless upgraded in a follow-up.
- [ ] Ensure horizontal visibility helper is skipped in wrap mode.

Tests:
- [ ] Add integration coverage for page up/down in wrap mode.
- [ ] Add caret visibility regression tests for deep wrapped lines.

Exit criteria:
- [ ] Caret remains visible and navigation is deterministic in both modes.

## Phase 7: Persistence v3 + Migration

Goal: persist `wordWrap` and `horizontalScrollOffset` while keeping v2 compatibility.

Primary files:
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/state/EditorStateData.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/state/EditorStateCodec.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/EditorStateCoordinator.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditorStateAdapter.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java`

Tasks:
- [ ] Extend state DTO with:
  - `horizontalScrollOffset` (default `0.0`)
  - `wordWrap` (default `false`)
- [ ] Extend codec map keys and tolerant decode defaults.
- [ ] Bump adapter version from `2` to `3`.
- [ ] Implement migration path:
  - v2 -> v3 defaults (`horizontal=0.0`, `wordWrap=false`).
- [ ] Apply state restore order:
  - apply `wordWrap` first,
  - then restore vertical/horizontal scroll offsets after layout stabilization.

Tests:
- [ ] Extend `state/EditorStateCodecTest` for v3 round-trip and v2 migration.
- [ ] Extend `api/CodeEditorIntegrationTest` for persistence restore behavior.
- [ ] Extend `api/CodeEditorDockingIntegrationTest` for session-level v3 restore.

Exit criteria:
- [ ] v3 persistence works and v2 payloads remain backward-compatible.

## Phase 8: Hardening + Performance Follow-ups

Goal: lock in behavior and reduce long-file overhead.

Tasks:
- [ ] Add incremental `WrapMap.update()` path for bounded line edits.
- [ ] Add incremental longest-line tracking for horizontal max offset.
- [ ] Add optional scrollbar auto-fade (if retained from design phase-5 backlog).
- [ ] Run benchmark comparisons (`CodeEditorBenchmarkTest`) for:
  - wrap off scrolling,
  - wrap on long-line scrolling.

Tests:
- [ ] Add regression tests around mode toggling with multi-carets and active selections.
- [ ] Verify no leaks/disposal regressions for new listeners/state.

Exit criteria:
- [ ] Performance is acceptable and no critical regressions remain.

## 5. File Change Matrix

## New files

- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/WrapMap.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/ScrollbarPass.java`

## Modified files (expected)

- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/EditorPointerController.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/EditorCaretCoordinator.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/EditorNavigationController.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/EditorStateCoordinator.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditorStateAdapter.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/Viewport.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/RenderContext.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/RenderLine.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/BackgroundPass.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/TextPass.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/SelectionPass.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/SearchPass.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/CaretPass.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/SelectionGeometry.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/gutter/GutterView.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/theme/CodeEditorTheme.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/theme/CodeEditorThemeMapper.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/state/EditorStateData.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/state/EditorStateCodec.java`

## 6. Test Matrix

New tests:
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/render/WrapMapTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/render/ScrollbarPassTest.java`

Existing test suites to extend:
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/render/ViewportTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/gutter/GutterViewTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/state/EditorStateCodecTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/theme/CodeEditorThemeMapperTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/api/CodeEditorIntegrationTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/api/MouseGestureTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/api/CodeEditorDockingIntegrationTest.java`

Validation commands:
- `mvn compile -pl papiflyfx-docking-code -am`
- `mvn test -pl papiflyfx-docking-code`
- `mvn -Dtestfx.headless=true test -pl papiflyfx-docking-code`

## 7. Risks and Mitigations

- Wrap-aware geometry regressions (selection/search/caret).
  - Mitigation: add focused unit tests per pass and wrap hit-test cases before refactors are merged.
- Scrollbar interaction conflicts with editor gestures.
  - Mitigation: process scrollbar hit regions before caret/selection logic and consume events.
- Performance regressions from full wrap rebuild.
  - Mitigation: ship correct full rebuild first, then incremental updates in hardening phase.
- Persistence migration errors.
  - Mitigation: keep version-gated decode methods and explicit v2->v3 migration tests.

## 8. Recommended PR Slices

1. PR-1: Phase 1 (horizontal offset model + render x-offset).
2. PR-2: Phases 2-3 (scrollbar rendering + interaction).
3. PR-3: Phase 4 (wordWrap behavior gates).
4. PR-4: Phases 5-6 (WrapMap + wrap-aware rendering/navigation).
5. PR-5: Phases 7-8 (persistence v3 + hardening/perf).

Each PR should remain buildable, testable, and backward-compatible relative to the previous merge state.
