# Ribbon Current Status

**Status:** current as of Ribbon 9 side-placement implementation
**Lead:** @spec-steward
**Required reviewers for future changes:** @core-architect, @feature-dev, @ops-engineer, @qa-engineer; add @ui-ux-designer for visual/accessibility behavior

This is the canonical status entry point for the PapiflyFX ribbon stream. Dated iteration directories remain trace records, but this file describes the current implementation baseline and where to find authoritative authoring/session guidance.

## Current Baseline

Ribbon 9 is the current implemented baseline. It keeps the Ribbon 6 runtime decisions and adds host-configurable ribbon placement:

- `RibbonPlacement` supports `TOP`, `LEFT`, `RIGHT`, and `BOTTOM`.
- `RibbonDockHost` and `Ribbon` expose `placementProperty()`, `getPlacement()`, and `setPlacement(...)`.
- `TOP` remains the default for existing hosts and saved sessions without placement.
- Left and right placements render a vertical edge tab strip plus an inner command content pane while keeping providers placement-agnostic.
- `CommandRegistry` keeps stable command identity and first metadata while refreshing enabled state, toggle selected state, and action dispatch from later provider emissions.
- JavaFX command bindings are disposable, including rebuilt controls, QAT buttons, launchers, collapsed popups, and cache eviction paths.
- Provider failures, duplicate tab ids, and duplicate command metadata conflicts are diagnostic and test-observable.
- Ribbon focus, collapsed-popup focus return, icon-only accessible names, disabled icon treatment, contextual accent styling, and live theme switching have focused coverage.
- Typed `RibbonAttributeKey<T>` metadata and explicit `RibbonAttributeContributor` / `RibbonCapabilityContributor` contracts are available while raw string attributes remain compatible.
- Floating active-content context resolution is covered.
- Hugo contextual tabs prefer explicit content metadata and retain legacy heuristics as fallback.
- Action-only commands use `RibbonCommand`; toggle-capable commands use `RibbonToggleCommand`.
- Boolean state exposes subscription-returning `RibbonBooleanState#subscribe(...)`.
- `PapiflyCommand`, `BoolState`, and `MutableBoolState` have been removed; providers use the Ribbon 6 contracts directly.
- Built-in control handling is centralized through package-private strategy/render-plan dispatch.
- `QuickAccessState` owns QAT id state behind the stable `RibbonManager` facade.
- Focused Maven selector guidance uses `-Dsurefire.failIfNoSpecifiedTests=false`.

## Authoritative Docs

- Provider authoring: [ribbon-provider-authoring.md](ribbon-provider-authoring.md)
- Release/migration notes: [ribbon-release-notes.md](ribbon-release-notes.md)
- Ribbon 6 design candidates: [2026-04-23-0-ribbon-6/ribbon-6-design.md](2026-04-23-0-ribbon-6/ribbon-6-design.md)
- Runtime module README: [../../papiflyfx-docking-docks/README.md](../../papiflyfx-docking-docks/README.md)

## Session Contract

Current ribbon state is a dock-session extension under `extensions.ribbon`:

- `extensions.ribbon.minimized`
- `extensions.ribbon.selectedTabId`
- `extensions.ribbon.quickAccessCommandIds`
- `extensions.ribbon.placement`

QAT persistence is id-first. Hosts persist `RibbonManager#getQuickAccessCommandIds()`; `getQuickAccessCommands()` is derived from currently contributed commands and is not the source of truth. A contextual command id may remain pinned while hidden and resolve again when the provider later contributes the command.

Current decode behavior reads known fields and ignores unknown fields under `extensions.ribbon`. Missing, unknown, or malformed placement values fall back to `TOP` only and do not invalidate minimized state, selected tab id, or QAT ids. Other malformed known fields remain strict for the ribbon extension and should be logged/isolated without aborting core dock-session restore. The current runtime does not preserve unknown ribbon customization fields across save because capture writes the known runtime state only.

Future customization policy is design-only in [Ribbon 6 design](2026-04-23-0-ribbon-6/ribbon-6-design.md): keep `extensions.ribbon`, preserve QAT id-first semantics, ignore unknown fields on decode, keep malformed known fields strict/isolated, and define unknown customization round-trip policy before any customization UI/schema ships.

## Iteration Status

| Iteration | Status | Current relevance |
| --- | --- | --- |
| [Ribbon 1](2026-04-19-0-ribbon/README.md) | Historical baseline | Introduced the ribbon shell, QAT, contextual tabs, adaptive layout, and session persistence. Its top-level `ribbon` JSON examples are superseded by `extensions.ribbon`. |
| [Ribbon 2](2026-04-20-0-ribbon-2/README.md) | Implemented breaking hardening | Current source of the UI-neutral contract cutover, command registry, id-first QAT model, SVG icon ADR, and namespaced extension persistence. |
| [Ribbon 3](2026-04-23-0-ribbon-3/README.md) | Implemented targeted fix | Fixed SamplesApp label/caption clipping and documented geometry validation. |
| [Ribbon 4](2026-04-23-0-ribbon-4/README.md) | Implemented samples | Added GitHub and Hugo ribbon sample demos and headless sample coverage. |
| [Ribbon 5](2026-04-23-0-ribbon-5/README.md) | Closed consolidated follow-up | Closed runtime/accessibility/context/test/docs findings from the multi-role review. |
| [Ribbon 6](2026-04-23-0-ribbon-6/README.md) | Implemented compatibility break | Splits action/toggle commands, introduces subscription boolean state, centralizes control strategies, and starts `RibbonManager` decomposition. |
| [Ribbon 9](2026-04-25-0-ribbon-9/README.md) | Implemented side placement | Adds host-configurable top/bottom/left/right placement, side-ribbon layout, placement persistence, and SamplesApp comparison coverage. |

## Completed Ribbon 5 Fixes

- Runtime command-state projection and refreshed action dispatch.
- Disposable JavaFX bindings and listener lifecycle coverage.
- Provider failure and duplicate-id diagnostics.
- Focus return for collapsed popups and visible focus states.
- Accessible icon-only controls and disabled icon treatment.
- Contextual accent styling and live theme-switch coverage.
- Typed context metadata and explicit capability contribution.
- Floating active-content context resolution.
- Hugo explicit metadata preference with legacy fallback.
- Focused Maven selector guidance and narrow test-scoped `RibbonTestSupport`.
- Rapid context churn and provider-module Fx coverage for GitHub/Hugo.
- Ribbon 6 design note for future breaking candidates.
- Phase 5 docs/status/release closure.

## Deferred Ribbon Candidates

These remain outside the Ribbon 6 compatibility implementation and require a dedicated plan:

- Add per-user customization schema/UI with explicit unknown-field round-trip policy.
- Consider optional busy/running command state only after UI/accessibility semantics are designed.
- Complete deeper `RibbonManager` extraction beyond `QuickAccessState` when a follow-up can isolate provider catalog, tab merger, command canonicalizer, and refresh scheduling without expanding this API break.

## Carry-Forward Work

- @ops-engineer: archetype ribbon scaffold, if generated apps should demonstrate `RibbonDockHost`, a provider class, and `META-INF/services` registration.
- @ops-engineer with @qa-engineer: Surefire/TestFX POM centralization, only when module-specific JavaFX native-access needs can be preserved.
- @qa-engineer with @ops-engineer and @core-architect: ribbon refresh/adaptive-layout performance budget and opt-in benchmark fixture.
- @feature-dev: code/tree/media production ribbon providers and action contracts.
- @ui-ux-designer with @core-architect: QAT/header overflow behavior, keytips, galleries, and any busy/running command presentation.
