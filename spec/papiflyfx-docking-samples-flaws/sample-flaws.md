# Docking Samples Flaws Investigation

Date: 2026-02-22

## Scope

Investigated the issues listed in `spec/papiflyfx-docking-samples-flaws/README.md`:

1. Docking samples: drag target appears shifted.
2. Code editor samples: double-click word selection is shifted.
3. Code editor samples: rectangular (box) selection is shifted.

## Findings

### 1) Docking drag hint is rendered in the wrong coordinate space

**Root cause**

- Drag hit testing is done in **scene coordinates**:
  - `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/drag/HitTester.java:43`
  - `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/drag/HitTester.java:53`
  - `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/drag/HitTester.java:76`
- `DragManager` forwards that result directly to the overlay:
  - `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/drag/DragManager.java:107`
  - `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/drag/DragManager.java:129`
- `OverlayCanvas` draws `zoneBounds` and `tabInsertX` as if they were its own local coordinates:
  - `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/render/OverlayCanvas.java:117`
  - `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/render/OverlayCanvas.java:136`
  - `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/render/OverlayCanvas.java:201`
- In samples, docking content is embedded in a `BorderPane` center with an external top bar and left sample list:
  - `papiflyfx-docking-samples/src/main/java/org/metalib/papifly/fx/samples/SamplesApp.java:45`
  - `papiflyfx-docking-samples/src/main/java/org/metalib/papifly/fx/samples/SamplesApp.java:46`
  - `papiflyfx-docking-samples/src/main/java/org/metalib/papifly/fx/samples/SamplesApp.java:47`

**Impact**

- Drag hint/target visuals are shifted by layout offsets (left list width, top bar height).
- Hit testing itself is mostly correct, but visual feedback is wrong, so target appears incorrect.

### 2) Code editor pointer mapping uses editor-root coordinates instead of viewport coordinates

**Root cause**

- Mouse handlers are attached to `CodeEditor` root:
  - `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/EditorLifecycleService.java:33`
  - `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java:250`
- Pointer logic feeds `event.getX()/getY()` directly into viewport mapping:
  - `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/EditorPointerController.java:75`
  - `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/EditorPointerController.java:79`
  - `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/EditorPointerController.java:114`
  - `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/EditorPointerController.java:118`
- `Viewport.getColumnAtX` expects x relative to the text viewport origin:
  - `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/Viewport.java:357`
- `CodeEditor` layout includes gutter to the left of viewport, so editor-root x includes gutter width:
  - `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java:193`
  - `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java:194`

**Impact**

- Double-click selects a word at a shifted column.
- Box selection rectangle is shifted horizontally by gutter/offset amount.

### 3) Existing tests miss these coordinate-space bugs

**Evidence**

- `MouseGestureTest` synthesizes events on `editor` with raw x/y that assume text starts at x=0 in editor coordinates:
  - `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/api/MouseGestureTest.java:288`
  - `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/api/MouseGestureTest.java:291`
- Dock drag tests mount docking root directly as scene root (origin), so no external offset is exercised:
  - `papiflyfx-docking-docks/src/test/java/org/metalib/papifly/fx/docks/drag/DragManagerSplitMergeFxTest.java:45`

**Validation run**

- `mvn -pl papiflyfx-docking-code -Dtest=MouseGestureTest -Dtestfx.headless=true test` -> PASS
- `mvn -pl papiflyfx-docking-docks -Dtest=DragManagerSplitMergeFxTest -Dtestfx.headless=true test` -> PASS

This confirms current coverage does not detect the sample-reported behavior.

## Suggested Fix

### A) Docks: normalize drag hint geometry to overlay local coordinates

Recommended minimal change:

1. Keep `HitTester` in scene space (it is already coherent).
2. Before drawing, convert `HitTestResult.zoneBounds` and `tabInsertX` from scene space to overlay local space.
3. Perform conversion in `OverlayCanvas.showDropHint(...)` (or in `DragManager` before calling overlay), consistently for all zones.

Implementation note:

- Use `sceneToLocal(minX, minY)` / `sceneToLocal(maxX, maxY)` to build local bounds.
- Convert `tabInsertX` similarly.
- Guard against null scene or non-finite coordinates.

### B) Code editor: normalize mouse points to viewport local coordinates

Recommended minimal change:

1. In `EditorPointerController`, convert pointer position via `viewport.sceneToLocal(event.getSceneX(), event.getSceneY())`.
2. Use the converted local coordinates for `getLineAtY`/`getColumnAtX` in both press and drag paths.
3. Keep current interaction semantics (double-click, box selection, multi-caret) unchanged.

This removes dependence on editor container offsets, gutter width, and surrounding sample layout.

## Regression Tests To Add

1. **Docks offset container test**
   - Place `DockManager.getRootPane()` in `BorderPane.center` with dummy top/left panes.
   - Drag and assert overlay hint aligns with target in local space (not shifted by top/left offsets).

2. **Editor double-click coordinate test**
   - Compute a click point in viewport-local text coordinates.
   - Transform to scene, then to editor event coordinates (real path).
   - Assert selected word is exact (no column shift).

3. **Editor box-selection coordinate test**
   - Same coordinate path as above for start/end drag points.
   - Assert selected columns match expected rectangle.

## Conclusion

The reported issues are real and share the same category: **mixing coordinate spaces without explicit normalization**.  
Applying the two focused fixes above should resolve all three sample flaws with minimal behavior risk.
