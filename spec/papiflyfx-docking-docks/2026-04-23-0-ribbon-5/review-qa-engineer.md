# Ribbon 5 Review — QA & Test Engineer Perspective

**Priority:** P1 (High)  
**Lead Agent:** `@qa-engineer`  
**Required Reviewers:** `@core-architect`, `@ops-engineer`, `@spec-steward`  
**Workflow:** review-only; emit findings into the `Findings` section at the bottom of this file.

## Goal

Map the ribbon's current automated coverage, identify gaps, assess headless determinism, and propose a test posture for the next iteration. Keep the suite fast, green in CI, and representative of the risks identified by the other review plans.

## Scope

### In scope

1. Ribbon test classes in `papiflyfx-docking-docks/src/test/java/org/metalib/papifly/fx/docks/ribbon/`:
   - `CommandRegistryTest`
   - `RibbonManagerTest`
   - `RibbonAdaptiveLayoutFxTest`
   - `RibbonSessionPersistenceFxTest`
   - `RibbonContextResolutionFxTest`
   - `RibbonCommandRegistryFxTest`
2. Feature-module ribbon tests in `papiflyfx-docking-github/src/test/java/org/metalib/papifly/fx/github/ribbon/` and `papiflyfx-docking-hugo/src/test/java/org/metalib/papifly/fx/hugo/ribbon/`.
3. `SamplesSmokeTest` and any ribbon-focused `*FxTest` under `papiflyfx-docking-samples/src/test/java/`.
4. `papiflyfx-docking-code` benchmark posture (JUnit `benchmark` tag) as a template for a potential ribbon benchmark.
5. Headless profile configuration inherited from root `pom.xml`.

### Out of scope

1. Writing new tests (this is a review; tests are authored in follow-up plans).
2. Build/dependency concerns (`@ops-engineer`).
3. Visual assertion granularity (`@ui-ux-designer`).

## Review Questions

### A. Coverage map

For each ribbon behavior, record which test covers it today and whether the coverage is strong, partial, or missing:

1. Provider discovery via `ServiceLoader`.
2. Two-phase refresh: command canonicalization then tab materialization.
3. Command registry pruning after materialization (a removed command must be evictable without breaking QAT restore for still-present commands).
4. QAT add / remove / reorder persistence.
5. Selected-tab persistence across restart (including when the tab id disappears).
6. Minimized-ribbon persistence.
7. Adaptive layout transitions LARGE ↔ MEDIUM ↔ SMALL ↔ COLLAPSED.
8. Collapsed-group popup open/close, keyboard dismissal, focus return.
9. Contextual-tab visibility driven by `RibbonContext`.
10. `HugoRibbonProvider` heuristic matrix (factory id, content type key, file extension, path pattern, type key content).
11. Capability-based resolution when active leaf is inside a floating window.
12. Behavior when a provider throws during `getTabs(...)`.
13. Concurrent context updates across FX pulses (not expected in practice; confirm the contract).
14. Theme switching while ribbon is displayed; while a collapsed popup is open.
15. QAT restore for commands that live on a hidden contextual tab.

Flag items in rows 11–15 first — they are the most likely gaps.

### B. Determinism under headless

1. TestFX + Monocle determinism for ribbon layout: does `Ribbon` ever depend on font metrics that differ between Linux CI and developer machines?
2. Are there any `Platform.runLater(...)` paths that rely on implicit timing (e.g., two consecutive layout passes)? Identify and suggest `waitForFxEvents(...)` or `WaitForAsyncUtils.waitForFxEvents()` usages.
3. Are there `Thread.sleep(...)` calls in ribbon tests? If yes, they should be replaced.
4. Do any tests depend on window decorations or OS-level focus? Monocle typically removes those concerns, but confirm.
5. Does the `headless-tests` profile pass on macOS developer machines identically to Linux CI?

### C. Fixture and helper strategy

1. Is there a shared `RibbonTestSupport` that builds a minimal `DockManager` + `RibbonDockHost`? If not, is that duplicated across tests?
2. Are there factory helpers for creating synthetic `RibbonProvider` instances (`inMemoryProvider("tab", "group", ...)`) or does each test build its own inline DSL?
3. Are capability interfaces mocked via hand-rolled test doubles or via Mockito? Pick one convention and call out drift.
4. Is there a fixture for "session payload with ribbon block" that round-trips through `DockSessionService`? Coverage for forward/backward compatibility depends on it.

### D. Performance guardrails

1. Is there any benchmark or timing assertion for `RibbonManager.refresh()`? The research surfaced a concern at 50+ groups.
2. Could we reuse `papiflyfx-docking-code`'s `benchmark` JUnit tag pattern to create a ribbon benchmark module? Weigh cost vs value.
3. Is `RibbonLayoutTelemetry` exposed enough that tests can assert "no layout thrash beyond N passes" on a resize? If not, propose the smallest addition that would enable such assertions.

### E. Regression protection from ribbon-3 fix

1. Is there a regression test for label/caption clipping (ribbon-3)? The test should assert descenders render fully across the LARGE/MEDIUM modes.
2. Does the test take a theme-independent measurement, or is it bound to one platform's font?

### F. Flakiness watchlist

1. Known flakiness vectors: drag-and-drop, focus changes during floating transitions, popup visibility. Identify ribbon tests that touch these vectors and record anti-flake notes.
2. Are retries (`@RetryingTest` or similar) in use anywhere? If yes, flag them — flakes should be fixed, not papered over.

### G. CI ergonomics

1. Typical time cost of the ribbon test classes under `-Dtestfx.headless=true` on a fresh clone. Ball-park numbers only.
2. Can we add a focused ribbon-only test alias in the parent POM (e.g., `-Dtest=*Ribbon*Test`) that developers can run for fast feedback? Or is that already idiomatic via `-pl`?
3. Coordinate with `@ops-engineer` (`review-ops-engineer.md` E) about any Surefire flag additions needed.

### H. Provider-level tests

1. Do `GitHubRibbonProvider` and `HugoRibbonProvider` each have:
   - a unit test verifying tab structure at a fixed context,
   - an FxTest verifying contextual-tab visibility flips when the context changes,
   - a test verifying command enablement reacts to capability presence/absence?
2. Is the Sample provider's test coverage sufficient to prevent accidental breakage of the generic `Ribbon Shell` demo? Cross-check with `SamplesSmokeTest`.

## Review Procedure

1. Enumerate every ribbon test class and read its assertions — summarize what each guards.
2. Cross-check against the coverage map in section A. Mark each behavior as Strong / Partial / Missing.
3. For each Missing or Partial, propose the smallest test that would close the gap, and record which module it belongs in.
4. Sanity-check determinism by reading the slowest ribbon FxTest's run duration trends if available in CI history.

## Deliverable

Populate the `Findings` section below using the common template:

```md
### F-<NN>: <short title>
**Severity:** P0|P1|P2|P3  
**Area:** <Coverage | Determinism | Fixture | Performance | Regression | Flake | CI | Provider tests>  
**Evidence:** <test file or absence of it; CI observation>  
**Risk:** <what regression could slip through>  
**Suggested follow-up:** <lead role, rough cost S/M/L>
```

## Validation

Optional dry-runs while scoping:

1. `./mvnw -pl papiflyfx-docking-docks -am -Dtest=Ribbon*Test -Dtestfx.headless=true test`
2. `./mvnw -pl papiflyfx-docking-samples -am -Dtest=SamplesSmokeTest -Dtestfx.headless=true test`

Do not commit output into this file; note any test that fails during scoping as a P0 finding.

## Findings

_Not yet started._

## Handoff Snapshot

Lead Agent: `@qa-engineer`  
Task Scope: test-coverage review for the ribbon feature — coverage map, determinism, fixtures, performance guardrails, regression protection  
Impacted Modules: `spec/**` only  
Files Changed: this file (on completion)  
Key Invariants:

- no production test code changes
- findings must cite test class names or their absence
- determinism concerns take priority over coverage breadth

Validation Performed: test source inspection; optional dry-runs recorded in findings  
Open Risks / Follow-ups: recorded as numbered findings  
Required Reviewer: `@core-architect`, `@ops-engineer`, `@spec-steward`
