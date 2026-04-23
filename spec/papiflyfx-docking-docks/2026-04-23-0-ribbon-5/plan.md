# Plan - Ribbon 5 Consolidated Implementation Follow-up

**Priority:** P1 (High)  
**Planning Lead:** @spec-steward  
**Implementation Coordination Lead:** @spec-steward  
**Phase Leads:** @core-architect, @feature-dev, @ops-engineer, @ui-ux-designer, @qa-engineer  
**Required Reviewers:** @core-architect, @feature-dev, @ops-engineer, @ui-ux-designer, @qa-engineer, @spec-steward  
**Workflow:** review consolidation -> phased implementation -> validation -> documentation closure

## Goal

Convert all findings from the Ribbon 5 review documents into an implementation roadmap that can be executed without losing ownership, dependency order, or validation coverage.

This plan intentionally groups overlapping findings into workstreams. The source review files remain the detailed evidence log; this document is the execution plan and progress tracker entry point.

## Source Reviews

The plan covers all current findings from:

1. `review-core-architect.md` - 14 findings.
2. `review-feature-dev.md` - 7 findings.
3. `review-ops-engineer.md` - 4 findings.
4. `review-ui-ux-designer.md` - 8 findings.
5. `review-qa-engineer.md` - 10 findings.
6. `review-spec-steward.md` - 6 findings.

Total intake: 49 findings.

## Scope

### In Scope

1. Public ribbon API/SPI corrections and migration notes where findings call for them.
2. Runtime correctness fixes in `papiflyfx-docking-docks`, especially command state, listener lifecycle, provider diagnostics, popup focus, and context propagation.
3. Ribbon CSS/visual/accessibility improvements in `ribbon.css` and related control factories.
4. Provider authoring documentation and non-breaking helper APIs where they reduce repeated provider code.
5. Test coverage for P1/P2 gaps across `docks`, `github`, `hugo`, `samples`, and Maven test ergonomics.
6. Archetype/sample updates that make ribbon contribution wiring discoverable.
7. Spec, README, release-note, and roadmap cleanup needed to make the current ribbon status discoverable.

### Out Of Scope

1. Implementing broad per-user ribbon customization beyond the schema-readiness work called out by the reviews.
2. Adding new large ribbon control families such as galleries or keytips unless a phase explicitly updates this plan first.
3. Replacing the existing ribbon SPI in one step. Breaking changes must be isolated behind a ribbon-6 design phase.
4. Changing GitHub or Hugo production behavior beyond what is required for provider/ribbon integration correctness.

## Key Invariants

1. P1 runtime and accessibility fixes must land before broad refactors or new provider expansion.
2. Shared public API or session-format changes require @core-architect and @spec-steward review before implementation.
3. CSS/theme/accessibility changes require @ui-ux-designer review and targeted TestFX coverage where behavior is observable.
4. Test infrastructure or Maven profile changes require @qa-engineer and @ops-engineer review.
5. Provider authoring work must keep feature modules able to depend on `papiflyfx-docking-api` rather than `papiflyfx-docking-docks`.
6. Documentation must state whether a finding is fixed, deliberately deferred, or superseded by a later design task.

## Workstream Map

### A. Runtime Correctness And Diagnostics

Lead: @core-architect  
Reviewers: @feature-dev, @qa-engineer, @ops-engineer, @spec-steward

Findings:

1. `review-core-architect.md` F-01 - Providers re-emit `MutableBoolState` and defeat command canonicalization.
2. `review-core-architect.md` F-02 - JavaFX command bindings leak listeners.
3. `review-core-architect.md` F-04 - No collision diagnostics for duplicate tab ids.
4. `review-core-architect.md` F-09 - Provider failures are logged but not observable to tests/telemetry.
5. `review-core-architect.md` F-11 - `syncRibbonContextFromTree` is synchronous and unbounded per FX pulse.
6. `review-core-architect.md` F-12 - `RibbonTabSpec.visibleWhen` is dropped during canonicalization.
7. `review-feature-dev.md` F-02 - Canonical command identity freezes provider-computed enabled/selected state.
8. `review-feature-dev.md` F-07 - Command id collision handling is first-wins and silent.
9. `review-ops-engineer.md` F-03 - Ribbon layout telemetry has no operator-facing switch.
10. `review-qa-engineer.md` F-02 - Provider failure isolation is implemented but untested.
11. `review-qa-engineer.md` F-06 - Rapid context churn across FX pulses is untested.

Planned outcomes:

1. Define command-state refresh semantics: either canonical commands absorb incoming state snapshots, or providers use a documented command-state cache helper.
2. Make JavaFX command binding subscriptions disposable and release them on control eviction/rebind.
3. Add duplicate tab/command diagnostics with tests and documentation for first-wins behavior.
4. Add provider-failure telemetry and a test that proves healthy providers survive a failing provider.
5. Coalesce ribbon context sync work to one refresh per FX pulse, with telemetry-backed regression coverage.
6. Decide whether post-materialization `visibleWhen` should be preserved or explicitly removed from the published spec.

### B. Accessibility, Visual Tokens, And Adaptive UX

Lead: @ui-ux-designer  
Reviewers: @core-architect, @feature-dev, @qa-engineer, @spec-steward

Findings:

1. `review-ui-ux-designer.md` F-01 - Ribbon sizing and spacing drift from shared UI tokens.
2. `review-ui-ux-designer.md` F-02 - MEDIUM mode is a compressed LARGE layout.
3. `review-ui-ux-designer.md` F-03 - Keyboard focus is invisible on several ribbon entry points.
4. `review-ui-ux-designer.md` F-04 - Collapsed group popups do not restore focus.
5. `review-ui-ux-designer.md` F-05 - Icon-only commands lack explicit accessible names.
6. `review-ui-ux-designer.md` F-06 - Disabled command state is color-only and does not mute icons.
7. `review-ui-ux-designer.md` F-07 - Contextual tab accent is lost in selected/focus states.
8. `review-ui-ux-designer.md` F-08 - Header has no overflow strategy when QAT grows.
9. `review-feature-dev.md` F-05 - Built-in providers do not expose busy/running command state.
10. `review-qa-engineer.md` F-03 - Theme switching is not covered for live ribbon chrome or open popups.
11. `review-qa-engineer.md` F-04 - Collapsed popup keyboard dismissal and focus return are missing.

Planned outcomes:

1. Add a shared ribbon metric source so CSS and Java adaptive estimates cannot drift.
2. Redesign MEDIUM presentation rules and label budgets before changing control geometry.
3. Add visible focus states for tabs, QAT buttons, group launchers, and collapsed-group buttons.
4. Restore focus to collapsed group triggers when popups hide and cover Esc/outside-click behavior.
5. Set accessible text for icon-only controls and document tooltip vs. accessible-label semantics.
6. Make disabled icon state visually distinct for SVG/octicon and decide raster-icon treatment.
7. Add contextual selected/focused styles using theme-derived accent tokens.
8. Define QAT/header overflow behavior for many pinned commands.
9. Decide whether busy/running state belongs in ribbon command API or feature-local status surfaces.

### C. Context, Capability, And Provider Authoring

Lead: @core-architect  
Reviewers: @feature-dev, @spec-steward, @qa-engineer

Findings:

1. `review-core-architect.md` F-06 - `RibbonContextAttributes` is an untyped string bag.
2. `review-core-architect.md` F-10 - `RibbonContext.capability(Class)` linear scan blurs registration rules.
3. `review-feature-dev.md` F-01 - Provider authoring is copy-heavy and under-documented.
4. `review-feature-dev.md` F-03 - Contextual tab activation relies on Hugo-specific heuristics.
5. `review-feature-dev.md` F-04 - Capability exposure is implicit and tied to the content root node.
6. `review-feature-dev.md` F-06 - Code, tree, and media lack action contracts and contextual ids.
7. `review-ops-engineer.md` F-01 - Archetype does not expose ribbon SPI wiring.
8. `review-qa-engineer.md` F-01 - Floating-window ribbon context has no direct regression test.
9. `review-qa-engineer.md` F-05 - Hugo contextual heuristic matrix is only partially covered.
10. `review-qa-engineer.md` F-10 - Provider-level Fx coverage is sample-driven, not module-local.
11. `review-spec-steward.md` F-05 - Provider-authoring guidance is too thin.

Planned outcomes:

1. Design typed ribbon context attribute keys and a namespacing rule.
2. Add an explicit capability contribution path or document root-node interface exposure as the supported convention.
3. Document provider ids, command ids, ServiceLoader registration, context metadata, QAT implications, and required tests.
4. Add non-breaking provider builder/helper APIs only after the authoring guide identifies stable repeated patterns.
5. Add an archetype or generated sample path that demonstrates `RibbonDockHost`, a minimal provider, and ServiceLoader registration.
6. Add focused tests for floating-window context, Hugo heuristic branches, and provider-module Fx coverage where it adds value beyond samples.

### D. Test, Build, And Performance Guardrails

Lead: @qa-engineer  
Reviewers: @ops-engineer, @core-architect, @ui-ux-designer

Findings:

1. `review-ops-engineer.md` F-02 - Surefire JavaFX/TestFX flags are duplicated.
2. `review-qa-engineer.md` F-01 - Floating-window ribbon context lacks direct coverage.
3. `review-qa-engineer.md` F-02 - Provider failure isolation lacks tests.
4. `review-qa-engineer.md` F-03 - Theme switching lacks ribbon coverage.
5. `review-qa-engineer.md` F-04 - Collapsed popup keyboard dismissal/focus return is missing.
6. `review-qa-engineer.md` F-05 - Hugo heuristic matrix is partially covered.
7. `review-qa-engineer.md` F-06 - Rapid context churn is untested.
8. `review-qa-engineer.md` F-07 - Ribbon fixture code is duplicated.
9. `review-qa-engineer.md` F-08 - Focused Maven selectors need `surefire.failIfNoSpecifiedTests=false`.
10. `review-qa-engineer.md` F-09 - Refresh performance has no scale guardrail.
11. `review-qa-engineer.md` F-10 - Provider-level Fx coverage is sample-driven.

Planned outcomes:

1. Introduce a small `RibbonTestSupport` helper after the first two new tests reveal the useful shared shape.
2. Centralize or document Maven/TestFX flags for focused ribbon runs.
3. Add tests for provider failure, floating-window context, theme toggles, popup keyboard behavior, Hugo heuristic coverage, and fast context churn.
4. Add an opt-in benchmark-tagged ribbon refresh/adaptive layout guard only after a concrete budget is defined.
5. Keep default CI fast; performance/benchmark tests must remain opt-in unless release criteria change.

### E. API Shape And Breaking-Change Design Debt

Lead: @core-architect  
Reviewers: @feature-dev, @ui-ux-designer, @spec-steward

Findings:

1. `review-core-architect.md` F-05 - `PapiflyCommand` violates Interface Segregation for push buttons.
2. `review-core-architect.md` F-06 - `RibbonContextAttributes` needs typed/namespaced keys.
3. `review-core-architect.md` F-07 - `RibbonManager` has too many responsibilities.
4. `review-core-architect.md` F-08 - Sealed `RibbonControlSpec` forces central edits for new control types.
5. `review-core-architect.md` F-13 - `BoolState` duplicates JavaFX property semantics with weaker guarantees.
6. `review-core-architect.md` F-14 - Per-user customization requires a schema bump and compatibility policy.
7. `review-feature-dev.md` F-05 - Busy/running command state is not expressible.

Planned outcomes:

1. Produce a ribbon-6 design note before implementing breaking API changes.
2. Split immediate non-breaking fixes from larger SPI redesign candidates.
3. Decide whether `PapiflyCommand`, `BoolState`, `RibbonControlSpec`, and customization schema changes are worth a coordinated compatibility break.
4. Define session forward-compatibility behavior before customization fields are introduced.
5. Extract `RibbonManager` responsibilities only after command-state and provider-diagnostics fixes are stable.

### F. Documentation, Roadmap, And Release Hygiene

Lead: @spec-steward  
Reviewers: @core-architect, @feature-dev, @ops-engineer, @qa-engineer

Findings:

1. `review-core-architect.md` F-03 - Session docs do not match persisted shape.
2. `review-core-architect.md` F-14 - Customization path needs schema policy.
3. `review-ops-engineer.md` F-04 - Ribbon 2 breaking changes lack release notes.
4. `review-qa-engineer.md` F-08 - Focused Maven selector flag should be documented or aliased.
5. `review-spec-steward.md` F-01 - No single current ribbon status source.
6. `review-spec-steward.md` F-02 - Ribbon 1 persistence docs are superseded but not marked historical.
7. `review-spec-steward.md` F-03 - Several ribbon spec landing pages are prompt-only.
8. `review-spec-steward.md` F-04 - Ribbon 5 review prompts contain stale baseline assumptions.
9. `review-spec-steward.md` F-05 - Provider-authoring guidance is too thin.
10. `review-spec-steward.md` F-06 - Manual checks need explicit disposition.
11. `review-feature-dev.md` F-01 - Provider authoring needs docs/helpers.
12. `review-feature-dev.md` F-06 - New content modules need action/contextual guidance.
13. `review-feature-dev.md` F-07 - Command id namespace convention needs documentation.
14. `review-ops-engineer.md` F-01 - Archetype docs/scaffold should expose ribbon wiring.

Planned outcomes:

1. Add a current ribbon status/index document or README section linking all ribbon iterations and authoritative docs.
2. Mark superseded Ribbon 1 persistence material and update session-shape docs to `extensions.ribbon`.
3. Normalize ribbon iteration landing pages to status-oriented summaries.
4. Add release/migration notes for Ribbon 2 breaking changes.
5. Add provider-authoring guidance with id namespace, ServiceLoader, capability, contextual-tab, QAT, and test checklist.
6. Require future manual criteria to be marked `performed`, `replaced by <test>`, or `deferred to <owner/date/reason>`.

## Implementation Phases

### Phase 0 - Planning And Triage

Lead: @spec-steward  
Status target: complete before implementation starts.

Tasks:

1. Create this `plan.md` and companion `progress.md`.
2. Verify every review finding is mapped to one workstream.
3. Mark P1 items as first implementation candidates.
4. Record deferral rules for breaking API changes.

Acceptance:

- Every review finding ID appears in this plan.
- `progress.md` has an initial status and next-task list.

### Phase 1 - P1 Runtime And Accessibility Stabilization

Lead: @core-architect with @ui-ux-designer and @qa-engineer support.

Tasks:

1. Fix command-state canonicalization semantics and add regression tests.
2. Add disposable JavaFX command bindings and listener leak coverage.
3. Add provider/collision diagnostics and provider-failure tests.
4. Add visible focus states, popup focus restoration, accessible names for icon-only controls, and disabled icon styling.
5. Add theme-switching and popup keyboard TestFX coverage.

Acceptance:

- All P1 findings in core, feature, UI/UX, and QA reviews have either landed fixes or explicit design deferrals.
- Focused `docks` ribbon tests pass in headless mode.
- Accessibility changes are reviewed by @ui-ux-designer.

### Phase 2 - Context And Provider Authoring Foundation

Lead: @core-architect and @feature-dev.

Tasks:

1. Design typed context attributes and explicit capability contribution semantics.
2. Add or document capability registration rules, including floating-window behavior.
3. Write provider-authoring guidance before adding broad helpers.
4. Add Hugo heuristic table tests and floating-window context tests.
5. Add a minimal archetype/sample provider path or scaffold if the docs alone are insufficient.

Acceptance:

- A new feature module can follow documented steps to contribute a ribbon provider without reading runtime internals.
- Contextual tab activation has an explicit metadata path, with legacy heuristics clearly documented as fallback.
- Floating-window capability resolution is covered by a focused test.

### Phase 3 - Build, Fixture, And Performance Guardrails

Lead: @qa-engineer and @ops-engineer.

Tasks:

1. Centralize or document common TestFX/Surefire focused-run flags.
2. Introduce shared ribbon test support if test duplication persists after Phase 1 and Phase 2 tests.
3. Add opt-in ribbon performance/scale guardrails only after agreeing on a useful budget.
4. Add provider-module Fx coverage where sample-level tests are too broad.

Acceptance:

- Focused ribbon test commands are documented and usable with `-am`.
- Repeated fixture code is reduced or explicitly accepted.
- Any benchmark is opt-in and excluded from default CI unless a release gate requires it.

### Phase 4 - API Design And Compatibility Decisions

Lead: @core-architect with @spec-steward.

Tasks:

1. Decide whether `PapiflyCommand` should split push-button and toggle contracts.
2. Decide whether `BoolState` should change to a subscription-returning observable abstraction.
3. Design the non-sealed/visitor path for future ribbon control types.
4. Split `RibbonManager` responsibilities only after correctness fixes are stable.
5. Define session forward-compatibility policy for future customization.

Acceptance:

- Breaking changes are captured in a dedicated ribbon-6 design note before code changes.
- Migration notes and release notes are drafted alongside any accepted API break.

### Phase 5 - Documentation And Roadmap Closure

Lead: @spec-steward.

Tasks:

1. Add current ribbon status/index documentation.
2. Update session persistence documentation and superseded notes.
3. Normalize dated ribbon spec landing pages.
4. Add provider-authoring guide or README appendix.
5. Add release/migration note for Ribbon 2 breaks and any new changes from this plan.
6. Record validation and manual-check disposition for each implemented phase.

Acceptance:

- A contributor can find current ribbon status, provider authoring, session shape, and validation commands from documented entry points.
- `progress.md` lists implemented phases, validation commands, residual risks, and reviewer handoff.

## Validation Strategy

Run the narrowest relevant checks first, then broaden:

1. Core ribbon unit/Fx tests:
   - `./mvnw -pl papiflyfx-docking-docks -am '-Dtest=Ribbon*Test,CommandRegistryTest' -Dsurefire.failIfNoSpecifiedTests=false -Dtestfx.headless=true test`
2. Feature provider tests:
   - `./mvnw -pl papiflyfx-docking-github,papiflyfx-docking-hugo -am -Dtestfx.headless=true test`
3. Samples smoke/ribbon tests:
   - `./mvnw -pl papiflyfx-docking-samples -am -Dtest=SamplesSmokeTest -Dsurefire.failIfNoSpecifiedTests=false -Dtestfx.headless=true test`
   - `./mvnw -pl papiflyfx-docking-samples -am '-Dtest=*Ribbon*FxTest' -Dsurefire.failIfNoSpecifiedTests=false -Dtestfx.headless=true test`
4. Build/test profile changes:
   - `./mvnw -Dtestfx.headless=true test` when Maven/Surefire configuration changes are made.
5. Documentation-only changes:
   - source/spec review plus link/path sanity checks.

Manual checks must be recorded as one of:

1. performed, with date and scenario;
2. replaced by a named automated test;
3. deferred, with owner/date/reason.

## Completion Criteria

This plan is complete when:

1. Every P1 finding has a fix, test, or explicit design deferral approved by the owning reviewers.
2. Every P2/P3 finding is either implemented, moved into a named future plan, or explicitly closed as accepted risk.
3. Public API/session changes have migration notes.
4. Test and documentation commands are recorded in `progress.md`.
5. `progress.md` has a final handoff snapshot.

## Handoff Snapshot

Lead Agent: `@spec-steward`  
Task Scope: consolidate all Ribbon 5 review findings into an implementation plan  
Impacted Modules: `spec/**` for this planning pass; later phases may touch `papiflyfx-docking-api`, `papiflyfx-docking-docks`, `papiflyfx-docking-github`, `papiflyfx-docking-hugo`, `papiflyfx-docking-samples`, `papiflyfx-docking-archetype`, root `pom.xml`, and docs  
Files Changed:
- `spec/papiflyfx-docking-docks/2026-04-23-0-ribbon-5/plan.md`  
Key Invariants:
- all 49 review findings are mapped
- implementation must follow the phase owners and review gates
- breaking API/session work must be designed before coding
Validation Performed: source review of all six review documents  
Open Risks / Follow-ups: implementation not started; see `progress.md`  
Required Reviewers: `@core-architect`, `@feature-dev`, `@ops-engineer`, `@ui-ux-designer`, `@qa-engineer`, `@spec-steward`
