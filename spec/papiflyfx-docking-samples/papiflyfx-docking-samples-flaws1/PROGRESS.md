# Progress Report: Detach Crash Fix

Date: 2026-02-22  
Scope: Sample fix + framework hardening + regression coverage

## Summary

The detach crash (`Owner stage not set`) is resolved in samples and hardened in the docking framework.  
Regression tests were added and all targeted/full module tests passed in headless mode.

## Completed work

1. Investigated crash path from stack trace into `DockManager.floatLeaf(...)`.
2. Confirmed root cause: most samples did not call `setOwnerStage(ownerStage)` even though the stage is provided.
3. Applied sample fix:
   - Added `dm.setOwnerStage(ownerStage)` to all sample builders using `DockManager`.
4. Applied framework hardening in `DockManager`:
   - Lazy owner-stage resolution from `rootPane.getScene().getWindow()`.
   - Float operations now fail safely with warning instead of throwing uncaught `IllegalStateException`.
   - Floating-session restore now falls back to docked restore when floating is unavailable.
5. Added regression tests:
   - `papiflyfx-docking-docks/src/test/java/org/metalib/papifly/fx/docks/DockManagerFloatingStageResolutionFxTest.java`
   - `papiflyfx-docking-samples/src/test/java/org/metalib/papifly/fx/samples/SamplesSmokeTest.java`
     (`tabGroupSampleFloatButtonDetachesWithoutException`)
6. Stabilized sample detach assertion for headless runners:
   - The test now verifies detach by observing dock-tab count decrease in the main scene after float click.
   - This avoids false negatives from environment-specific floating `Stage` visibility/enumeration behavior.

## Validation

All commands executed with `sdk use java 25.0.1.fx-zulu`:

1. `mvn -pl papiflyfx-docking-docks -Dtest=DockManagerFloatingStageResolutionFxTest -Dtestfx.headless=true test` -> PASS
2. `mvn -pl papiflyfx-docking-samples -Dtest=SamplesSmokeTest#tabGroupSampleFloatButtonDetachesWithoutException -Dtestfx.headless=true test` -> PASS
3. `mvn -pl papiflyfx-docking-docks -Dtestfx.headless=true test` -> PASS (51 tests)
4. `mvn -pl papiflyfx-docking-samples -Dtestfx.headless=true test` -> PASS (3 tests)

## Notes

- Build still reports pre-existing Maven model warnings about duplicate `maven-release-plugin` declarations in parent `pom.xml`.
- These warnings are unrelated to this fix and did not block validation.
