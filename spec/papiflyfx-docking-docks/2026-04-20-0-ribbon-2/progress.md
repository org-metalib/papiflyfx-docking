# Progress — Docking Ribbon 2

**Status:** Phase 3 implementation complete; required on-host validation green  
**Current Milestone:** Phase 3 — layout and rendering efficiency  
**Priority:** P1 (High)  
**Lead Agent:** @core-architect  
**Required Reviewers:** @ui-ux-designer, @feature-dev, @qa-engineer, @ops-engineer, @spec-steward  
**Compatibility Stance:** Compatibility is not a concern for implementation; deliberate breakage is allowed.

## Completion summary

- Research: 100%
- Planning: 100%
- Phase 1 — API contracts: 100%
- Phase 2 — Runtime command architecture: 100%
- Phase 3 — Layout and rendering efficiency: 100%
- Phase 4 — Persistence extension generalization: 0%
- Phase 5 — Provider migration and test closure: 0%

## Accomplishments

- [2026-04-20] Completed Phase 1 API refactor: UI-neutral `BoolState`/`MutableBoolState`, typed `RibbonContext` capabilities, `collapseOrder` rename, and provider/sample compile fixes.
- [2026-04-21] Completed Phase 2 runtime command architecture: long-lived `CommandRegistry`, canonical command identities through `RibbonManager.refresh()`, and ID-first Quick Access Toolbar persistence/rendering.
- [2026-04-21] Ran the required on-host preflight validation for Phases 1 and 2 before starting new refactors:
  - `./mvnw -pl papiflyfx-docking-api,papiflyfx-docking-docks -am compile` → `BUILD SUCCESS`
  - `./mvnw -pl papiflyfx-docking-api,papiflyfx-docking-docks -am -Dtestfx.headless=true test` → `BUILD SUCCESS` (`79` tests before Phase 3; `84` after Phase 3)
  - `./mvnw -pl papiflyfx-docking-samples,papiflyfx-docking-github,papiflyfx-docking-hugo -am compile` → `BUILD SUCCESS`
- [2026-04-21] Phase 3.1 — Reworked ribbon rendering to reconcile cached tab/group/control nodes instead of recreating them on every refresh:
  - `RibbonTabStrip` now reuses tab buttons keyed by tab id and emits deterministic cache-hit/rebuild telemetry.
  - `Ribbon` now caches `RibbonGroup` instances per `(tabId, groupId)` and only requests adaptive relayout when selection/structure actually changes.
  - `RibbonGroup` now caches control nodes per `(controlId, RibbonGroupSizeMode)` and reuses them across steady-state refreshes and collapse transitions.
- [2026-04-21] Phase 3.1 — Replaced the old “reset every group to LARGE then shrink” layout pass with an incremental shrink/grow policy:
  - shrink uses the Phase 1 invariant (`smaller collapseOrder` collapses earlier; ties collapse from higher `order` downward)
  - grow restores in reverse order
  - steady-state refreshes no longer schedule avoidable `Platform.runLater(...)` batches when the visible structure is unchanged
- [2026-04-21] Phase 3.2 — Added internal `RibbonLayoutTelemetry` plus package-visible `RibbonLayoutTelemetryRecorder` for deterministic event capture:
  - `tabRebuild(tabId, reason)`
  - `groupRebuild(groupId, reason)`
  - `controlRebuild(controlId, reason)`
  - `collapseTransition(groupId, from, to)`
  - `nodeCacheHit(kind, id)` / `nodeCacheMiss(kind, id)`
- [2026-04-21] Phase 3.2 — Wired telemetry to the new cache invariants:
  - identical refreshes now emit cache hits without rebuilds
  - structural deltas rebuild only the changed surface
  - collapse transitions emit ordered `from`/`to` state changes
- [2026-04-21] Phase 3.3 — Fixed the pre-existing accumulator double-merge bug by removing the in-constructor `merge(initial)` from `TabAccumulator` and `GroupAccumulator`.
- [2026-04-21] Phase 3.3 — Added a targeted `RibbonManagerTest` regression that proves a single tab + single group contribution yields exactly one merged control.
- [2026-04-21] Phase 3.4 — Introduced `RibbonIconLoader` with SVG support and graceful fallback:
  - keeps the SPI unchanged (`RibbonIconHandle` string paths only)
  - supports existing octicon pseudo-URIs
  - renders path-based SVG resources through JavaFX `SVGPath`
  - logs `WARNING` and falls back to the existing raster path / alternate icon handle when SVG parsing fails
  - intentionally uses no new Maven dependency; rationale captured in `adr-0001-svg-icons.md`
- [2026-04-21] Phase 3.5 — Expanded deterministic tests in `papiflyfx-docking-docks`:
  - steady-state node-cache reuse
  - structural control-add telemetry
  - full collapse/restore transition ordering
  - accumulator merge regression
  - SVG parse failure with raster fallback

## Validation status

### Phase 1 + Phase 2 preflight

- `2026-04-21` — `./mvnw -pl papiflyfx-docking-api,papiflyfx-docking-docks -am compile` → `BUILD SUCCESS`
- `2026-04-21` — `./mvnw -pl papiflyfx-docking-api,papiflyfx-docking-docks -am -Dtestfx.headless=true test` → `BUILD SUCCESS`
- `2026-04-21` — `./mvnw -pl papiflyfx-docking-samples,papiflyfx-docking-github,papiflyfx-docking-hugo -am compile` → `BUILD SUCCESS`

### Phase 3 validation

- `2026-04-21` — `./mvnw -pl papiflyfx-docking-api,papiflyfx-docking-docks -am compile` → `BUILD SUCCESS`
- `2026-04-21` — `./mvnw -pl papiflyfx-docking-api,papiflyfx-docking-docks -am -Dtestfx.headless=true test` → `BUILD SUCCESS`
  - `84` tests, `0` failures, `0` errors
  - expected warning coverage exercised:
    - malformed SVG icon test logs a `WARNING` before raster fallback
    - existing layout/session fallback tests still log their intentional adapter warnings
- `2026-04-21` — `./mvnw -pl papiflyfx-docking-github,papiflyfx-docking-hugo,papiflyfx-docking-samples -am compile` → `BUILD SUCCESS`
- `2026-04-21` — `./mvnw -pl papiflyfx-docking-samples -am -Dtest=SamplesSmokeTest -Dtestfx.headless=true test` → `BUILD FAILURE`
  - cause: reactor stopped in `papiflyfx-docking-api` because Surefire found no test matching `SamplesSmokeTest`
- `2026-04-21` — `./mvnw -pl papiflyfx-docking-samples -am -Dtest=SamplesSmokeTest -Dsurefire.failIfNoSpecifiedTests=false -Dtestfx.headless=true test` → `BUILD SUCCESS`
  - `SamplesSmokeTest`: `12` tests, `0` failures, `0` errors

### Packaging validation

- Not required for this session.
- `clean package` was not run because the SVG loader intentionally added no new dependency.

## New and changed invariants

- Canonical command identity remains owned by `RibbonManager` and `CommandRegistry`; Phase 3 assumes that if the manager re-emits the same canonical command id, command-backed control nodes can be reused safely.
- `RibbonTabStrip` now treats tab button nodes as persistent render surfaces keyed by tab id. Identical refreshes produce `nodeCacheHit(TAB, tabId)` and no tab rebuild.
- `Ribbon` now treats `RibbonGroup` instances as persistent render surfaces keyed by `(tabId, groupId)`. Identical refreshes produce `nodeCacheHit(GROUP, tabId/groupId)` and no group rebuild.
- `RibbonGroup` now treats non-collapsed control nodes as persistent render surfaces keyed by `(tabId, groupId, controlId, RibbonGroupSizeMode)`. Identical refreshes in the same size mode produce `nodeCacheHit(CONTROL, ...)` and no control rebuild.
- Structural deltas at the group level may reuse the same `RibbonGroup` node while invalidating only the changed control ids. In the control-add path, unchanged controls remain cache hits and only the new control emits `controlRebuild(..., STRUCTURAL)`.
- Adaptive layout no longer resets every group to `LARGE` before recomputing. Shrink and grow are incremental and deterministic from the current size-state graph.
- The Phase 1 collapse contract is authoritative: smaller `collapseOrder` values collapse earlier. The prompt text also mentioned “highest collapseOrder first” in one test bullet; this implementation follows the Phase 1/Javadoc contract instead.
- `TabAccumulator` and `GroupAccumulator` constructors no longer merge their initial contribution eagerly. The only merge now happens in the outer `computeIfAbsent(...).merge(...)` flow.
- SVG icon support is best-effort and internal. Parsing/render failure must never propagate to providers; the loader logs a `WARNING` and falls back to raster loading.

## Open risks and follow-ups

- `RibbonIconLoader` currently supports path-based SVG documents, which is sufficient for icon-grade assets but not a general SVG renderer. Multi-element icons that rely on richer SVG features may still fall back to raster handles.
- Hidden contextual tabs keep their cached `RibbonGroup` instances so they can reappear without reconstruction. This is intentional for Phase 3 efficiency, but it means cache lifetime is tied to the ribbon host lifetime rather than visible-tab lifetime.
- `ACTIVE_CONTENT_NODE` is still populated in `RibbonContextAttributes` for the transitional provider migration phase and remains a Phase 5 cleanup item.
- Phase 4 persistence-extension generalization is still pending. Phase 3 deliberately kept the existing `RibbonSessionData` shape intact.

## Next tasks

1. Phase 4 — design the namespaced session-extension payload model in `RibbonSessionData` / `DockSessionStateContributor`.
2. Phase 4 — add serializer shape-validation helpers before touching the session schema.
3. Phase 5 — migrate remaining providers off `ACTIVE_CONTENT_NODE` and onto typed capabilities.
4. Phase 5 — expand provider-facing docs with the new cache/telemetry invariants and the FX-thread ownership rule for `CommandRegistry`.

## Handoff snapshot — Phase 3

Lead Agent: `@core-architect`  
Task Scope: Ribbon 2 Phase 3 — adaptive layout/node reuse, layout telemetry, accumulator merge fix, SVG icon fallback, and deterministic regression coverage.  
Impacted Modules: `papiflyfx-docking-docks` (runtime + tests), `spec/papiflyfx-docking-docks/2026-04-20-0-ribbon-2/**`.

Files Changed — Phase 3 runtime:

- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/Ribbon.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonTabStrip.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonGroup.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonGroupSizeMode.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonControlFactory.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonIconLoader.java` (new)
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonLayoutTelemetry.java` (new)
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonLayoutTelemetryRecorder.java` (new)
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonManager.java`

Files Changed — Phase 3 tests:

- `papiflyfx-docking-docks/src/test/java/org/metalib/papifly/fx/docks/ribbon/RibbonAdaptiveLayoutFxTest.java`
- `papiflyfx-docking-docks/src/test/java/org/metalib/papifly/fx/docks/ribbon/RibbonManagerTest.java`

Files Changed — Spec:

- `spec/papiflyfx-docking-docks/2026-04-20-0-ribbon-2/progress.md`
- `spec/papiflyfx-docking-docks/2026-04-20-0-ribbon-2/adr-0001-svg-icons.md`

Reviewer focus:

- `@ui-ux-designer` — verify the incremental collapse/grow ordering still feels correct in the live ribbon and that collapsed-group affordances remain visually stable when switching widths repeatedly.
- `@feature-dev` — verify provider-authored controls with stable canonical command ids benefit from the cache path and that no provider assumes per-refresh control reconstruction.
- `@qa-engineer` — scrutinize the new telemetry invariants and the headless `RibbonAdaptiveLayoutFxTest` coverage, especially the “identical refresh => hits only” expectation.
- `@ops-engineer` — confirm the validation command record is sufficient and that the documented `SamplesSmokeTest` Surefire workaround is acceptable for reactor runs.
- `@spec-steward` — confirm the Phase 3 handoff captures the prompt ambiguity around collapse ordering and the “no new dependency” SVG decision clearly enough for later phases.
