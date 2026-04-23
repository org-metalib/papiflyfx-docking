# Ribbon 5 Review — Core Architect Perspective

**Priority:** P1 (High)  
**Lead Agent:** `@core-architect`  
**Required Reviewers:** `@spec-steward`, `@qa-engineer`  
**Workflow:** review-only; emit findings into the `Findings` section at the bottom of this file.

## Goal

Audit the ribbon API/SPI, runtime orchestration, and session format against the iteration 1 (`ribbon-1`) and iteration 2 (`ribbon-2`) invariants and the repository's SOLID principles. Report architectural risks and contract gaps that should shape the next iteration.

## Scope

### In scope

1. `papiflyfx-docking-api/src/main/java/org/metalib/papifly/fx/api/ribbon/` public contracts.
2. `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/` runtime.
3. `DockManager` ribbon context plumbing (`trackRibbonContext`, `syncRibbonContextFromTree`, `resolveActiveRibbonLeaf`, `buildRibbonContext`).
4. Session extension hook (`RibbonSessionCodec`, `RibbonSessionStateContributor`) as it interacts with `DockSessionService`/`DockSessionExtensionCodec`.

### Out of scope

1. Feature-module provider implementations (go in `review-feature-dev.md`).
2. Build, packaging, samples, dependency hygiene (go in `review-ops-engineer.md`).
3. Visual/theme concerns (go in `review-ui-ux-designer.md`).
4. Test harness changes (go in `review-qa-engineer.md`).

## Review Questions

### A. API/SPI shape (`papiflyfx-docking-api`)

1. Does `PapiflyCommand` honor Interface Segregation? It currently exposes id, label, tooltip, icons, enabled/selected state, and execution — confirm each field is needed by every provider and that no UI type leaks.
2. Are `BoolState`/`MutableBoolState` the right level of abstraction, or are they a minimal reinvention of `javafx.beans.property.BooleanProperty` that duplicates concerns (invalidation, binding semantics, thread affinity)?
3. Does `RibbonContext` expose a sound extension model via `attribute(...)` and `capability(...)`? Are attribute keys typed, collision-safe, and discoverable? Who owns the canonical `RibbonContextAttributes` vocabulary?
4. Are the spec records (`RibbonTabSpec`, `RibbonGroupSpec`, `RibbonButtonSpec`, `RibbonToggleSpec`, `RibbonSplitButtonSpec`, `RibbonMenuSpec`) actually value types (immutable, no behavior), or do they carry hidden mutable state through commands and state props?
5. Is `RibbonIconHandle` the right abstraction compared to the shared UI primitives in `org.metalib.papifly.fx.ui` (see `UiStyleSupport`, `UiCommonStyles`)? Is there duplication with `RibbonIconLoader`?
6. Does `RibbonProvider` document ordering, stability, and exception semantics for `ServiceLoader` discovery? What happens if two providers claim the same tab id?

### B. Canonical command identity and refresh lifecycle

1. `CommandRegistry` (196 LOC) promises single-instance command identity. Confirm:
   - That the registry is the only source of truth and is always consulted before a node is built (see `RibbonManager.refresh()` and `RibbonGroup.resolveControlNode(...)`).
   - That post-materialization pruning does not evict commands still referenced by the QAT.
   - That command reference equality is preserved across context changes that keep the command id stable.
2. `RibbonManager.refresh()` runs a two-phase pass (canonicalize → materialize). Verify that providers cannot defeat canonicalization by returning fresh `MutableBoolState` instances on every call; if they can, the UI may bind to stale state. `GitHubRibbonProvider` allocates state per call — confirm this is safe or codify the contract.
3. Is `refresh()` idempotent, re-entrant, and FX-thread-only? Are there documented preconditions?
4. What is the fallback behavior when a `RibbonProvider#getTabs(...)` throws? The research hypothesized logging only. Confirm that one failing provider cannot take down the ribbon for all others and that the failure surface is observable (telemetry or log).

### C. DockManager integration

1. `trackRibbonContext(...)` wires listeners on `activeTabIndexProperty`. Confirm listeners are removed on detach and do not leak across floating-window lifecycle transitions.
2. `syncRibbonContextFromTree()` is called on every active-leaf change, mouse press, minimize/maximize, and float/dock. Is synchronous refresh acceptable, or should we debounce/coalesce on the FX pulse?
3. `resolveActiveRibbonLeaf(...)` — does it correctly handle floating windows, minimized leaves, and maximized leaves? Cross-check the semantics against `DockState` and the `FloatingWindowManager` contract.
4. Does `DockManager` register the active content node under its concrete class for capability lookup (per `ribbon-2` plan)? Confirm the capability type is the concrete class and that feature modules do not need to re-declare interfaces in a central registry.

### D. Session format and persistence

1. Persistence schema documented in `ribbon-1/README.md`:

    ```json
    {
      "type": "dockSession",
      "version": 2,
      "layout": { },
      "ribbon": {
        "minimized": true,
        "selectedTabId": "hugo-editor",
        "quickAccessCommandIds": ["github.fetch", "hugo.preview"]
      }
    }
    ```

    Verify `RibbonSessionCodec` still emits exactly this shape and that the serializer rejects or filters unknown values safely.
2. Confirm that `DockSessionStateContributor` is the only hook used by ribbon persistence, and that no ribbon state leaks into `LayoutNode`.
3. Ensure `RibbonManager.getQuickAccessCommandIds()` is treated as the source of truth per `CLAUDE.md`; confirm there is no reliance on the derived `getQuickAccessCommands()` for persistence.
4. Check restore tolerance for:
   - unknown/missing tab id,
   - unknown/missing QAT command ids,
   - future fields (forward compatibility),
   - legacy sessions without a `ribbon` block.

### E. SOLID audit

1. **Single Responsibility:** is `RibbonManager` (523 LOC) one responsibility or several (provider discovery, tab merging, canonicalization, QAT state, refresh scheduling)? Propose a split if responsibilities blur.
2. **Single Responsibility:** same question for `Ribbon` (438 LOC) — it renders the strip, holds adaptive-layout logic, and owns QAT composition.
3. **Open/Closed:** can a new control type be added without editing `RibbonControlFactory` (410 LOC)? Current design uses sealed spec records and a factory switch. Assess whether a visitor or strategy pattern is warranted.
4. **Liskov Substitution:** do all `RibbonProvider` implementations honor the same contract around tab visibility, command equality, and null handling?
5. **Interface Segregation:** does `RibbonContext` force consumers to depend on `attribute(...)` when they only need `capability(...)` (or vice versa)?
6. **Dependency Inversion:** does `papiflyfx-docking-docks` ribbon runtime depend on API abstractions only, or does `RibbonControlFactory` leak into anything feature modules can see?

### F. Extensibility surface for iteration 3+

1. What would a KeyTips implementation cost given the current shape? Does the API need to reserve a `commandKeyTip` slot now to avoid a later breaking change?
2. What would a gallery control cost? Is there a way to add it without widening `RibbonControlSpec`?
3. Can we support per-user ribbon customization without changing the session schema? If not, note the breaking-change boundary explicitly.
4. Is there a path for command i18n (localized labels/tooltips) without every provider implementing its own pipeline?

## Review Procedure

1. Open the API files listed above and take notes on each review question, citing file and line.
2. Diff the current implementation against the `ribbon-2` plan's invariants (see `spec/papiflyfx-docking-docks/2026-04-20-0-ribbon-2/plan.md` key invariants).
3. Confirm the current code honors the `CLAUDE.md` "Ribbon Integration Pattern" guidance (especially canonical command identity and QAT ID source of truth).
4. For each risk, record: severity (P0–P3), affected file/line, suggested owner, suggested remediation cost (S/M/L).

## Deliverable

Populate the `Findings` section below in the form:

```md
### F-<NN>: <short title>
**Severity:** P0|P1|P2|P3  
**Area:** <API | CommandRegistry | RibbonManager | DockManager integration | Session | SOLID | Extensibility>  
**Evidence:** <file:line citations or method references>  
**Risk:** <what could go wrong and when>  
**Suggested follow-up:** <lead role, rough cost S/M/L>
```

Prioritize findings that block iteration 3 ergonomics (customization UI, KeyTips, galleries, localization) or that put session compatibility at risk.

## Validation

No automated validation is required for this review. However, when a finding recommends a behavior change, cite the test that currently covers (or fails to cover) the behavior.

## Findings

<!-- Populate during review. Keep one entry per distinct finding. -->

_Not yet started._

## Handoff Snapshot

Lead Agent: `@core-architect`  
Task Scope: architectural review of the ribbon API, runtime, DockManager integration, and session format  
Impacted Modules: `spec/**` only  
Files Changed: this file (on completion)  
Key Invariants:

- no production code changes
- no API or session format changes
- findings must cite files and lines

Validation Performed: source inspection only  
Open Risks / Follow-ups: will be captured as numbered findings  
Required Reviewer: `@spec-steward`
