# Docking Pane Detach Crash: Owner Stage Not Set

Date: 2026-02-22  
Status: Implemented and validated

## Reported error

When detaching a docking pane (float action), the app throws:

`java.lang.IllegalStateException: Owner stage not set. Call setOwnerStage() first.`

Stack trace points to:
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/DockManager.java:851`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/DockManager.java:402`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/core/DockTabGroup.java:225`

## Reproduction in samples

1. Run `SamplesApp`.
2. Open a sample that uses `DockManager` but is not `FloatingSample` (for example `TabGroupSample`).
3. Click the float/detach button in the tab header.
4. The exception above is thrown on the JavaFX Application Thread.

## Findings

1. `DockManager.floatLeaf(...)` throws by design if no floating manager exists:
   - `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/DockManager.java:850`
   - `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/DockManager.java:851`
2. Floating manager is created only by `setOwnerStage(Stage)`:
   - `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/DockManager.java:185`
   - `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/DockManager.java:187`
3. Samples already receive `ownerStage` in the sample contract:
   - `papiflyfx-docking-samples/src/main/java/org/metalib/papifly/fx/samples/SampleScene.java:26`
4. Only `FloatingSample` calls `dm.setOwnerStage(ownerStage)`:
   - `papiflyfx-docking-samples/src/main/java/org/metalib/papifly/fx/samples/docks/FloatingSample.java:37`
5. Other samples build `DockManager` without setting owner stage (examples):
   - `papiflyfx-docking-samples/src/main/java/org/metalib/papifly/fx/samples/docks/TabGroupSample.java:31`
   - `papiflyfx-docking-samples/src/main/java/org/metalib/papifly/fx/samples/docks/BasicSplitSample.java:30`
   - `papiflyfx-docking-samples/src/main/java/org/metalib/papifly/fx/samples/code/JavaEditorSample.java:64`

## Root cause

Sample wiring violates the `DockManager` floating contract:
- Float button is active in all tab groups.
- Most sample-created `DockManager` instances never call `setOwnerStage(...)`.
- Clicking float therefore reaches a guarded throw path in `DockManager`.

## Proposed fix

### 1. Required fix (samples)

Ensure every sample that creates `DockManager` sets owner stage immediately:

```java
DockManager dm = new DockManager();
dm.themeProperty().bind(themeProperty);
dm.setOwnerStage(ownerStage);
```

Apply to all sample builders that currently omit this call:
- `papiflyfx-docking-samples/src/main/java/org/metalib/papifly/fx/samples/docks/BasicSplitSample.java`
- `papiflyfx-docking-samples/src/main/java/org/metalib/papifly/fx/samples/docks/NestedSplitSample.java`
- `papiflyfx-docking-samples/src/main/java/org/metalib/papifly/fx/samples/docks/TabGroupSample.java`
- `papiflyfx-docking-samples/src/main/java/org/metalib/papifly/fx/samples/docks/MinimizeSample.java`
- `papiflyfx-docking-samples/src/main/java/org/metalib/papifly/fx/samples/docks/PersistSample.java`
- `papiflyfx-docking-samples/src/main/java/org/metalib/papifly/fx/samples/code/MarkdownEditorSample.java`
- `papiflyfx-docking-samples/src/main/java/org/metalib/papifly/fx/samples/code/JavaEditorSample.java`
- `papiflyfx-docking-samples/src/main/java/org/metalib/papifly/fx/samples/code/JavaScriptEditorSample.java`
- `papiflyfx-docking-samples/src/main/java/org/metalib/papifly/fx/samples/code/JsonEditorSample.java`

### 2. Recommended hardening (framework)

Make `DockManager` more defensive so this cannot crash host apps:

1. Add a private `ensureFloatingWindowManager(...)` that lazily resolves a `Stage` from:
   - explicitly set `ownerStage`, or
   - `rootPane.getScene().getWindow()` when attached and `instanceof Stage`.
2. Call this guard at the start of `floatLeaf(...)` methods.
3. If stage still cannot be resolved, skip float and log a warning instead of throwing an uncaught exception from UI callbacks.

This preserves API guidance while preventing runtime crashes from a common wiring omission.

## Validation plan

1. Manual: open each sample, click float button on a tab, verify no exception.
2. Automated: extend `papiflyfx-docking-samples/src/test/java/org/metalib/papifly/fx/samples/SamplesSmokeTest.java` with one interaction test that:
   - loads `TabGroupSample`,
   - triggers the float action on an active tab,
   - asserts no uncaught exception.

## Risk and impact

- Sample-only fix is low risk and directly resolves the reported issue.
- Framework hardening is medium risk but improves robustness for all consumers and reduces support/debug overhead.

## Implementation status (2026-02-22)

Implemented:

1. Sample wiring fix: all sample builders now call `dm.setOwnerStage(ownerStage)` right after creating `DockManager`.
2. Framework hardening in `DockManager`:
   - Added lazy owner-stage resolution from attached scene/window.
   - Removed uncaught `IllegalStateException` from floating paths when owner stage is unavailable.
   - Added restore-session fallback: if floating restore is unavailable, leaves are restored as docked content.
3. Test coverage:
   - Added `DockManagerFloatingStageResolutionFxTest` in docks module.
   - Added `tabGroupSampleFloatButtonDetachesWithoutException` in samples smoke tests (clicks real float button and asserts detached stage appears).

Validation:

1. `mvn -pl papiflyfx-docking-docks -Dtest=DockManagerFloatingStageResolutionFxTest -Dtestfx.headless=true test` -> PASS
2. `mvn -pl papiflyfx-docking-samples -Dtest=SamplesSmokeTest#tabGroupSampleFloatButtonDetachesWithoutException -Dtestfx.headless=true test` -> PASS
3. `mvn -pl papiflyfx-docking-docks -Dtestfx.headless=true test` -> PASS
4. `mvn -pl papiflyfx-docking-samples -Dtestfx.headless=true test` -> PASS
