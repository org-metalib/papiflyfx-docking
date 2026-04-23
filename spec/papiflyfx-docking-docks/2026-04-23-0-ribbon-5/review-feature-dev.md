# Ribbon 5 Review — Feature Developer Perspective

**Priority:** P1 (High)  
**Lead Agent:** `@feature-dev`  
**Required Reviewers:** `@core-architect`, `@spec-steward`  
**Workflow:** review-only; emit findings into the `Findings` section at the bottom of this file.

## Goal

Evaluate the ribbon SPI from the perspective of a feature-module author (`code`, `tree`, `media`, `hugo`, `github`, or a new content module). Surface friction in provider onboarding, capability resolution, contextual-tab heuristics, command lifecycle, and parity between the built-in providers.

## Scope

### In scope

1. `GitHubRibbonProvider` in `papiflyfx-docking-github/src/main/java/org/metalib/papifly/fx/github/ribbon/`.
2. `HugoRibbonProvider` in `papiflyfx-docking-hugo/src/main/java/org/metalib/papifly/fx/hugo/ribbon/`.
3. `SampleRibbonProvider` in `papiflyfx-docking-samples/src/main/java/org/metalib/papifly/fx/samples/docks/ribbon/`.
4. `GitHubRibbonActions` and `HugoRibbonActions` capability interfaces (locate them, confirm their module of residence and visibility, cite line numbers).
5. The pattern by which a `DockLeaf` content node exposes typed actions to `RibbonContext#capability(...)`.

### Out of scope

1. Ribbon API redesign (belongs to `@core-architect`).
2. Global CSS/theme work (belongs to `@ui-ux-designer`).
3. Sample catalog wiring (belongs to `@ops-engineer` under `review-ops-engineer.md`).

## Review Questions

### A. Provider onboarding ergonomics

1. How many lines of boilerplate does a trivial feature provider cost today? Compare `SampleRibbonProvider` (~150 LOC) and `GitHubRibbonProvider` (~150 LOC). Identify repeated patterns that could move into an API-level helper (e.g., `RibbonProviders.tab(...)`, `RibbonProviders.group(...)`).
2. Is `ServiceLoader` discovery documented well enough for a downstream consumer (archetype-generated app) to add a provider without reading `papiflyfx-docking-docks` source? Check the archetype template under `papiflyfx-docking-archetype/`.
3. Are icon-loading conventions (`RibbonIconLoader`, `RibbonIconHandle`) consistent across modules, or does each module reinvent its own icon path convention?
4. Can a feature module introduce a provider without depending on `papiflyfx-docking-docks` (runtime) — only on `papiflyfx-docking-api`? Confirm each existing provider's dependency list.

### B. Capability resolution contract

1. Typed capability resolution from `RibbonContext#capability(Class<T>)` replaced raw-node routing in iteration 2. Validate that:
   - The active content node is registered under **every** relevant capability type (not just its concrete class).
   - A capability interface defined in a feature module can be resolved even if the ribbon provider lives in a different module.
2. What happens when the active leaf's content implements the action interface, but is inside a floating window? Confirm floating-window leaves still participate in capability lookup.
3. What happens when the active leaf has no content yet (factory pending), or the content is a placeholder (e.g., error view)? Confirm provider commands degrade gracefully (disabled, not crashing).

### C. Contextual tab heuristics

1. `HugoRibbonProvider` reportedly uses five heuristics for its contextual `Hugo Editor` tab (factory id, content type key, file extension, path pattern, type key content). Enumerate each heuristic, cite the exact lines, and answer:
   - Are heuristics ordered deterministically?
   - Can heuristics conflict across providers (two providers claiming the same leaf)?
   - Are they documented anywhere outside the source?
2. Should contextual-tab activation move from heuristic matching to explicit context attributes set by the content module (e.g., `RibbonContextAttributes.CONTENT_KIND`)? Propose a migration path.
3. How does contextual-tab activation interact with QAT restore? If a QAT command lived on a contextual tab, is it retained when the tab becomes invisible? Cross-check with `review-core-architect.md` question D.3.

### D. Command state lifecycle

1. `GitHubRibbonProvider` allocates new `MutableBoolState` instances on each `getTabs(...)` call. Two risks:
   - If the UI binds to a stale instance, enablement updates are lost.
   - If providers are called on every `syncRibbonContextFromTree()`, allocations add pressure.
   
   Confirm the current behavior, cite the binding path in `RibbonControlFactory`, and decide whether the contract should force providers to cache state.
2. Is there a pattern for reactive enablement (e.g., repo became dirty, Hugo server started)? Today the only refresh path is a full `RibbonManager.refresh()`. Propose a finer-grained signal if needed.
3. How do providers signal "command is running" (busy state) vs "command is disabled"? Confirm the current `BoolState` pair (enabled + selected) is sufficient for typical feature flows.

### E. Parity across built-in providers

1. Walk each provider (`github`, `hugo`, `sample`) and record:
   - tabs contributed
   - groups per tab with collapse order
   - controls per group with small/medium/large modes
   - capability dependency (yes/no, type)
2. Flag any divergence that looks like drift rather than intentional:
   - inconsistent collapse-order constants
   - different naming conventions for command ids (`github.fetch` vs `hugo.preview` vs `sample.*`)
   - different tooltip styles

### F. New-content-module readiness

1. What would it cost to ship a ribbon contribution for `papiflyfx-docking-code`? Specifically:
   - Which code-editor commands belong on a ribbon?
   - What capability interface would `code` expose to `RibbonContext`?
   - What contextual-tab identifier/factory-id should the ribbon-2 heuristics honor?
2. Same question for `papiflyfx-docking-tree`.
3. Same question for `papiflyfx-docking-media`.
4. Identify the missing documentation (ideally in `papiflyfx-docking-docks/README.md` or a new `spec/.../ribbon-provider-authoring.md`) that would make these additions straightforward.

### G. Command id namespace hygiene

1. Is there a documented namespace convention (e.g., `<module>.<action>`)?
2. Are there collision risks if two modules register a command with the same id? `CommandRegistry` canonicalizes by id — confirm which wins and whether the loser is observable.
3. Should command ids become URI-like (`pf://github/fetch`) or stay flat strings? Record trade-offs.

## Review Procedure

1. Read each provider end-to-end.
2. Locate the capability action interfaces (`GitHubRibbonActions`, `HugoRibbonActions`) and note their module, package, visibility, and method list.
3. For each review question, record observations with file/line citations.
4. Draft a skeleton "new provider checklist" for feature modules that do not yet contribute a ribbon; do not commit it as a separate doc in this review, reference it in the finding instead.

## Deliverable

Populate the `Findings` section below using the common template:

```md
### F-<NN>: <short title>
**Severity:** P0|P1|P2|P3  
**Area:** <Provider boilerplate | Capability resolution | Contextual tabs | Command lifecycle | Parity | Readiness | Namespaces>  
**Evidence:** <file:line citations>  
**Risk:** <what a future feature module hits>  
**Suggested follow-up:** <lead role, rough cost S/M/L>
```

## Validation

No automated validation is required. For any finding about reactive enablement, cite the existing test (or test absence) in `papiflyfx-docking-github`, `papiflyfx-docking-hugo`, or `papiflyfx-docking-docks`.

## Findings

_Not yet started._

## Handoff Snapshot

Lead Agent: `@feature-dev`  
Task Scope: feature-module review of the ribbon SPI, capability resolution, contextual tab heuristics, and provider parity  
Impacted Modules: `spec/**` only  
Files Changed: this file (on completion)  
Key Invariants:

- no production code changes
- no API or session format changes
- findings must cite file/line in provider sources

Validation Performed: source inspection only  
Open Risks / Follow-ups: recorded as numbered findings  
Required Reviewer: `@core-architect`, `@spec-steward`
