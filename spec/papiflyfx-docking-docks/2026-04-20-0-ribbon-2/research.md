# Research — Docking Ribbon 2

**Date:** 2026-04-20  
**Lead Agent:** @core-architect  
**Prompt Source:** `spec/papiflyfx-docking-docks/2026-04-20-0-ribbon-2/README.md`

## Objective

Evaluate the Phase 1-5 ribbon delivery (`2026-04-19-0-ribbon`) against the intended architecture and identify concrete improvements across concept, design, and implementation for a follow-up iteration.

## Compatibility stance

Compatibility is intentionally **not** a design constraint for this iteration.  
Breaking API, provider-contract, and session-shape changes are acceptable when they simplify architecture or improve runtime correctness.

## Method

1. Read design docs in `spec/papiflyfx-docking-docks/2026-04-19-0-ribbon`.
2. Review ribbon API/runtime/provider code in:
   - `papiflyfx-docking-api` (`org.metalib.papifly.fx.api.ribbon`)
   - `papiflyfx-docking-docks` (`org.metalib.papifly.fx.docks.ribbon`)
   - `papiflyfx-docking-github` and `papiflyfx-docking-hugo` providers
3. Review existing ribbon-focused test suites.

## Findings

| Area | Current state | Gap | Improvement direction | Priority |
| --- | --- | --- | --- | --- |
| API abstraction boundary | `PapiflyCommand` exposes JavaFX `BooleanProperty` in API module. | “UI-agnostic” contract is partially violated. | Introduce framework-neutral command state contract in API; adapt to JavaFX in runtime layer. | P0 |
| Context contract | Providers resolve actions by casting `ACTIVE_CONTENT_NODE` from context attributes. | Context still leaks concrete node types; weak decoupling. | Replace node-casting with typed capabilities in `RibbonContext` attributes (or a typed context facet API). | P0 |
| Command lifecycle | Provider commands are recreated on each `getTabs(context)` and enabled state is snapshotted. | Stale enable/selected state risk and avoidable allocation churn. | Use long-lived command registry with reactive state updates and stable identities. | P0 |
| QAT model | Runtime stores QAT as command object list and resolves persisted IDs only from currently materialized tabs. | QAT restore can miss commands when contextual tabs are hidden at restore time. | Persist and manage QAT by command IDs backed by global command registry, independent of visible tabs. | P0 |
| Provider merge semantics | Tabs/groups with identical IDs are merged silently; first tab metadata wins. | ID collisions are hard to detect and can produce non-obvious UI behavior. | Add duplicate-ID diagnostics and configurable conflict policy (warn/fail). | P1 |
| Adaptive layout engine | Layout always resets groups to `LARGE`, then shrinks iteratively; mode changes rebuild controls. | Potential flicker/perf overhead in frequent resize scenarios. | Cache control nodes per mode and use measured widths with incremental transition strategy. | P1 |
| Reduction priority semantics | API doc says lower priority collapses first; providers use `HIGH_PRIORITY=30`, `LOW_PRIORITY=10`. | Semantic ambiguity for contributors. | Rename field or invert semantics to explicit `collapseOrder`; update Javadocs/examples. | P1 |
| Icon pipeline | `.svg` handles are skipped by loader; octicon path fallback + raster only. | Limits visual quality/theming flexibility. | Add SVG icon loading and optional tint strategy for theme consistency. | P2 |
| Session extension model | Contributor SPI exists, but `DockSessionData` still has hardcoded `ribbon` field. | Extension architecture is only partially generalized. | Replace with namespaced extension payload map + per-extension codecs; migration bridge is optional and not required for this iteration. | P1 |
| Lifecycle cleanup | `RibbonDockHost.dispose()` unregisters contributor but does not actively unbind theme/context bindings. | Potential lifecycle leaks in dynamic host mount/unmount scenarios. | Add explicit unbind/dispose wiring for host-managed ribbon bindings/listeners. | P2 |
| Test depth | Core happy-path suites exist for adaptive layout, context, persistence, providers. | Gaps around merge conflict diagnostics, reactive state updates, SVG/icon failure paths, and extension payload validation. | Add targeted unit + Fx tests for all above risk areas. | P1 |

## Concept-level improvements

1. Treat ribbon as a command platform, not just ribbon chrome:
   - stable command identity
   - reusable command state
   - host-level command registry shared by ribbon, menus, shortcuts, and QAT
2. Split “context visibility” from “action resolution”:
   - visibility decides what is shown
   - capability resolution decides what is executable
3. Formalize extension payload ownership so future modules (not only ribbon) can persist host state through a unified mechanism.

## Design-level improvements

1. Clarify contribution contracts:
   - duplicate tab/group IDs
   - priority semantics
   - command ownership and lifecycle
2. Add explicit capability interfaces for provider integration instead of `Node` probing.
3. Define adaptive layout policy as deterministic rules with measurable thresholds and test vectors.

## Implementation-level improvements

1. Add `CommandRegistry` and make providers contribute descriptors keyed by command ID.
2. Refactor `RibbonManager` merge path to report collisions and preserve deterministic ordering rules.
3. Refactor adaptive layout to avoid full-control re-instantiation during every resize transition.
4. Extend serializer with robust typed-validation helpers before map/list casts.

## Validation implications

Future validation should include:

1. Resize stress and adaptive stability checks under continuous width changes.
2. QAT restore correctness when contextual providers are absent/present across sessions.
3. Provider conflict tests for duplicate IDs.
4. Lifecycle leak checks for host create/dispose cycles.
