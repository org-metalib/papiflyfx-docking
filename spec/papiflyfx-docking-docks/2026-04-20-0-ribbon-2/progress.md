# Progress — Docking Ribbon 2

**Status:** Phase 1 (API contracts) implementation complete; validation pending
**Current Milestone:** Phase 1 — UI-neutral API, typed capability model, collapse semantics, intentional-breakage docs
**Priority:** P1 (High)
**Lead Agent:** @core-architect
**Required Reviewers:** @ui-ux-designer, @feature-dev, @qa-engineer, @ops-engineer, @spec-steward
**Compatibility Stance:** Compatibility is not a concern for implementation; deliberate breakage is allowed.

## Completion summary

- Research: 100%
- Planning: 100%
- Phase 1 — API contracts: 100% (code changes landed; build validation still outstanding)
- Phase 2 — Runtime command architecture: 0%
- Phase 3 — Layout and rendering efficiency: 0%
- Phase 4 — Persistence extension generalization: 0%
- Phase 5 — Provider migration and test closure: 0%

## Accomplishments

- [2026-04-20] Created ribbon-2 research dossier with concept/design/implementation gap analysis.
- [2026-04-20] Created phased implementation plan with acceptance criteria and validation strategy.
- [2026-04-20] Established risks/mitigations and module ownership boundaries for implementation start.
- [2026-04-20] Recorded explicit non-compatibility policy for API/session/provider changes in ribbon-2 specs.
- [2026-04-20] Phase 1.1 — Introduced UI-neutral `BoolState` / `MutableBoolState` in `papiflyfx-docking-api`; refactored `PapiflyCommand` so `enabled`/`selected` are `BoolState` components (no JavaFX types in the public contract).
- [2026-04-20] Phase 1.1 — Added `JavaFxCommandBindings` adapter in `papiflyfx-docking-docks` to bridge `BoolState` to read-only `BooleanProperty` (for `disableProperty`) and to bidirectional `BooleanProperty` (for toggle `selectedProperty`) with FX-thread dispatch and a re-entrancy guard.
- [2026-04-20] Phase 1.2 — Added typed capability registry to `RibbonContext` (`Map<Class<?>, Object> capabilities`, `capability(Class)`, `withCapability(Class, T)`); wired `DockManager#buildRibbonContext` to publish the active content node under its concrete class.
- [2026-04-20] Phase 1.2 — Deprecated `RibbonContextAttributes.ACTIVE_CONTENT_NODE` for removal with migration note pointing providers to `RibbonContext.capability(Class)`. Transitional attribute still populated so existing providers compile.
- [2026-04-20] Phase 1.3 — Renamed `RibbonGroupSpec.reductionPriority` to `collapseOrder` with explicit Javadoc convention ("smaller values collapse earlier; recommended 10/20/30"). Updated runtime comparators (`RibbonManager`, `Ribbon.applyAdaptiveLayout`) and accumulator fields accordingly.
- [2026-04-20] Phase 1.4 — Documented intentional breakage: `papiflyfx-docking-api/…/package-info.java` call-out; Javadoc notes in `PapiflyCommand`, `RibbonContext`, and `RibbonContextAttributes`.
- [2026-04-20] Propagated compile fixes across touched consumers: `SampleRibbonProvider`, `GitHubRibbonProvider`, `HugoRibbonProvider` (switched from `SimpleBooleanProperty` to `MutableBoolState` and renamed reduction-priority constants to `COLLAPSE_LATE/MID/EARLY`). Updated provider tests to use `command.enabled()` instead of `command.enabledProperty()`.

## Intentional breakages (Ribbon 2)

- `PapiflyCommand` component types changed: `BooleanProperty enabledProperty` / `BooleanProperty selectedProperty` → `BoolState enabled` / `BoolState selected`. The accessor methods are now `enabled()` / `selected()` (record accessors), not `enabledProperty()` / `selectedProperty()`.
- `RibbonContext` gained a 5th component (`Map<Class<?>, Object> capabilities`). Existing callers that construct the record directly now must pass the map (a 4-arg convenience constructor is provided to default it to empty).
- `RibbonContextAttributes.ACTIVE_CONTENT_NODE` is marked `@Deprecated(forRemoval = true)`. Providers must migrate to `RibbonContext.capability(SomeApi.class)`.
- `RibbonGroupSpec.reductionPriority` renamed to `collapseOrder`; accessor and constructor parameter name both changed.
- All JavaFX types removed from `papiflyfx-docking-api/.../ribbon/`. Downstream code that relied on JavaFX observables coming from the API must switch to `BoolState` listeners or use the runtime's `JavaFxCommandBindings` adapter (package-private in docks).

## Next tasks

1. Validate Phase 1: `./mvnw -pl papiflyfx-docking-api,papiflyfx-docking-docks -am compile` and `-am -Dtestfx.headless=true test`. If green, also `./mvnw -pl papiflyfx-docking-github,papiflyfx-docking-hugo -am compile`.
2. Phase 2 bootstrap — introduce `CommandRegistry` SPI and QAT ID-first resolution (not in Phase 1 scope; ready to start once Phase 1 is validated).
3. Begin provider migration off `ACTIVE_CONTENT_NODE` to typed capability access (Phase 5 seed; do not start until Phase 2 is underway).
4. Prepare regression test list for collision diagnostics, capability-based context, and new extension payload schema.

## Open risks

- `JavaFxCommandBindings` bridges `BoolState` to `BooleanProperty` via an internal `ReadOnlyBooleanWrapper`. The wrapper relies on `Platform.runLater` for off-FX-thread updates; if a provider mutates `BoolState` very frequently, this can create runLater churn. Mitigation: batched updates in Phase 2 when the command registry owns state transitions.
- `ACTIVE_CONTENT_NODE` still populated alongside the typed capability map for one transitional phase. If providers ignore the deprecation we carry the attribute longer than planned. Mitigation: Phase 5 migration gate, remove attribute then.
- Session schema unchanged in Phase 1; existing ribbon session payloads still load. Phase 4 will introduce schema changes by design.
- Adaptive layout refactor deferred to Phase 3; semantic rename of `collapseOrder` should not alter observable layout behavior, but visual diffs should be spot-checked once tests run.

## Validation status

- Code-level review: complete for Phase 1.
- Build validation: **not yet executed** (Phase 1.5). Commands queued:
  - `./mvnw -pl papiflyfx-docking-api,papiflyfx-docking-docks -am compile`
  - `./mvnw -pl papiflyfx-docking-api,papiflyfx-docking-docks -am -Dtestfx.headless=true test`
  - `./mvnw -pl papiflyfx-docking-github,papiflyfx-docking-hugo -am compile`

## Handoff snapshot

Lead Agent: `@core-architect`
Task Scope: Ribbon 2 Phase 1 — UI-neutral API, typed capability model, collapse semantics, documented breakage
Impacted Modules: `papiflyfx-docking-api`, `papiflyfx-docking-docks`, `papiflyfx-docking-github` (compile-fix only), `papiflyfx-docking-hugo` (compile-fix only), `papiflyfx-docking-samples` (compile-fix only), `spec/papiflyfx-docking-docks/2026-04-20-0-ribbon-2/**`

Files Changed — API:
- `papiflyfx-docking-api/src/main/java/org/metalib/papifly/fx/api/ribbon/BoolState.java` (new)
- `papiflyfx-docking-api/src/main/java/org/metalib/papifly/fx/api/ribbon/MutableBoolState.java` (new)
- `papiflyfx-docking-api/src/main/java/org/metalib/papifly/fx/api/ribbon/PapiflyCommand.java` (rewritten — no JavaFX types)
- `papiflyfx-docking-api/src/main/java/org/metalib/papifly/fx/api/ribbon/RibbonContext.java` (added capabilities component + typed accessors)
- `papiflyfx-docking-api/src/main/java/org/metalib/papifly/fx/api/ribbon/RibbonContextAttributes.java` (deprecated `ACTIVE_CONTENT_NODE`)
- `papiflyfx-docking-api/src/main/java/org/metalib/papifly/fx/api/ribbon/RibbonGroupSpec.java` (renamed `reductionPriority` → `collapseOrder`)
- `papiflyfx-docking-api/src/main/java/org/metalib/papifly/fx/api/ribbon/package-info.java` (documented breakage)

Files Changed — Runtime:
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/JavaFxCommandBindings.java` (new adapter)
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonControlFactory.java` (bind via adapter)
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonGroup.java` (bind via adapter)
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonManager.java` (collapse-order rename)
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/Ribbon.java` (collapse-order comparator)
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/DockManager.java` (`buildRibbonContext` publishes typed capability)

Files Changed — Provider/test compile fixes (not full migration):
- `papiflyfx-docking-samples/src/main/java/org/metalib/papifly/fx/samples/docks/ribbon/SampleRibbonProvider.java`
- `papiflyfx-docking-github/src/main/java/org/metalib/papifly/fx/github/ribbon/GitHubRibbonProvider.java`
- `papiflyfx-docking-hugo/src/main/java/org/metalib/papifly/fx/hugo/ribbon/HugoRibbonProvider.java`
- `papiflyfx-docking-github/src/test/java/org/metalib/papifly/fx/github/ribbon/GitHubRibbonProviderTest.java`
- `papiflyfx-docking-hugo/src/test/java/org/metalib/papifly/fx/hugo/ribbon/HugoRibbonProviderTest.java`

Files Changed — Spec:
- `spec/papiflyfx-docking-docks/2026-04-20-0-ribbon-2/progress.md` (this file)

Reviewer focus:
- `@ui-ux-designer` — confirm `BoolState`-based toggle bridging preserves visual pin/collapse behavior; confirm `collapseOrder` semantics match the intended UI language.
- `@feature-dev` — confirm `JavaFxCommandBindings` suits provider authoring; confirm typed capability pattern for future providers.
- `@qa-engineer` — re-run ribbon-centric test suites; add tests for new capability lookup paths.
- `@ops-engineer` — confirm no external behavior affecting bundling/launch scripts.
- `@spec-steward` — confirm breakage list is complete and aligned with compatibility stance.

Required Reviewer: `@spec-steward`
