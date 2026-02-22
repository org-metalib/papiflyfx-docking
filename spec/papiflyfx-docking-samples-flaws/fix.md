# Implementation Plan: Fix Sample Coordinate Shift Bugs

Date: 2026-02-22
Status: Proposed
Owner: papiflyfx-docking maintainers

## Goal

Fix all three reported sample flaws by normalizing coordinate spaces correctly:

1. Docking drag target hint is shifted.
2. Code editor double-click word selection is shifted.
3. Code editor rectangular selection is shifted.

## Non-Goals

- Do not redesign drag/drop behavior.
- Do not change editor gesture semantics.
- Do not change sample layout (`SamplesApp`) to work around framework bugs.

## Root Cause Summary

1. Docks module mixes scene-space hit-test geometry with overlay-local rendering.
2. Code editor module uses editor-root mouse x/y directly for viewport-local hit mapping.

Reference: `spec/papiflyfx-docking-samples-flaws/sample-flaws.md`.

## Implementation Strategy

Two focused changes, each local to the owning module:

1. Docks fix in overlay rendering pipeline.
2. Code editor fix in pointer-coordinate mapping.

Then add regression tests that include non-zero layout offsets.

## Detailed Work Plan

### Phase 1: Docks coordinate normalization

#### 1.1 Update overlay API to accept scene-space hit result and render in local-space

Target file:
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/render/OverlayCanvas.java`

Planned changes:

1. Keep current public `showDropHint(HitTestResult result)` signature.
2. Convert `result.zoneBounds()` from scene to overlay-local before storing/drawing.
3. Convert `result.tabInsertX()` from scene x to overlay-local x.
4. Store a normalized `HitTestResult` (local bounds/local insert x) in `currentHitResult`.
5. Add a private helper for bounds conversion with guard rails.
6. If conversion is impossible (null scene, non-finite values), clear hint safely.

Implementation details:

1. Compute local corners using `sceneToLocal(minX, minY)` and `sceneToLocal(maxX, maxY)`.
2. Build normalized bounds from min/max of converted corners.
3. For insert line x, convert point `(sceneX, tabBarSceneMinY)` and use resulting local x.
4. Preserve `element`, `zone`, `targetBounds`, `tabInsertIndex`.
5. Do not alter drawing code paths besides consuming normalized data.

Why here:

1. `HitTester` is intentionally scene-based and currently coherent.
2. `OverlayCanvas` owns rendering coordinate space and is the narrowest safe conversion boundary.

#### 1.2 Keep drag/hit testing unchanged

Target files:
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/drag/DragManager.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/drag/HitTester.java`

Planned change:

1. No behavior change expected.
2. Only minor comments may be added if needed to document coordinate contract.

### Phase 2: Code editor coordinate normalization

#### 2.1 Map mouse event coordinates into viewport-local space before hit mapping

Target file:
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/EditorPointerController.java`

Planned changes:

1. Add helper method to compute viewport-local pointer point from `MouseEvent`.
2. In `handleMousePressed`, use viewport-local x/y for `getLineAtY` and `getColumnAtX`.
3. In `handleMouseDragged`, use viewport-local x/y for the same mapping.
4. Keep click-count, modifier, and selection logic unchanged.

Implementation details:

1. Use scene coordinates from event (`event.getSceneX()`, `event.getSceneY()`).
2. Convert with `viewport.sceneToLocal(sceneX, sceneY)`.
3. Clamp or early-return on invalid numeric results.
4. Keep `handleMouseReleased` and `handleScroll` logic unchanged.

Why this is safe:

1. All downstream APIs (`Viewport.getLineAtY/getColumnAtX`) already assume viewport-local coordinates.
2. Only coordinate frame is corrected; selection behavior remains as designed.

#### 2.2 Keep input wiring unchanged

Target files:
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/EditorLifecycleService.java`

Planned change:

1. No handler re-binding required.
2. Optional comment clarifying that handlers are attached to `CodeEditor`, but coordinates are normalized in `EditorPointerController`.

### Phase 3: Regression tests

#### 3.1 Add docks offset-container regression

Preferred target:
- `papiflyfx-docking-docks/src/test/java/org/metalib/papifly/fx/docks/drag/DragManagerSplitMergeFxTest.java`

Alternative target:
- New class `papiflyfx-docking-docks/src/test/java/org/metalib/papifly/fx/docks/drag/DragOverlayCoordinateFxTest.java`

Test scenario:

1. Build scene with `BorderPane` and non-zero `top` + `left` regions.
2. Put `dockManager.getRootPane()` in `center`.
3. Perform drag using existing test utilities.
4. Assert drag outcome is correct and no shifted targeting behavior occurs.

Recommended assertion style:

1. Assert structural drop result (tab moved/split in expected group).
2. If practical, introspect overlay-local hint bounds against expected local region during drag.

#### 3.2 Add editor double-click with true coordinate path

Target file:
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/api/MouseGestureTest.java`

Test scenario:

1. Place editor in container with left/top offsets.
2. Compute intended click point in viewport-local coordinates.
3. Convert viewport-local -> scene -> editor-local for event construction.
4. Fire double-click and assert exact selected word.

#### 3.3 Add editor box selection with true coordinate path

Target file:
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/api/MouseGestureTest.java`

Test scenario:

1. Same offset-container setup.
2. Build drag start/end from viewport-local points transformed through scene/editor frames.
3. Fire press/drag/release.
4. Assert selected columns and carets match expected rectangular block.

### Phase 4: Validation and cleanup

Run targeted tests:

1. `mvn -pl papiflyfx-docking-docks -Dtest=DragManagerSplitMergeFxTest -Dtestfx.headless=true test`
2. `mvn -pl papiflyfx-docking-code -Dtest=MouseGestureTest -Dtestfx.headless=true test`

Run module suites:

1. `mvn -pl papiflyfx-docking-docks -Dtestfx.headless=true test`
2. `mvn -pl papiflyfx-docking-code -Dtestfx.headless=true test`
3. `mvn -pl papiflyfx-docking-samples -Dtestfx.headless=true test`

Manual smoke pass:

1. Run samples app.
2. Reproduce the three original scenarios.
3. Confirm no visual shift remains.

## Acceptance Criteria

1. Drag hint aligns with actual drop region in docking samples even with sample list and top bar present.
2. Double-click selects the intended word with no horizontal offset in code samples.
3. Box selection starts and ends at intended columns with no offset in code samples.
4. Added regression tests fail before fix and pass after fix.
5. No regressions in existing drag/selection behavior.

## Risks and Mitigations

Risk:
- Incorrect scene/local conversion could over-correct or under-correct.

Mitigation:
- Convert at a single boundary per subsystem.
- Add tests with explicit coordinate transforms and non-zero offsets.

Risk:
- Overlay conversion may break tab insertion indicator line.

Mitigation:
- Include TAB_BAR drag scenario in docks regression.
- Verify `tabInsertX` conversion specifically.

Risk:
- Editor conversion might impact gestures on overlays.

Mitigation:
- Keep current focus/overlay gating logic unchanged.
- Restrict change to pointer-to-viewport mapping only.

## Sequencing Recommendation

1. Implement editor fix first (smaller scope, faster feedback).
2. Add editor regression tests.
3. Implement docks overlay fix.
4. Add docks regression test.
5. Run targeted tests, then full module tests.

## Deliverables

1. Code changes in:
   - `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/EditorPointerController.java`
   - `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/render/OverlayCanvas.java`
2. Test updates/additions in:
   - `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/api/MouseGestureTest.java`
   - `papiflyfx-docking-docks/src/test/java/org/metalib/papifly/fx/docks/drag/DragManagerSplitMergeFxTest.java` or a new dedicated FX test class
3. Brief implementation report update in:
   - `spec/papiflyfx-docking-samples-flaws/sample-flaws.md` (status note after completion)
