# Docks — JUnit Tests Progress Report

Date: `2026-01-14`

Scope: `papiflyfx-docks` module.

This document tracks implementation progress against the plan in `spec/papiflyfx-docks/2026-01-14-junits/docks-junits.md`.

---

## Summary

An initial automated test suite is now in place for the docking framework:

- Maven test setup added (JUnit 5 + TestFX).
- A first set of **pure JVM unit tests** for `LayoutSerializer` was implemented.
- A first set of **JavaFX integration tests** (TestFX `ApplicationExtension`) was implemented for `DockManager`, `DockTabGroup`, and `LayoutFactory`.

Additionally, one small production hardening change was made to ensure deterministic error handling in `LayoutSerializer.deserialize(...)`.

---

## Implementation Status (high level)

| Area | Status | Notes |
|------|--------|-------|
| Tooling (`pom.xml`, surefire, JUnit 5/TestFX deps) | ✅ Complete | Headless is currently opt-in via `-Dtestfx.headless=true`. |
| Layer A: serializer unit tests | ✅ Initial coverage | DTO↔Map, JSON round-trip + escaping, error handling. |
| Layer B: JavaFX integration tests | ✅ Initial coverage | Capture/restore, ContentFactory restore, tab active-index behavior, close/dispose behavior. |
| Remaining matrix items from the plan | ⏳ Not started | Drag/hit-testing, floating/minimize/maximize flows, overlay rendering/theme, etc. |

---

## Files Modified

### Maven test setup

- `papiflyfx-docks/pom.xml`
  - Added JUnit Jupiter (`junit-jupiter-api`, `junit-jupiter-engine`) and TestFX (`testfx-junit5`, `testfx-core`) as test-scoped dependencies.
  - Added `maven-surefire-plugin` configuration.
  - Introduced opt-in headless toggle via `-Dtestfx.headless=true`.

### Production hardening

- `papiflyfx-docks/src/main/java/org/metalib/papifly/fx/docks/serial/LayoutSerializer.java`
  - `deserialize(Map<String,Object>)` now throws `IllegalArgumentException("Missing type")` when the `type` key is absent (instead of failing with `NullPointerException`).

---

## Test Suite Added

All tests were added under `papiflyfx-docks/src/test/java`:

### Layer A — Pure unit tests

- `papiflyfx-docks/src/test/java/org/metalib/papifly/fx/docks/serial/LayoutSerializerTest.java`
  - DTO ↔ Map round-trips for `LeafData`, `SplitData`, `TabGroupData`.
  - JSON round-trip (`toJson`/`fromJson`) and escaping behavior.
  - Robustness: unknown `type` and missing `type` error handling.

### Layer B — JavaFX integration tests

- `papiflyfx-docks/src/test/java/org/metalib/papifly/fx/docks/layout/LayoutFactoryFxTest.java`
  - Verifies `ContentFactory` is applied when restoring a `LeafData` with `contentFactoryId`.

- `papiflyfx-docks/src/test/java/org/metalib/papifly/fx/docks/DockManagerCaptureRestoreFxTest.java`
  - Verifies `DockManager.restore(layout)` followed by `capture()` yields a structurally equivalent layout.

- `papiflyfx-docks/src/test/java/org/metalib/papifly/fx/docks/core/DockTabGroupFxTest.java`
  - Verifies active-tab index behavior when removing tabs.

- `papiflyfx-docks/src/test/java/org/metalib/papifly/fx/docks/DockManagerCloseLeafFxTest.java`
  - Verifies closing the only leaf clears the root and disposes leaf content.

### Test utilities

- `papiflyfx-docks/src/test/java/org/metalib/papifly/fx/docks/testutil/FxTestUtil.java`
  - Provides `runFx(...)` / `callFx(...)` helpers to execute code on the JavaFX thread and wait for completion.

---

## Verification

Executed:

```bash
./mvnw -pl papiflyfx-docks test
```

Result:

- ✅ All tests passed (11 tests).

---

## Notes / Deviations

- The original plan mentioned Monocle for headless CI. In this iteration, Monocle was not added yet; the current setup keeps headless mode opt-in via `-Dtestfx.headless=true`.
- While implementing the test setup, an attempted TestFX version (`4.0.16-alpha`) was not resolvable; the module was moved to TestFX `4.0.18`.

---

## Next Steps (from the plan)

Suggested next targets to expand coverage (in order of impact):

1. `DockManager` tree mutation edge cases:
   - rebalancing split/tab groups on remove/close in more complex trees
2. Drag/hit-testing:
   - deterministic `HitTester` results given controlled bounds/layout
3. Floating/minimize/maximize:
   - state transitions and restore hints
4. Headless CI readiness:
   - add Monocle and the full set of recommended system properties
