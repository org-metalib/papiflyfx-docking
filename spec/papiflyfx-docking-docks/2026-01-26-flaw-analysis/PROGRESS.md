# Progress Report

Date: 2026-01-26

## Completed
- Implemented safe split collapse reparenting and disposal in `DockManager` and drag cleanup to avoid stale parents and leaks.
- Wired close handlers for leaves restored from session data (floating and minimized).
- Added regression tests for split collapse on close and restored floating leaf close behavior.
- Implemented a self-drop guard for single-tab splits and added a no-op drag regression test.
- Attempted `./mvnw test -pl papiflyfx-docks` (timed out during JavaFX init) and `./mvnw -Dtestfx.headless=true test -pl papiflyfx-docks` (failed: Monocle headless platform missing).
- Added Maven flags for display vs headless TestFX runs: default display, headless via `-Dtestfx.headless=true` (Monocle/Prism properties applied).
- Updated Monocle/TestFX headless setup (Monocle 21.0.2 + module exports), and `./mvnw -Dtestfx.headless=true test -pl papiflyfx-docks` now passes.

## In Progress
- None.

## Next
- Configure Monocle (or run with a display) to execute TestFX UI tests.
- Optional: add `--enable-native-access=javafx.graphics` to suppress the JavaFX native access warning.
