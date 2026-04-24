# Progress - Ribbon 5 Consolidated Implementation Follow-up

**Status:** Phase 3 implementation complete; reviewer handoff ready
**Current Milestone:** Phase 3 - Build, Fixture, And Performance Guardrails complete
**Priority:** P1 (High)  
**Planning Lead:** @spec-steward  
**Implementation Coordination Lead:** @spec-steward  
**Required Reviewers:** @core-architect, @feature-dev, @ops-engineer, @ui-ux-designer, @qa-engineer, @spec-steward

## Completion Summary

- Review finding intake: 100%
- Phase 0 - Planning And Triage: 100%
- Phase 1 - P1 Runtime And Accessibility Stabilization: 100%
- Phase 2 - Context And Provider Authoring Foundation: 100%
- Phase 3 - Build, Fixture, And Performance Guardrails: 100%
- Phase 4 - API Design And Compatibility Decisions: 0%
- Phase 5 - Documentation And Roadmap Closure: 0%
- Validation: Phase 3 focused validation complete

## Accomplishments

- [2026-04-23] Read the spec-steward role brief and routed this planning task to @spec-steward because it is cross-cutting `spec/**` coordination.
- [2026-04-23] Inspected all populated review documents under `spec/papiflyfx-docking-docks/2026-04-23-0-ribbon-5`.
- [2026-04-23] Counted and mapped all 49 findings:
  - `review-core-architect.md`: 14 findings
  - `review-feature-dev.md`: 7 findings
  - `review-ops-engineer.md`: 4 findings
  - `review-ui-ux-designer.md`: 8 findings
  - `review-qa-engineer.md`: 10 findings
  - `review-spec-steward.md`: 6 findings
- [2026-04-23] Created `plan.md` with six implementation workstreams:
  - Runtime Correctness And Diagnostics
  - Accessibility, Visual Tokens, And Adaptive UX
  - Context, Capability, And Provider Authoring
  - Test, Build, And Performance Guardrails
  - API Shape And Breaking-Change Design Debt
  - Documentation, Roadmap, And Release Hygiene
- [2026-04-23] Prioritized P1 findings for the first implementation phase and kept breaking API/session changes behind an explicit design step.
- [2026-04-23] Added `.prompt-codex.md` to start Phase 1 implementation with @core-architect as lead and @ui-ux-designer, @qa-engineer, @feature-dev, and @spec-steward as required reviewers.
- [2026-04-23] Assigned Phase 1 implementation to @core-architect with @ui-ux-designer, @qa-engineer, @feature-dev, and @spec-steward as required reviewers.
- [2026-04-23] Chose runtime command-state projection as the canonicalization strategy: `CommandRegistry` keeps first command metadata by id and projects incoming `enabled`/`selected` snapshots plus action dispatch into the canonical command on refresh.
- [2026-04-23] Made JavaFX command bindings disposable and wired ribbon controls, QAT rebuilds, launcher rebinding, control-cache eviction, and collapsed-popup disposal to release command-state listeners.
- [2026-04-23] Added test-observable telemetry plus warning logs for provider failures, duplicate tab metadata conflicts, and duplicate command metadata conflicts.
- [2026-04-23] Restored focus to collapsed group triggers when popups hide and added Escape/focus-return TestFX coverage.
- [2026-04-23] Added visible focus states for ribbon tabs, QAT buttons, group launchers, and collapsed group buttons; added icon-only accessible names; added vector disabled icon tint plus raster/icon opacity reduction; added contextual selected/focused accent styling.
- [2026-04-23] Added live theme-switch coverage for ribbon chrome and open collapsed popups.
- [2026-04-23] Began Phase 2 with @core-architect lead, @feature-dev support, and required @feature-dev, @qa-engineer, @spec-steward, and @ops-engineer review.
- [2026-04-23] Added typed context metadata through `RibbonAttributeKey<T>` and typed `RibbonContext#attribute(...)` / `withAttribute(...)` overloads while preserving raw string access.
- [2026-04-23] Added canonical typed keys beside existing `RibbonContextAttributes` raw constants, including `CONTENT_KIND_KEY` and `CONTENT_DOMAIN_KEY` for explicit contextual metadata.
- [2026-04-23] Added `RibbonAttributeContributor` and `RibbonCapabilityContributor` as non-breaking active-content contracts so feature modules can publish explicit metadata and one or more capabilities while depending only on `papiflyfx-docking-api`.
- [2026-04-23] Updated `DockManager.buildRibbonContext(...)` to consume explicit content metadata/capabilities, register active content under its interface hierarchy for direct capability hits, retain raw `ACTIVE_CONTENT_NODE` compatibility, and mark newly floated leaves as the active ribbon source.
- [2026-04-23] Updated Hugo contextual tab activation to prefer explicit `CONTENT_DOMAIN_KEY` / `CONTENT_KIND_KEY` metadata and keep legacy factory/type/path/title heuristics as fallback.
- [2026-04-23] Added focused tests for typed/raw attribute compatibility, floating-window active content context/capability resolution, and table-style Hugo explicit/legacy contextual activation branches.
- [2026-04-23] Added `spec/papiflyfx-docking-docks/ribbon-provider-authoring.md` and linked it from `papiflyfx-docking-docks/README.md`.
- [2026-04-23] Deferred archetype ribbon scaffold work to a later @ops-engineer Phase 3/5 task because the Phase 2 docs now cover the provider path and no template change was required to validate the non-breaking API/runtime work.
- [2026-04-23] Began Phase 3 with @qa-engineer lead, @ops-engineer support, and required @ops-engineer, @core-architect, @ui-ux-designer, and @spec-steward review.
- [2026-04-23] Decided not to centralize Surefire/TestFX POM configuration in Phase 3 because the duplicated blocks still carry module-specific native-access requirements (`javafx.web`, `javafx.media`) and a docs-only fix closes the focused selector pain without broader build churn.
- [2026-04-23] Added focused ribbon validation commands to `ribbon-provider-authoring.md`, including `-Dsurefire.failIfNoSpecifiedTests=false` for `-pl ... -am` selector runs.
- [2026-04-23] Introduced narrow test-scoped `RibbonTestSupport` for repeated docks ribbon fixture code: FX settling, in-memory providers, simple command tabs, command lookup, and tab-id extraction.
- [2026-04-23] Adopted `RibbonTestSupport` in docks ribbon tests where repeated provider/settle helpers already existed.
- [2026-04-23] Added rapid context churn coverage in `RibbonCommandRegistryFxTest`: multiple context changes queued in one FX turn settle to the final tab model, selected tab, and command registry state.
- [2026-04-23] Added provider-module Fx coverage for real GitHub and Hugo providers. The tests mount a `RibbonManager`/`Ribbon`, drive context changes, and assert command enablement/action routing plus Hugo explicit metadata visibility.
- [2026-04-23] The new GitHub provider Fx test exposed that canonical commands refreshed enabled state but kept the first no-capability action lambda. Updated `CommandRegistry` to keep stable canonical identity and first metadata while refreshing action dispatch from later provider emissions.
- [2026-04-23] Added `CommandRegistryTest` coverage for refreshed action dispatch so provider callbacks stay coherent with refreshed command state.
- [2026-04-23] Deferred a ribbon refresh/adaptive-layout benchmark. Owner: @qa-engineer with @ops-engineer and @core-architect input. Reason: no accepted timing/layout budget exists. Likely shape: benchmark-tagged large-provider fixture with 50+ groups/commands, refresh/adaptive telemetry counts, and a budget derived from observed CI baselines before any timing assertion enters a release gate.

## Current Understanding

The review findings are not independent tasks. Several findings describe the same underlying issue from different roles:

1. Command-state canonicalization appears in both core and feature reviews and should be handled once by @core-architect with feature-provider review.
2. Provider and command collision diagnostics appear in core, feature, QA, and ops reviews and should share one logging/telemetry policy.
3. Collapsed popup focus behavior appears in UI/UX and QA reviews and should be implemented with one runtime fix plus one TestFX regression.
4. Provider authoring guidance appears in feature, spec, and ops reviews and should produce one canonical guide plus optional archetype/sample support.
5. Session documentation and schema forward-compatibility appear in core and spec reviews and should be closed through a shared docs/design update.

The implementation should therefore proceed by workstream, not by review file.

## Phase Status

| Phase | Lead | Status | Notes |
| --- | --- | --- | --- |
| Phase 0 - Planning And Triage | @spec-steward | Complete | `plan.md` and this tracker created |
| Phase 1 - P1 Runtime And Accessibility Stabilization | @core-architect / @ui-ux-designer / @qa-engineer | Complete | Runtime, accessibility, diagnostics, and focused validation complete |
| Phase 2 - Context And Provider Authoring Foundation | @core-architect / @feature-dev | Complete | Typed metadata, explicit capability contribution, Hugo fallback coverage, floating context test, provider guide complete |
| Phase 3 - Build, Fixture, And Performance Guardrails | @qa-engineer / @ops-engineer | Complete | Focused selector docs, narrow test helper, rapid churn coverage, provider Fx coverage, and benchmark deferral complete |
| Phase 4 - API Design And Compatibility Decisions | @core-architect / @spec-steward | Not started | Breaking-change candidates only |
| Phase 5 - Documentation And Roadmap Closure | @spec-steward | Not started | Some docs can begin early; final closure waits for implementation outcomes |

## Next Tasks

1. Required Phase 3 reviewers should inspect their ownership slices:
   - @ops-engineer: docs-only Surefire/TestFX decision, no POM centralization, and provider-module validation impact.
   - @core-architect: `CommandRegistry` canonical command action-dispatch refresh semantics and API documentation alignment.
   - @ui-ux-designer: no new visual/CSS changes; confirm Phase 3 test coverage does not alter UI expectations.
   - @spec-steward: progress/handoff completeness, benchmark deferral, and docs alignment.
2. Begin Phase 4 only after Phase 3 reviewer feedback is resolved or explicitly recorded.

## Validation Status

Phase 1 automated validation completed on 2026-04-23.

Commands and results:

1. `./mvnw -pl papiflyfx-docking-docks -am '-Dtest=Ribbon*Test,CommandRegistryTest' -Dsurefire.failIfNoSpecifiedTests=false -Dtestfx.headless=true test`
   - First run failed because the new duplicate-tab diagnostic path created `TabAccumulator` instances without merging the initial tab groups.
   - Fixed the accumulator regression and reran successfully: 37 tests, 0 failures, 0 errors, 0 skips.
2. `./mvnw -pl papiflyfx-docking-api,papiflyfx-docking-docks -am compile`
   - Passed.
3. `./mvnw -pl papiflyfx-docking-github,papiflyfx-docking-hugo -am compile`
   - Passed.
4. `./mvnw -pl papiflyfx-docking-samples -am '-Dtest=*Ribbon*FxTest' -Dsurefire.failIfNoSpecifiedTests=false -Dtestfx.headless=true test`
   - Passed. Reactor also ran matching upstream docks ribbon Fx tests; samples reported 5 tests, 0 failures, 0 errors, 0 skips.

Source inspection performed:

1. `spec/agents/spec-steward.md`
2. `spec/papiflyfx-docking-docks/2026-04-23-0-ribbon-5/review-core-architect.md`
3. `spec/papiflyfx-docking-docks/2026-04-23-0-ribbon-5/review-feature-dev.md`
4. `spec/papiflyfx-docking-docks/2026-04-23-0-ribbon-5/review-ops-engineer.md`
5. `spec/papiflyfx-docking-docks/2026-04-23-0-ribbon-5/review-ui-ux-designer.md`
6. `spec/papiflyfx-docking-docks/2026-04-23-0-ribbon-5/review-qa-engineer.md`
7. `spec/papiflyfx-docking-docks/2026-04-23-0-ribbon-5/review-spec-steward.md`

Phase 2 automated validation completed on 2026-04-23.

Commands and results:

1. `./mvnw -pl papiflyfx-docking-api,papiflyfx-docking-docks -am compile`
   - Passed.
2. `./mvnw -pl papiflyfx-docking-docks -am -Dtest=RibbonFloatingContextFxTest -Dsurefire.failIfNoSpecifiedTests=false -Dtestfx.headless=true test`
   - First runs failed because the new test exposed that floating a leaf left the docked tab group as the active ribbon source in headless focus conditions.
   - Fixed `DockManager.floatLeaf(...)` to mark the created floating tab group active for ribbon context and reran successfully: 1 test, 0 failures, 0 errors, 0 skips.
3. `./mvnw -pl papiflyfx-docking-docks -am '-Dtest=Ribbon*Test,Ribbon*FxTest,CommandRegistryTest' -Dsurefire.failIfNoSpecifiedTests=false -Dtestfx.headless=true test`
   - Passed: 39 tests, 0 failures, 0 errors, 0 skips.
   - Monocle emitted a non-fatal pixel-buffer warning during `RibbonAdaptiveLayoutFxTest`; the suite completed successfully.
4. `./mvnw -pl papiflyfx-docking-hugo -am -Dtest=HugoRibbonProviderTest -Dsurefire.failIfNoSpecifiedTests=false -Dtestfx.headless=true test`
   - Passed: 6 tests, 0 failures, 0 errors, 0 skips.
5. `./mvnw -pl papiflyfx-docking-github,papiflyfx-docking-hugo -am -Dtestfx.headless=true test`
   - Passed.
   - Reactor results: `papiflyfx-docking-docks` 101 tests, `papiflyfx-docking-hugo` 20 tests, `papiflyfx-docking-github` 53 tests; all 0 failures, 0 errors, 0 skips.
   - Expected fixture warnings were emitted for adapter restore/save failures, malformed SVG fallback, provider failure diagnostics, malformed ribbon session extension handling, and detached floating owner-stage setup. GitHub also emitted the known SLF4J no-op binding notice and a non-fatal Monocle pixel-buffer warning.

No samples or archetype templates were touched, so sample/archetype validation was not required for Phase 2.

Phase 3 automated validation completed on 2026-04-23.

Commands and results:

1. `./mvnw -pl papiflyfx-docking-docks -am '-Dtest=Ribbon*Test,Ribbon*FxTest,CommandRegistryTest' -Dsurefire.failIfNoSpecifiedTests=false -Dtestfx.headless=true test`
   - Initial Phase 3 run passed after adding the rapid churn test and helper adoption: 40 tests, 0 failures, 0 errors, 0 skips.
   - After the provider Fx test exposed stale action dispatch and `CommandRegistry` was fixed, reran successfully: 41 tests, 0 failures, 0 errors, 0 skips.
   - Reran after final public API/provider-authoring documentation alignment and recompile: 41 tests, 0 failures, 0 errors, 0 skips.
   - Expected warning-path output appeared for malformed SVG fallback, provider failure diagnostics, duplicate id diagnostics, malformed ribbon session extension handling, and the known non-fatal Monocle pixel-buffer warning.
2. `./mvnw -pl papiflyfx-docking-github,papiflyfx-docking-hugo -am -Dtestfx.headless=true test`
   - First run failed in `GitHubRibbonProviderFxTest.commandEnablementTracksCapabilityContextRefreshes`: command enablement refreshed to true, but the canonical command still executed the first no-capability no-op action.
   - Fixed `CommandRegistry` to keep stable canonical identity/first metadata while refreshing action dispatch from later provider emissions.
   - Rerun passed.
   - Reactor results after the fix: `papiflyfx-docking-docks` 103 tests, `papiflyfx-docking-hugo` 21 tests, `papiflyfx-docking-github` 54 tests; all 0 failures, 0 errors, 0 skips.
   - Expected warning-path output appeared for adapter restore/save fallback, malformed SVG fallback, provider failure diagnostics, malformed ribbon session extension handling, detached floating owner-stage setup, the known SLF4J no-op binding notice, and a non-fatal Monocle pixel-buffer warning.
3. `./mvnw -pl papiflyfx-docking-samples -am '-Dtest=*Ribbon*FxTest' -Dsurefire.failIfNoSpecifiedTests=false -Dtestfx.headless=true test`
   - Passed.
   - Reactor also ran matching upstream ribbon Fx tests. Final samples result: 5 tests, 0 failures, 0 errors, 0 skips.

No POM/TestFX/Surefire configuration was changed, so a full repository test run was not required for Phase 3. Samples files were not touched, but the sample ribbon Fx selector was run because `CommandRegistry` action dispatch is shared runtime behavior.

## Open Risks

- The plan intentionally includes breaking-change candidates, but those must not be implemented without a narrower design note and migration plan.
- The review files were already modified in the worktree before this planning pass; this tracker treats their current contents as the authoritative intake.
- Some P2/P3 items are broad enough to become separate follow-up plans after Phase 3.
- Visual/accessibility changes may need an interactive SamplesApp pass in addition to headless tests.
- Phase 1 visual checks were automated through CSS/state assertions and TestFX theme/focus coverage; no interactive SamplesApp pass was performed.
- Raster icons cannot be tinted through CSS, so Phase 1 intentionally reduces disabled raster/icon opacity while vector SVG/octicon controls receive disabled fill styling.
- Headless Monocle emitted a non-fatal pixel-buffer warning during `RibbonAdaptiveLayoutFxTest`; the affected suites completed successfully.
- `RibbonContextAttributes.ACTIVE_CONTENT_NODE` remains populated and deprecated for compatibility; removal stays out of scope until a breaking ribbon-6 plan.
- Hugo legacy heuristics are intentionally retained as fallback. Explicit `CONTENT_DOMAIN_KEY` metadata takes precedence and can suppress fallback claims for non-Hugo content.
- The archetype ribbon scaffold remains deferred to @ops-engineer for Phase 5 or a separate generated-app plan because Phase 3 did not reopen template work.
- Surefire/TestFX POM centralization remains deferred to @ops-engineer with @qa-engineer review; this phase documented focused selectors instead of changing module-specific JavaFX native-access flags.
- Ribbon refresh/adaptive-layout benchmarking remains deferred to @qa-engineer with @ops-engineer and @core-architect input until a large-provider fixture and concrete timing/layout budget are agreed. Any future benchmark should be opt-in and excluded from default CI.
- Full repository `./mvnw -Dtestfx.headless=true test` was not run because Phase 3 made no parent/module POM changes.

## Handoff Snapshot

Lead Agent: `@qa-engineer`
Task Scope: Phase 3 - Build, Fixture, And Performance Guardrails
Impacted Modules: `papiflyfx-docking-api`, `papiflyfx-docking-docks`, `papiflyfx-docking-github`, `papiflyfx-docking-hugo`, `spec/**`
Files Changed:
- `papiflyfx-docking-api/src/main/java/org/metalib/papifly/fx/api/ribbon/PapiflyCommand.java`
- `papiflyfx-docking-api/src/main/java/org/metalib/papifly/fx/api/ribbon/RibbonProvider.java`
- `papiflyfx-docking-api/src/main/java/org/metalib/papifly/fx/api/ribbon/package-info.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/CommandRegistry.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonManager.java`
- `papiflyfx-docking-docks/src/test/java/org/metalib/papifly/fx/docks/ribbon/CommandRegistryTest.java`
- `papiflyfx-docking-docks/src/test/java/org/metalib/papifly/fx/docks/ribbon/RibbonCommandRegistryFxTest.java`
- `papiflyfx-docking-docks/src/test/java/org/metalib/papifly/fx/docks/ribbon/RibbonContextResolutionFxTest.java`
- `papiflyfx-docking-docks/src/test/java/org/metalib/papifly/fx/docks/ribbon/RibbonFloatingContextFxTest.java`
- `papiflyfx-docking-docks/src/test/java/org/metalib/papifly/fx/docks/ribbon/RibbonManagerTest.java`
- `papiflyfx-docking-docks/src/test/java/org/metalib/papifly/fx/docks/ribbon/RibbonTestSupport.java`
- `papiflyfx-docking-github/src/test/java/org/metalib/papifly/fx/github/ribbon/GitHubRibbonProviderFxTest.java`
- `papiflyfx-docking-hugo/src/test/java/org/metalib/papifly/fx/hugo/ribbon/HugoRibbonProviderFxTest.java`
- `spec/papiflyfx-docking-docks/ribbon-provider-authoring.md`
- `spec/papiflyfx-docking-docks/2026-04-23-0-ribbon-5/progress.md`
Key Invariants:
- feature modules continue to depend on `papiflyfx-docking-api`, not `papiflyfx-docking-docks`
- QAT id-first persistence semantics are unchanged
- canonical command identity remains stable while runtime state and action dispatch refresh from later provider emissions
- no POM/TestFX/Surefire configuration was changed
- provider-module production dependencies remain pointed at `papiflyfx-docking-api`; new runtime imports are test-scoped
- benchmark/performance guardrails remain opt-in-only until a concrete budget is approved
Validation Performed: required docks ribbon selector; broad GitHub/Hugo provider module tests; sample ribbon Fx selector as downstream integration
Open Risks / Follow-ups: reviewer inspection pending; Surefire/TestFX centralization deferred; benchmark deferred pending budget; archetype ribbon scaffold remains deferred to Phase 5/future @ops-engineer work; no full repository test run because no POM changes were made
Required Reviewer: `@ops-engineer`, `@core-architect`, `@ui-ux-designer`, `@spec-steward`
