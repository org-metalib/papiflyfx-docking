# Docks — JUnit Test Implementation Plan

Date: `2026-01-14`

Scope: `papiflyfx-docks` module.

This document proposes a practical, incremental plan to add automated tests for the docking framework. The framework is JavaFX-heavy, so the plan explicitly separates **pure JVM unit tests** (fast, deterministic) from **headless JavaFX integration tests** (minimal UI surface, still automated).

---

## 1) Current baseline

### 1.1 Code areas (what we will cover)

From `papiflyfx-docks/src/main/java/org/metalib/papifly/fx/docks/`:

- **Core model/UI composition** (`docks/core/`): `DockElement`, `DockLeaf`, `DockSplitGroup`, `DockTabGroup`, `DockPane`, `DockData`, `DockState`.
- **Orchestration**: `DockManager` (create/close/remove/capture/restore + float/dock/minimize/maximize + drag wiring).
- **Layout & persistence DTOs** (`docks/layout/`): `LayoutNode`, `LeafData`, `SplitData`, `TabGroupData`, `LayoutFactory`, `ContentFactory`.
- **Serialization** (`docks/serial/`): `LayoutSerializer` (map ↔ JSON ↔ layout DTOs).
- **Drag/drop logic** (`docks/drag/`): `DragManager`, `HitTester`, `HitTestResult`, `DropZone`.
- **Floating & minimize** (`docks/floating/`, `docks/minimize/`): `FloatingDockWindow`, `FloatingWindowManager`, `MinimizedStore`, `MinimizedBar`, `RestoreHint`.
- **Rendering overlay** (`docks/render/`): `OverlayCanvas`.
- **Theme** (`docks/theme/`): `Theme`.

### 1.2 Testing baseline

- No existing unit tests found in the repository.
- `papiflyfx-docks/pom.xml` currently has no test dependencies (only `javafx-controls`).

---

## 2) Goals & non-goals

### Goals

1. Provide **fast feedback** on correctness of:
   - model/tree invariants
   - layout capture/restore
   - serialization round-trips and edge cases
   - drag/hit-testing decisions
   - state transitions: dock ↔ float ↔ minimize, and maximize/restore
2. Keep the suite **deterministic** and **headless-friendly**.
3. Create a test structure that encourages **small, focused tests** and isolates UI-dependent parts.

### Non-goals (initial iteration)

- Pixel-perfect UI rendering tests.
- End-to-end “user drags with mouse on real window” tests.
- Performance benchmarking (can be added later).

---

## 3) Test layering strategy (what runs where)

### Layer A — Pure unit tests (no JavaFX toolkit)

Target: DTOs, pure logic, and logic that can be decoupled from JavaFX runtime.

Characteristics:

- Runs on plain JUnit (no `Platform.startup`).
- Fast (< 100ms per test file).
- No `Stage`, no layout passes.

Primary candidates:

- `LayoutSerializer` JSON parsing/formatting and DTO mapping.
- `LayoutNode` DTO equality/shape expectations.
- Any extracted geometry/decision logic (see §6.2 refactors that improve testability).

### Layer B — Headless JavaFX integration tests (minimal UI)

Target: anything that manipulates `Node` parenting, `Scene` graph, `Stage`, or relies on JavaFX layout/bounds.

Characteristics:

- Uses JavaFX toolkit in headless mode.
- Verifies structural outcomes (tree changes, state changes, restored layout shape), not pixels.
- Uses deterministic “run on FX thread and wait” utilities.

Primary candidates:

- `DockManager` state transitions and tree mutations.
- `LayoutFactory` restore into live `DockElement` instances.
- `HitTester` when it depends on `Node` bounds.
- Floating/minimize/maximize behavior.

---

## 4) Tooling & Maven setup (to be implemented)

### 4.1 Dependencies to add (test scope)

Add to `papiflyfx-docks/pom.xml`:

- JUnit 5:
  - `org.junit.jupiter:junit-jupiter-api`
  - `org.junit.jupiter:junit-jupiter-engine`
- TestFX (for JavaFX thread + stage lifecycle helpers):
  - `org.testfx:testfx-junit5`
  - `org.testfx:testfx-core`
- Headless JavaFX (Monocle) for CI/headless runs:
  - `org.testfx:openjfx-monocle` (version aligned to JavaFX major; confirm exact artifact/version during implementation)

Notes:

- Prefer JUnit 5 only (no Vintage).
- Keep dependencies module-local (only `papiflyfx-docks`), unless other modules need the same.

### 4.2 Surefire configuration

Add `maven-surefire-plugin` config (either in root `pom.xml` pluginManagement or in `papiflyfx-docks/pom.xml`) to:

- Enable JUnit 5 provider.
- Pass headless properties when running in CI:
  - `-Dtestfx.robot=glass`
  - `-Dtestfx.headless=true`
  - `-Dprism.order=sw`
  - `-Dprism.text=t2k`
  - `-Djava.awt.headless=true`
  - Monocle platform properties as required (`-Dglass.platform=Monocle`, etc.)

### 4.3 Test source layout

Create:

- `papiflyfx-docks/src/test/java/org/metalib/papifly/fx/docks/...`
- a small `testutil` package for FX-thread helpers.

---

## 5) Conventions (so tests stay stable)

### 5.1 Naming

- Unit tests: `*Test` per class under test (`LayoutSerializerTest`, `DockManagerCloseLeafTest`, etc.).
- Integration tests (JavaFX): either
  - `*FxTest` suffix, or
  - separate package `.../fx/`.

### 5.2 FX thread rules

- Any interaction with `Node`, `Scene`, `Stage`, `Bounds`, `layout()` should run on the FX thread.
- Provide a single shared helper API:
  - `runFx(Runnable)`
  - `callFx(Callable<T>)`
  - `waitForFxEvents()`

### 5.3 Determinism

- Avoid time-based assertions.
- Avoid animation timers.
- Avoid asserting pixel coordinates unless they come from controlled inputs.

---

## 6) Test matrix (what to test)

This section lists concrete test cases by subsystem. During implementation, each bullet becomes one or more test methods.

### 6.1 Layout serialization (`docks/serial/LayoutSerializer`)

**Pure unit tests (Layer A)**

1. DTO → Map → DTO:
   - `LeafData` round-trip (id, title, optional `contentFactoryId`).
   - `SplitData` round-trip (orientation, dividerPosition, children).
   - `TabGroupData` round-trip (tabs list order, activeTabIndex).
2. JSON formatting/parsing:
   - `toJson(fromJson(json))` produces equivalent map (order-insensitive for objects).
   - Proper escaping of quotes, backslashes, and Unicode edge cases.
   - Numbers: integer vs decimal behavior (document expected type: `Integer` vs `Double`).
3. Robustness:
   - Unknown `type` in map → predictable failure mode (exception type + message).
   - Missing required keys → predictable failure mode.

### 6.2 Layout restore/capture (`docks/layout/*` + `DockManager.capture/restore`)

**Layer B (JavaFX integration)**

1. `LayoutFactory` builds correct element types for a given `LayoutNode` tree.
2. `DockManager.restore(layout)` followed by `capture()` yields a layout tree that is structurally equivalent.
3. `ContentFactory` usage:
   - When `LeafData.contentFactoryId` is present, content is created via the factory.
   - When missing, fallback behavior is consistent (document expected behavior).

### 6.3 Core model invariants (`docks/core/*`)

**Layer B (JavaFX integration)**

1. `DockLeaf`:
   - `DockData` id is stable.
   - state updates reflected where expected (via `DockManager`).
2. `DockTabGroup`:
   - adding/removing tabs updates active tab index correctly.
   - removing active tab chooses a new active tab deterministically.
3. `DockSplitGroup`:
   - divider position boundaries and orientation are preserved on capture/restore.
4. Parent cleanup:
   - Closing/removing a leaf collapses empty containers correctly (no empty tab groups / dangling splits).

### 6.4 DockManager tree mutations & lifecycle (`DockManager`)

**Layer B (JavaFX integration)**

1. `closeLeaf(leaf)`:
   - leaf is removed from dock tree.
   - content is disposed as intended (document what “dispose” means in this codebase).
2. `removeElement(element)`:
   - removes leaf or composite and rebalances tree correctly.
3. Builder behavior (`DockManager.create()`):
   - `withTheme`, `withLayout`, `withContentFactory` combinations produce expected root.
4. `dispose()`:
   - detaches handlers and closes floating windows (if present).

### 6.5 Drag/hit-testing (`docks/drag/*`)

Split into two parts:

**Part A (preferred): extract pure logic for drop-zone decisions**

- If feasible, extract “given bounds + pointer position → DropZone/index” into a small pure helper so it can be covered by Layer A.

**Part B (as-is): JavaFX integration tests**

1. `DragManager` threshold:
   - below threshold: no drag.
   - above threshold: drag begins with correct source.
2. `HitTester`:
   - returns correct `DropZone` for leaf/tab group center/edges.
   - tab bar targeting yields `TAB_BAR` or equivalent.
   - split targeting yields LEFT/RIGHT/TOP/BOTTOM depending on orientation.
   - insertion index rules in tab group are stable.

### 6.6 Floating / minimize / maximize (`docks/floating/*`, `docks/minimize/*`, `DockManager`)

**Layer B (JavaFX integration)**

1. Floating:
   - `floatLeaf(leaf)` moves leaf out of dock tree and sets `DockState.FLOATING`.
   - `dockLeaf(leaf)` restores into dock tree and sets `DockState.DOCKED`.
   - floating window lifecycle: close request behavior is deterministic.
2. Minimize:
   - `minimizeLeaf(leaf)` removes from dock without disposing; state set to `MINIMIZED`.
   - `restoreLeaf(leafId)` restores to dock using `RestoreHint` when available.
3. Maximize:
   - `maximizeLeaf(leaf)` sets maximized state and preserves previous layout snapshot.
   - `restoreMaximized()` restores exactly the prior layout.
   - maximizing a leaf that is not currently docked has a defined outcome (reject or auto-dock; document expected behavior).

### 6.7 Theme / rendering smoke tests (`Theme`, `OverlayCanvas`)

**Layer B (JavaFX integration)**

- Theme application does not throw and applies expected CSS properties where accessible.
- Overlay rendering API can be invoked without exceptions for representative drop zones.

---

## 7) CI and local execution considerations

### 7.1 Headless runs

- Prefer Monocle + software rendering for CI.
- On macOS developer machines, tests may run with the real Glass platform (non-headless) by default.

### 7.2 Flakiness mitigations

- Always wait for FX events (`WaitForAsyncUtils` / equivalent) after `runLater`.
- Avoid asserting intermediate states during layout pulses.
- Keep integration tests minimal and structural.

### 7.3 Expected runtime

- Layer A: seconds.
- Layer B: keep under ~30–60 seconds for the whole module.

---

## 8) Implementation phases (incremental delivery)

### Phase 1 — Test infrastructure (P0)

1. Add JUnit 5 + Surefire configuration.
2. Add TestFX + headless configuration.
3. Add `testutil` helpers for FX thread execution.

Deliverable: a single “sanity” FX test that starts toolkit and creates a minimal `DockManager`.

### Phase 2 — Serialization & DTO unit tests (P0)

1. `LayoutSerializerTest` covering all DTO types and JSON parsing edge cases.
2. Decide and document number typing and error behaviors.

### Phase 3 — Capture/restore & core invariants (P0)

1. `LayoutFactoryTest` (restore from DTO).
2. `DockManagerCaptureRestoreFxTest` (round-trip).
3. `DockManagerCloseLeafFxTest` (tree cleanup).

### Phase 4 — Drag/hit-testing (P1)

1. Add tests for `DragManager` threshold.
2. Add `HitTesterFxTest` with deterministic layout fixtures.
3. Optional refactor: extract pure decision logic and move most tests to Layer A.

### Phase 5 — Floating/minimize/maximize (P1)

1. State transition tests.
2. Restore-hint behavior tests.
3. Window lifecycle tests (as headless as possible).

---

## 9) Definition of Done

1. `mvn test -pl papiflyfx-docks` runs locally and in headless mode.
2. Core behaviors have automated coverage:
   - serializer round-trips + invalid input handling
   - capture/restore structural equivalence
   - close/remove cleanup invariants
   - drag/hit-testing decisions
   - float/minimize/maximize transitions
3. Tests are stable across repeated runs (no flaky timing).

