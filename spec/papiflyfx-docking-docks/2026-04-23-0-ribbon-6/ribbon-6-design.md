# Ribbon 6 Design Note - API Compatibility Candidates

**Status:** Drafted by Ribbon 5 Phase 4  
**Date:** 2026-04-24  
**Lead:** @core-architect  
**Support:** @spec-steward  
**Required reviewers:** @feature-dev, @ui-ux-designer, @qa-engineer, @spec-steward  
**Consult if scope expands:** @ops-engineer for build, archetype, release, or session validation impact

## Scope

This note records Ribbon 6 compatibility decisions that should be planned before any breaking public API or session-format work starts. It does not authorize implementation in Ribbon 5.

Primary inputs:

- `review-core-architect.md` F-05, F-07, F-08, F-13, F-14
- `review-feature-dev.md` F-05
- session documentation findings from `review-core-architect.md` F-03 and `review-spec-steward.md` F-02
- Phase 1-3 handoff in `progress.md`
- current API/runtime source in `PapiflyCommand`, `BoolState`, `RibbonControlSpec`, `RibbonManager`, `RibbonControlFactory`, `RibbonGroup`, and `RibbonSessionCodec`

## Phase 3 Review Handoff

`progress.md` records Phase 3 as complete and asks @ops-engineer, @core-architect, @ui-ux-designer, and @spec-steward to inspect their ownership slices before Phase 4 starts. No separate Phase 3 reviewer feedback artifact was present in this directory at the start of Phase 4. Phase 4 therefore proceeds from the current Phase 3 handoff, with this residual risk:

- @ops-engineer has not separately confirmed the docs-only Surefire/TestFX decision or provider-module validation impact.
- @core-architect has not separately confirmed the Phase 3 action-dispatch refresh Javadocs beyond the current source and progress notes.
- @ui-ux-designer has not separately confirmed that Phase 3 test additions preserve UI expectations.
- @spec-steward has not separately confirmed progress/handoff completeness after Phase 3.

Because Phase 4 is design-only and does not change runtime, API, Maven, or CSS behavior, these risks do not block recording Ribbon 6 candidates. They should be rechecked before Phase 5 closure or any Ribbon 6 implementation plan.

## Current Ribbon 5 Baseline

The following Phase 1-3 decisions are preserved:

- `CommandRegistry` keeps stable canonical command identity and first metadata while refreshing enabled/selected state and action dispatch from later provider emissions.
- JavaFX command bindings are disposable; listener lifecycle is handled by node/menu-item disposal and focused tests.
- Provider failures and duplicate tab/command ids emit diagnostics.
- Typed `RibbonAttributeKey<T>` metadata and explicit capability contribution exist while raw string attributes remain compatible.
- `DockManager` resolves floating active content and marks floating ribbon context.
- Hugo contextual tabs prefer explicit content metadata while retaining documented legacy heuristics.
- Focused TestFX/Surefire selector usage is documented; POM centralization and refresh/adaptive benchmark budgets remain deferred.

## Decision Summary

### Accepted Ribbon 6 Candidates

1. Split command contracts so push commands do not carry toggle-only selected state.
2. Replace `BoolState` listener add/remove with a subscription-returning UI-neutral observable boolean contract.
3. Move control handling toward a UI-neutral render-plan/strategy model so new built-in control families do not require scattered runtime switches.
4. Decompose `RibbonManager` into package-private orchestration collaborators while preserving its public facade.
5. Adopt a documented `extensions.ribbon` forward-compatibility policy before adding customization fields.

### Rejected Options

1. Do not land Ribbon 6 breaking API or session changes in Ribbon 5.
2. Do not add busy/running state to the base command contract as part of the minimum Ribbon 6 break.
3. Do not expose JavaFX `BooleanProperty` or scene-graph types from `papiflyfx-docking-api`.
4. Do not allow feature modules to provide arbitrary JavaFX ribbon node factories through the shared API.
5. Do not move ribbon session state back to a top-level `ribbon` block or persist QAT command objects instead of command ids.

### Deferred Research

1. Busy/running command state as an optional activity/status extension after UI semantics are designed.
2. Per-user ribbon customization fields and UI, including hide/reorder policies and preservation of unknown customization data.
3. Detailed keytip and gallery specs.
4. Ribbon refresh/adaptive-layout performance budgets.
5. Archetype ribbon scaffold and release-note placement, owned by later docs/build work.

## 1. Command Contract Direction

### Current Shape

`PapiflyCommand` is a single record with identity, labels, icons, `enabled`, `selected`, and action callback components. The constructor defaults `selected` to a mutable false state even for push-button commands (`PapiflyCommand.java:35-65`). `RibbonButtonSpec` and `RibbonToggleSpec` both accept the same command type, and the runtime binds toggle selection from `command.selected()` (`RibbonControlFactory.java:107-113`).

Phase 1-3 fixed the most urgent behavior: command identity remains stable while `CommandRegistry` projects incoming enabled/selected snapshots and action dispatch into the canonical command (`CommandRegistry.java:76-111`). That runtime fix should remain unchanged in Ribbon 5.

### Accepted Candidate

Ribbon 6 should split command contracts into a base action command and a toggle command:

- Base action command: id, label, tooltip, icons, enabled observable state, action callback.
- Toggle command: all base action metadata plus selected observable state.
- `RibbonButtonSpec`, `RibbonSplitButtonSpec`, and `RibbonMenuSpec` should accept the base action contract.
- `RibbonToggleSpec` should require the toggle contract.
- The runtime command registry should canonicalize by id for both contracts, but it must reject or diagnose a command id that appears as both action-only and toggle-capable in the same runtime.

The concrete type names should be finalized in the Ribbon 6 plan. Candidate names are `RibbonCommand` plus `RibbonToggleCommand`, or `RibbonActionCommand` plus `RibbonToggleCommand`. The important compatibility decision is the contract split, not the exact names.

### Migration Notes And Compatibility Impact

- Source compatibility break: providers constructing `PapiflyCommand` for toggle controls must migrate to the toggle-capable contract.
- Source compatibility break: code that reads `command.selected()` from a push command must move that state to a toggle command or feature-local state.
- Runtime behavior should not change for existing command ids after migration: id-based canonicalization, first metadata, refreshed enabled state, refreshed selected state for toggles, and refreshed action dispatch remain required.
- Ribbon 6 removes `PapiflyCommand`; providers migrate directly to `RibbonCommand` for actions and `RibbonToggleCommand` for toggles.
- Tests must cover action-only commands not allocating or binding selected state, toggle commands preserving selected refresh semantics, and duplicate ids emitted with incompatible command kinds.

### Busy/Running State

Busy/running command state is not accepted for the base Ribbon 6 command split. Current feature modules already own richer status surfaces: GitHub has busy UI, and Hugo has starting/running server state. The ribbon should not conflate "temporarily running" with "disabled" or "selected" until the UX contract is defined.

Disposition:

- Ribbon 5: feature-local status surfaces remain authoritative.
- Ribbon 6 minimum break: no base `busy` field.
- Later extension: design a UI-neutral `CommandActivityState` or optional command-status capability only after @ui-ux-designer defines rendering semantics for progress, indeterminate running state, cancellation, and accessibility announcements.

## 2. BoolState Direction

### Current Shape

`BoolState` exposes `get`, `set`, `addListener`, and `removeListener` (`BoolState.java:17-61`). `MutableBoolState` stores listeners in a `CopyOnWriteArrayList` (`MutableBoolState.java:15-65`). JavaFX adapters are package-private in the docks runtime and now create disposable bindings that close subscriptions when controls are evicted (`JavaFxCommandBindings.java:73-127`, `JavaFxCommandBindings.java:163-245`).

The abstraction intentionally keeps JavaFX types out of `papiflyfx-docking-api` (`BoolState.java:4-12`), which preserves feature-module dependency boundaries.

### Options Compared

1. Keep current `BoolState`.
   - Pros: no API break; current runtime tests cover listener disposal.
   - Cons: the base interface is mutable, add/remove listener lifecycle is easy to misuse, and implementors must infer subscription ownership.

2. Expose JavaFX `BooleanProperty`.
   - Pros: rich binding semantics for JavaFX callers.
   - Cons: rejected because it leaks JavaFX runtime types into the shared API and repeats the Ribbon 2 problem.

3. Replace with a UI-neutral subscription-returning observable.
   - Pros: preserves API/runtime boundary, makes listener lifecycle explicit, and aligns with Phase 1's disposable binding lesson.
   - Cons: source break for providers and tests that call `addListener` or `set` on the base type.

### Accepted Candidate

Ribbon 6 should introduce a UI-neutral read-only observable boolean contract with subscription-returning listener registration, plus a separate mutable implementation or mutable sub-interface.

Candidate shape:

```java
public interface RibbonBooleanState {
    boolean get();
    Subscription subscribe(BooleanStateListener listener);
}

public interface MutableRibbonBooleanState extends RibbonBooleanState {
    void set(boolean value);
}
```

`Subscription` may extend `AutoCloseable` but should have a no-throws `close()` method, matching the existing runtime adapter behavior.

### Migration Notes And Compatibility Impact

- Source compatibility break: `BoolState#addListener` and `removeListener` call sites migrate to `subscribe(...).close()`.
- Source compatibility break: command records should accept read-only state for provider output; mutation belongs to `MutableRibbonBooleanState` or provider-owned state holders.
- `MutableBoolState` is removed; providers use `MutableRibbonBooleanState` from `RibbonBooleanState.mutable(...)`.
- JavaFX integration stays in `papiflyfx-docking-docks`: adapters read the initial value with `get()`, subscribe, update JavaFX properties on the FX thread, and close subscriptions when nodes/menu items are disposed.
- Tests must retain the listener-count invariant from Phase 1: repeated refreshes, popup rebuilds, QAT rebuilds, and control-cache eviction must not increase retained subscriptions for stable commands.

## 3. Control Extensibility Direction

### Current Shape

`RibbonControlSpec` is sealed and permits only button, toggle, split-button, and menu specs (`RibbonControlSpec.java:9-10`). New control kinds currently require coordinated edits in:

- `RibbonManager#canonicalizeControl` for command canonicalization (`RibbonManager.java:425-450`).
- `RibbonControlFactory#createGroupControl` and presentation helpers for JavaFX materialization (`RibbonControlFactory.java:46-52`, `RibbonControlFactory.java:343-350`).
- `RibbonGroup.ControlSignature#from` for cache/signature comparison (`RibbonGroup.java:476-518`).

### Accepted Candidate

Ribbon 6 should move toward a strategy/render-plan model. The public API must remain UI-neutral; the docks runtime can own JavaFX rendering strategies.

Required design properties:

- `RibbonControlSpec` should expose stable `id()` and a stable control kind or render-plan descriptor.
- Built-in controls should be handled through registered runtime strategies rather than exhaustive switches in every runtime class.
- Unknown control kinds should be diagnosed and skipped, not crash the whole ribbon.
- Command extraction/canonicalization should be a control strategy responsibility so each control kind declares which commands it owns.
- Signature comparison should be strategy-provided or plan-provided, so cache reuse does not need to know every concrete record.

This does not mean arbitrary feature modules should inject JavaFX nodes. Feature modules must remain able to depend on `papiflyfx-docking-api` only. If third-party controls are ever allowed, the extension should be UI-neutral render plans or a separate runtime renderer SPI with explicit dependency consequences.

### Keytips And Galleries

- Keytips should start as command/control metadata and focus-navigation behavior, not as a new visual control family. Their design depends on command identity, focus scopes, popup behavior, and accessibility announcements.
- Galleries are a likely new control family. They need item identity, selection model, virtualization or size budgeting, keyboard behavior, QAT eligibility rules, and session/customization policy before implementation.

### Migration Notes And Compatibility Impact

- Source compatibility break: consumers who switch exhaustively on the sealed control hierarchy must migrate to the new kind/visitor/render-plan API.
- Binary compatibility break: changing `RibbonControlSpec` from sealed to open, or adding abstract methods such as `kind()`, requires a Ribbon 6 major/API break.
- Existing built-in specs can remain as records if they implement the new contract.
- Tests must cover built-in rendering parity, unknown-kind diagnostics, command canonicalization for nested commands, signature/cache reuse, QAT hidden contextual command behavior, and any new gallery/keytip control behavior.

## 4. RibbonManager Responsibility Split

### Current Shape

`RibbonManager` currently owns provider discovery and ordering, context refresh, tab collection/merging, command canonicalization, duplicate diagnostics, command registry pruning, QAT id state, and QAT derived command view (`RibbonManager.java:53-81`, `RibbonManager.java:115-123`, `RibbonManager.java:332-351`, `RibbonManager.java:371-388`, `RibbonManager.java:425-510`, `RibbonManager.java:513-517`).

### Accepted Refactoring Plan

This is a non-breaking refactor candidate. Keep `RibbonManager` as the public facade and extract package-private collaborators:

1. `RibbonProviderCatalog`
   - Owns `ServiceLoader` discovery, provider sorting, explicit provider list mutation, and reload.

2. `RibbonTabMerger`
   - Owns tab/group accumulation, ordering, duplicate tab diagnostics, and post-filter tab materialization.

3. `RibbonCommandCanonicalizer`
   - Owns control command extraction, command collision diagnostics, registry canonicalization, reachable id collection, and pruning.

4. `QuickAccessState`
   - Owns mutable id list, duplicate/blank normalization, derived command view, host-owned QAT command registration, and id-first persistence semantics.

5. `RibbonRefreshScheduler`
   - Owns refresh triggering from provider/context changes and can later coalesce redundant refreshes on the FX pulse without mixing that policy with provider logic.

6. Session contribution remains outside `RibbonManager`
   - `Ribbon` and `RibbonSessionStateContributor` currently capture/restore `RibbonSessionData`. Keep this boundary unless a future customization manager needs a dedicated session collaborator.

### Migration Notes And Compatibility Impact

- No public API break is required if the facade methods remain stable.
- Tests should be moved or added around collaborators as they are extracted: provider failure isolation, duplicate diagnostics, command canonicalization/action dispatch, QAT id restore, selected-tab fallback, and rapid context churn.
- Refactor order matters: extract QAT state and command canonicalization only after preserving current Phase 1-3 tests, then extract tab merger and provider catalog, then consider refresh scheduling.

## 5. Session Forward-Compatibility And Customization Policy

### Current Shape

Ribbon state is a namespaced dock-session extension under `extensions.ribbon`, not a top-level `ribbon` field. The ribbon payload currently contains:

- `minimized`
- `selectedTabId`
- `quickAccessCommandIds`

`RibbonSessionCodec` encodes those fields only (`RibbonSessionCodec.java:18-28`) and decodes known fields by type (`RibbonSessionCodec.java:31-39`). Malformed known fields throw precise `IllegalArgumentException`s (`RibbonSessionCodec.java:42-90`). The session service isolates contributor restore failures by logging and continuing (`DefaultDockSessionService.java:328-338`, `DefaultDockSessionService.java:360-368`). The outer serializer ignores malformed extension containers and malformed extension payload keys (`DockSessionSerializer.java:314-345`).

QAT semantics are id-first: `Ribbon#captureSessionState` persists `RibbonManager#getQuickAccessCommandIds()`, not the derived command view (`Ribbon.java:225-243`), and restore keeps unresolved command ids pinned until providers contribute them again (`Ribbon.java:246-268`).

### Accepted Policy

1. Keep ribbon session state under `extensions.ribbon`.
2. Keep QAT persistence as command id lists.
3. Treat missing `extensions.ribbon` as "no ribbon state".
4. Ignore unknown fields under `extensions.ribbon` during decode. Current decode already reads known keys only.
5. Do not promise unknown-field round-trip preservation on save until a customization store exists. Current capture writes the current known payload from runtime state.
6. Malformed known fields remain invalid for the ribbon extension and should be logged/isolated without aborting core session restore.
7. Future customization fields should be introduced under a nested object rather than interleaving many top-level keys. Candidate:

```json
{
  "extensions": {
    "ribbon": {
      "minimized": false,
      "selectedTabId": "home",
      "quickAccessCommandIds": ["app.ribbon.save"],
      "schemaVersion": 2,
      "customization": {
        "hiddenTabIds": [],
        "tabOrder": [],
        "groupOrder": {}
      }
    }
  }
}
```

8. Absence of `schemaVersion` means the current Ribbon 5 payload shape.
9. A future `schemaVersion` must version the ribbon extension payload only, not the whole dock session.
10. Customization introduction must define how unknown customization fields are preserved or discarded before any user-editing UI writes sessions.

### Migration Notes And Compatibility Impact

- No Ribbon 5 session shape changes are made by this phase.
- Adding `schemaVersion` and `customization` in Ribbon 6 can be forward-compatible for current readers if unknown fields continue to be ignored.
- Current readers will drop unknown ribbon customization fields when they save a session because they recapture known runtime state only. That must be documented before customization ships.
- Malformed known field behavior remains strict: a non-boolean `minimized`, non-string `selectedTabId`, or non-list/string item in `quickAccessCommandIds` invalidates only the ribbon extension restore path.
- Per-user customization is deferred until the schema, UI, and merge policy are specified.

## Finding Dispositions

| Finding | Disposition |
| --- | --- |
| `review-core-architect.md` F-05 | Accepted for Ribbon 6: split action and toggle command contracts. |
| `review-feature-dev.md` F-05 | Deferred: busy/running state remains feature-local now; later optional command activity extension requires UX/accessibility design. Rejected for base Ribbon 6 command. |
| `review-core-architect.md` F-13 | Accepted for Ribbon 6: replace add/remove listener pattern with subscription-returning UI-neutral observable boolean state. |
| `review-core-architect.md` F-08 | Accepted for Ribbon 6 design: move toward strategy/render-plan control handling. Detailed keytip/gallery specs deferred. |
| `review-core-architect.md` F-07 | Accepted as non-breaking refactor plan: keep `RibbonManager` facade and extract package-private collaborators. |
| `review-core-architect.md` F-14 | Accepted as policy: keep `extensions.ribbon`, ignore unknown fields, keep malformed known fields strict/isolated, defer customization schema. |
| `review-core-architect.md` F-06 | Superseded by Phase 2 typed `RibbonAttributeKey<T>` metadata and explicit contributor contracts. |
| `review-core-architect.md` F-03 and `review-spec-steward.md` F-02 | Accepted for Phase 5 documentation closure: authoritative schema is `extensions.ribbon`; Ribbon 1 top-level examples are historical/superseded. |

## Validation Expectations For A Future Ribbon 6 Plan

Before implementing any accepted breaking candidate:

1. Create a dedicated Ribbon 6 implementation plan with exact type names, migration sequence, and compatibility adapters.
2. Update public Javadocs and provider authoring docs before changing feature providers.
3. Run API and docks compile checks after public type changes.
4. Run focused docks ribbon tests, GitHub/Hugo provider tests, and sample ribbon Fx tests.
5. Add migration/release notes for every public API or session compatibility break.

## Handoff

Lead Agent: `@core-architect`  
Support Agent: `@spec-steward`  
Task Scope: Phase 4 design decisions for Ribbon 6 compatibility candidates  
Impacted Modules: `spec/**` only  
Files Changed: this file and `progress.md`  
Key Invariants:

- no Ribbon 6 breaking API/session behavior is implemented in Ribbon 5
- feature modules remain able to depend on `papiflyfx-docking-api`
- QAT id-first persistence remains unchanged
- raw-string attributes, `RibbonContextAttributes` constants, and Hugo legacy heuristics remain intact
- Phase 1-3 runtime, accessibility, context, provider, and validation decisions are preserved

Validation Performed: documentation/source sanity checks recorded in `progress.md`  
Open Risks / Follow-ups: Phase 3 reviewer feedback still has no separate artifact; Ribbon 6 needs a focused implementation plan before code changes; busy state, customization UI/schema, keytips, galleries, and performance budgets remain deferred  
Required Reviewer: `@feature-dev`, `@ui-ux-designer`, `@qa-engineer`, `@spec-steward`
