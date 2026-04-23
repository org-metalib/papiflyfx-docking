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

Coverage map summary:

| Behavior | Status | Current coverage |
| --- | --- | --- |
| Provider discovery via `ServiceLoader` | Partial | `RibbonShellSampleIntegrationFxTest` verifies GitHub/Hugo providers appear in the sample shell; no focused custom classloader/reload test. |
| Two-phase refresh and command canonicalization | Strong | `RibbonManagerTest.commandRegistry_canonicalizesAcrossRefreshCycles`; `RibbonCommandRegistryFxTest.commandIdentityIsStableAcrossContextRefreshes`. |
| Registry pruning after materialization | Strong | `RibbonManagerTest.commandRegistry_prunesCommandsNoLongerReachable`. |
| QAT id persistence/order | Strong | `RibbonSessionPersistenceFxTest.saveRestore_roundTripIncludesRibbonState`; `RibbonCommandRegistryFxTest` hidden/missing id restore cases. |
| Selected tab persistence and missing-tab fallback | Strong | `RibbonSessionPersistenceFxTest.restore_missingTabAndCommandFallsBackGracefully`. |
| Minimized-ribbon persistence | Strong | `RibbonSessionPersistenceFxTest.saveRestore_roundTripIncludesRibbonState`. |
| Adaptive layout transitions | Strong | `RibbonAdaptiveLayoutFxTest` collapse/expand/order/cache tests. |
| Collapsed popup open/close/keyboard/focus | Partial | Open and command execution are covered; close, Escape, and focus return are not. |
| Contextual-tab visibility from `RibbonContext` | Strong | `RibbonContextResolutionFxTest`, `HugoRibbonProviderTest`, and `RibbonShellSampleIntegrationFxTest`. |
| `HugoRibbonProvider` heuristic matrix | Partial | Happy paths and one negative path covered; branch matrix is incomplete. |
| Capability resolution inside floating windows | Missing | No ribbon test activates a floating leaf and asserts context/capabilities. |
| Provider throws during `getTabs(...)` | Missing | Implementation catches runtime failures; no test covers it. |
| Concurrent context updates across FX pulses | Missing | Existing tests use sequential waits; contract is implicitly FX-thread single-writer. |
| Theme switching while displayed/open popup | Missing | Login sample theme toggle is covered; ribbon live theme switch is not. |
| QAT restore for hidden contextual tab commands | Strong | `RibbonCommandRegistryFxTest.qatRestore_preservesHiddenContextualCommandIds`. |

### F-01: Floating-window ribbon context has no direct ribbon regression test
**Severity:** P1  
**Area:** Coverage  
**Evidence:** `RibbonContextResolutionFxTest` verifies contextual tab switching between dock groups, but no ribbon test floats a leaf, activates content inside the floating stage, and asserts `RibbonContextAttributes.FLOATING` plus typed capabilities. `DockManagerFloatingStageResolutionFxTest` only verifies floating-window creation, not ribbon/provider resolution.  
**Risk:** Provider commands that depend on capabilities could remain disabled, or a contextual tab could resolve against the wrong leaf, when the active content lives in a floating window.  
**Suggested follow-up:** `@qa-engineer` + `@core-architect`, M. Add a `papiflyfx-docking-docks` FxTest that builds a `RibbonDockHost`, floats a capability-bearing leaf, activates it from the floating stage, and asserts the ribbon context, tab visibility, and command enablement.

### F-02: Provider failure isolation is implemented but untested
**Severity:** P1  
**Area:** Coverage  
**Evidence:** `RibbonManager.collectTabs(...)` catches `RuntimeException` from `RibbonProvider#getTabs(...)`, but `RibbonManagerTest` has no throwing-provider case. Current provider tests only cover normal tab contribution and command routing.  
**Risk:** A future refresh refactor could let one bad provider fail the whole ribbon or clear unrelated provider tabs without a regression signal.  
**Suggested follow-up:** `@qa-engineer`, S. Add a `RibbonManagerTest` with one provider that throws and one healthy provider; assert healthy tabs remain materialized and command registry/QAT state is still coherent.

### F-03: Theme switching is not covered for live ribbon chrome or open collapsed popups
**Severity:** P1  
**Area:** Coverage  
**Evidence:** `Ribbon` binds theme through `RibbonDockHost` and applies CSS variables in `Ribbon.applyTheme(...)`, but ribbon tests do not change the `DockManager` theme while the ribbon is displayed. `SamplesSmokeTest.loginSampleThemeToggleDoesNotThrow` covers login UI, not ribbon UI.  
**Risk:** Theme listeners, cached ribbon groups, or collapsed popup content can retain stale CSS or throw during live theme changes.  
**Suggested follow-up:** `@qa-engineer` + `@ui-ux-designer`, M. Extend `RibbonAdaptiveLayoutFxTest` or add a focused FxTest that switches dark/light themes with the ribbon visible and again while a collapsed group popup is open.

### F-04: Collapsed popup keyboard dismissal and focus return are missing
**Severity:** P2  
**Area:** Determinism  
**Evidence:** `RibbonAdaptiveLayoutFxTest.collapsedGroupPopupKeepsCommandsReachable` shrinks to `COLLAPSED`, opens `.pf-ribbon-collapsed-popup`, and clicks a command, but it does not press Escape, click outside, assert close behavior, or verify focus returns to the collapsed group button.  
**Risk:** Popup accessibility and keyboard behavior can regress while mouse-only command reachability stays green. Focus-sensitive failures are also a known TestFX/Monocle flake vector.  
**Suggested follow-up:** `@qa-engineer` + `@ui-ux-designer`, S. Add assertions for Escape dismissal, outside-click dismissal, and post-dismiss focus owner using `WaitForAsyncUtils.waitForFxEvents()`.

### F-05: Hugo contextual heuristic matrix is only partially covered
**Severity:** P2  
**Area:** Provider tests  
**Evidence:** `HugoRibbonProviderTest` covers a combined Hugo factory/content path case, a markdown path case, and a Java negative case. `HugoRibbonProvider.isHugoEditorContext(...)` also branches on active type key, `CONTENT_FACTORY_ID`, `.markdown`, `/content/`, and type-key content, but those branches are not independently asserted.  
**Risk:** A provider refactor can silently drop one heuristic path, such as content-factory-only restore or `.markdown` files under content paths, while the broad happy path remains green.  
**Suggested follow-up:** `@feature-dev` + `@qa-engineer`, S. Add a table-style unit test in `papiflyfx-docking-hugo` for factory id, content type key, `.md`, `.markdown`, `/content/`, type-key markdown/hugo, and negative non-content markdown.

### F-06: Rapid context churn across FX pulses is untested
**Severity:** P2  
**Area:** Determinism  
**Evidence:** Context tests use explicit sequential updates followed by double `waitForFxEvents()` settles. No test drives several `RibbonManager.setContext(...)` calls in one FX pulse or validates that the final context wins after ribbon adaptive layout's queued `Platform.runLater(...)` pass.  
**Risk:** Stale contextual tabs, selected-tab fallback, or QAT derived-command updates could appear only under fast focus changes, especially during docking/floating transitions.  
**Suggested follow-up:** `@qa-engineer`, S. Add a deterministic FxTest that queues multiple context changes in a single `runFx(...)`, waits for FX events, and asserts final tab visibility, selected tab, and command registry state.

### F-07: Ribbon fixture code is duplicated instead of centralized
**Severity:** P3  
**Area:** Fixture  
**Evidence:** There is no `RibbonTestSupport` under `papiflyfx-docking-docks/src/test/java`; each ribbon FxTest creates its own `RibbonManager`, `Ribbon`, `RibbonDockHost`, and inline `RibbonProvider`/stub DSL. Provider tests hand-roll capability stubs and command traversal helpers separately in GitHub and Hugo.  
**Risk:** Follow-up tests will keep copying subtly different settle logic, provider builders, and capability doubles, which increases flake risk and review cost.  
**Suggested follow-up:** `@qa-engineer`, M. Introduce a small test support helper for `DockManager + RibbonDockHost`, in-memory providers, command lookup, session round-trip setup, and a documented convention for hand-rolled capability doubles.

### F-08: Focused Maven test selectors need `surefire.failIfNoSpecifiedTests=false`
**Severity:** P2  
**Area:** CI  
**Evidence:** The documented focused commands with `-pl ... -am -Dtest=...` fail in upstream modules that have no matching tests. The usable commands during this review were `./mvnw -pl papiflyfx-docking-docks -am '-Dtest=Ribbon*Test' -Dsurefire.failIfNoSpecifiedTests=false -Dtestfx.headless=true test` and `./mvnw -pl papiflyfx-docking-samples -am -Dtest=SamplesSmokeTest -Dsurefire.failIfNoSpecifiedTests=false -Dtestfx.headless=true test`.  
**Risk:** Developers may avoid the focused ribbon loop or misread selector setup failures as test failures.  
**Suggested follow-up:** `@ops-engineer` + `@qa-engineer`, S. Document the extra flag in the review plan or add a parent-POM profile/alias for ribbon-focused runs.

### F-09: Refresh performance has telemetry assertions but no scale guardrail
**Severity:** P3  
**Area:** Performance  
**Evidence:** `RibbonAdaptiveLayoutFxTest` asserts cache hits and structural rebuild counts, and `papiflyfx-docking-code` has a `benchmark` JUnit tag pattern. There is no ribbon benchmark or large-provider fixture for 50+ groups, and no timing or layout-pass budget around `RibbonManager.refresh()`.  
**Risk:** Refresh or adaptive layout can become noticeably slower with large provider contributions without tripping the normal suite.  
**Suggested follow-up:** `@qa-engineer` + `@ops-engineer`, M. Reuse the code module's `benchmark` tag convention for an opt-in ribbon refresh benchmark; keep it excluded from default CI unless a concrete performance budget is adopted.

### F-10: Provider-level Fx coverage is sample-driven, not module-local
**Severity:** P3  
**Area:** Provider tests  
**Evidence:** `GitHubRibbonProviderTest` and `HugoRibbonProviderTest` are unit tests for tab structure, command routing, and capability-based enablement. Runtime visibility and command execution are covered in sample FxTests (`RibbonShellSampleIntegrationFxTest`, `GitHubRibbonSampleFxTest`, `HugoRibbonSampleFxTest`) rather than in the provider modules.  
**Risk:** Provider module regressions may require running the broader samples module to catch, and feature owners do not get a focused `-pl papiflyfx-docking-hugo` or `-pl papiflyfx-docking-github` UI feedback loop.  
**Suggested follow-up:** `@feature-dev` + `@qa-engineer`, S/M. Add focused provider-module FxTests only where they provide coverage not already present in samples, especially contextual visibility flips and capability enablement in a mounted ribbon.

Validation notes: source inspection covered the listed ribbon, feature-provider, and sample test classes. Scoped validation passed after adding `-Dsurefire.failIfNoSpecifiedTests=false`: docks ribbon selector ran 20 tests in about 8 seconds on macOS/Monocle; samples smoke ran 13 tests in about 10 seconds. No `Thread.sleep(...)` or retry annotations were found in the in-scope ribbon tests; the only sleeps found by scan were outside this review scope.

## Handoff Snapshot

Lead Agent: `@qa-engineer`  
Task Scope: test-coverage review for the ribbon feature — coverage map, determinism, fixtures, performance guardrails, regression protection  
Impacted Modules: `spec/**` only  
Files Changed: this file (on completion)  
Key Invariants:

- no production test code changes
- findings must cite test class names or their absence
- determinism concerns take priority over coverage breadth

Validation Performed: test source inspection; `./mvnw -pl papiflyfx-docking-docks -am '-Dtest=Ribbon*Test' -Dsurefire.failIfNoSpecifiedTests=false -Dtestfx.headless=true test`; `./mvnw -pl papiflyfx-docking-samples -am -Dtest=SamplesSmokeTest -Dsurefire.failIfNoSpecifiedTests=false -Dtestfx.headless=true test`  
Open Risks / Follow-ups: recorded as numbered findings F-01 through F-10  
Required Reviewer: `@core-architect`, `@ops-engineer`, `@spec-steward`
