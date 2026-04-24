# Plan - Ribbon 6 Compatibility Implementation

**Priority:** P2 (Normal)  
**Planning Lead:** @spec-steward  
**Implementation Lead:** @core-architect  
**Support:** @feature-dev, @ui-ux-designer, @qa-engineer, @spec-steward  
**Required Reviewers:** @core-architect, @feature-dev, @ui-ux-designer, @qa-engineer, @spec-steward  
**Consult If Scope Expands:** @ops-engineer for Maven/TestFX, archetype, release, or session migration tooling  
**Source Design:** [ribbon-6-design.md](ribbon-6-design.md)

## Goal

Turn the Ribbon 6 compatibility candidates from `ribbon-6-design.md` into an executable implementation plan for a coordinated API/runtime break.

Ribbon 6 is allowed to change public ribbon API contracts, but the work must preserve the Ribbon 5 behavioral baseline where the design note explicitly calls it out: stable command ids, refreshed command state/action dispatch, QAT id-first persistence, provider failure isolation, typed context metadata compatibility, and `extensions.ribbon` session namespacing.

## Scope

### In Scope

1. Split ribbon command contracts so action-only commands do not carry toggle-only selected state.
2. Replace `BoolState` listener add/remove APIs with a UI-neutral subscription-returning observable boolean contract and a separate mutable state.
3. Move ribbon control handling toward a strategy/render-plan model that avoids scattered runtime switches for built-in control families.
4. Decompose `RibbonManager` into package-private collaborators while keeping it as the public facade.
5. Adopt and document the `extensions.ribbon` forward-compatibility policy before adding future customization fields.
6. Update API Javadocs, provider-authoring documentation, release/migration notes, and focused tests for every public contract change.

### Out Of Scope

1. Implementing busy/running command state in the base command contract.
2. Implementing per-user ribbon customization UI or persistence beyond compatibility policy documentation.
3. Implementing keytips or galleries unless a follow-up design narrows their behavior and validation.
4. Allowing feature modules to inject arbitrary JavaFX ribbon nodes through `papiflyfx-docking-api`.
5. Moving ribbon session state out of `extensions.ribbon` or persisting QAT command objects instead of command ids.
6. Changing archetype templates, publishing configuration, or release automation without an @ops-engineer-owned subplan.

## Key Invariants

1. Feature modules must remain able to depend on `papiflyfx-docking-api` only for ribbon contribution contracts.
2. JavaFX scene-graph and property types must stay out of public shared API contracts unless a separately approved plan changes the module boundary.
3. Command canonicalization remains id-based: first metadata wins, state and action dispatch refresh from later provider emissions, and incompatible command-kind reuse is diagnosed.
4. Toggle selected state belongs only to toggle-capable commands after migration.
5. Listener lifecycle remains explicit and disposable; repeated refreshes, popup rebuilds, QAT rebuilds, and control-cache eviction must not retain stale subscriptions.
6. Unknown ribbon session fields are ignored on decode, malformed known fields invalidate only ribbon extension restore, and QAT persistence remains id-first.
7. `RibbonManager` remains the public integration facade while extracted collaborators stay package-private unless a public API need is proven.

## Workstreams

### A. Public Command And State Contracts

Lead: @core-architect  
Reviewers: @feature-dev, @qa-engineer, @spec-steward

Planned outcomes:

1. Finalize type names for action and toggle command contracts.
2. Decide whether `PapiflyCommand` remains as a deprecated compatibility bridge for one release.
3. Introduce a read-only observable boolean state and mutable state with subscription-returning listener registration.
4. Migrate built-in specs and providers to action-only vs. toggle-capable command contracts.
5. Add diagnostics for a command id contributed as both action-only and toggle-capable in one runtime.
6. Add tests for action-only commands not allocating selected state, toggle state refresh, action dispatch refresh, and subscription disposal.

### B. Control Strategy And Rendering Model

Lead: @core-architect  
Reviewers: @feature-dev, @ui-ux-designer, @qa-engineer

Planned outcomes:

1. Define the UI-neutral control kind/render-plan contract.
2. Move command extraction, signature comparison, and rendering dispatch into built-in runtime strategies.
3. Preserve existing button, toggle, split-button, and menu rendering behavior.
4. Diagnose and skip unknown control kinds without aborting the whole ribbon refresh.
5. Document how future keytip and gallery work should plug into the strategy model without exposing JavaFX factories through the API.
6. Add tests for built-in rendering parity, nested command canonicalization, unknown-kind diagnostics, cache/signature reuse, and QAT behavior.

### C. RibbonManager Decomposition

Lead: @core-architect  
Reviewers: @qa-engineer, @spec-steward

Planned outcomes:

1. Extract `QuickAccessState` first so id-first persistence and derived command lookup have focused coverage.
2. Extract command canonicalization into `RibbonCommandCanonicalizer`.
3. Extract tab/group accumulation and duplicate tab diagnostics into `RibbonTabMerger`.
4. Extract provider discovery, sorting, explicit provider mutation, and reload into `RibbonProviderCatalog`.
5. Extract refresh triggering into `RibbonRefreshScheduler` only after current refresh behavior is locked by tests.
6. Keep session contribution in `Ribbon` / `RibbonSessionStateContributor` unless customization work creates a new requirement.

### D. Session Policy, Migration, And Documentation

Lead: @spec-steward  
Reviewers: @core-architect, @feature-dev, @qa-engineer

Planned outcomes:

1. Document Ribbon 6 source and binary compatibility breaks before code migration starts.
2. Keep `extensions.ribbon` as the authoritative persisted namespace.
3. Keep `quickAccessCommandIds` as the persisted QAT source of truth.
4. Document that current readers ignore unknown ribbon fields on decode but do not preserve unknown customization fields on save.
5. Define release/migration notes for command contract split, boolean state subscriptions, control strategy migration, and any session schema additions.
6. Record validation commands and reviewer disposition in `progress.md`.

## Implementation Phases

### Phase 0 - Planning And API Names

Lead: @core-architect with @spec-steward support

Tasks:

1. Read `ribbon-6-design.md`, this plan, and current Ribbon 5 status/release notes.
2. Finalize names for action command, toggle command, boolean state, subscription, control kind, and strategy/render-plan contracts.
3. Decide compatibility bridge policy for `PapiflyCommand`, `BoolState`, and existing control records.
4. Update this plan if exact names or migration sequence differ from the design note.

Acceptance:

- Public type names and bridge/deprecation policy are recorded before implementation.
- Required reviewers have enough detail to assess the break.

### Phase 1 - Command And Boolean State Contracts

Lead: @core-architect

Tasks:

1. Add the new command and boolean state contracts in `papiflyfx-docking-api`.
2. Migrate ribbon control specs and built-in providers to the split command model.
3. Update docks runtime canonicalization and JavaFX binding adapters.
4. Add focused unit/TestFX coverage for command kind diagnostics, state refresh, action dispatch refresh, and subscription disposal.
5. Update Javadocs and provider-authoring docs.

Acceptance:

- API and docks compile.
- Existing Ribbon 5 command behavior is preserved after migration except for intentional source breaks.
- Tests prove action-only commands do not carry toggle selected state.

### Phase 2 - Control Strategy Model

Lead: @core-architect with @feature-dev and @ui-ux-designer review

Tasks:

1. Add the control strategy/render-plan contract and built-in strategies.
2. Move command extraction, signature comparison, and JavaFX materialization dispatch into strategy-owned paths.
3. Preserve rendering parity for existing built-in controls.
4. Add unknown-kind diagnostics and cache/signature tests.
5. Document extension limits for future controls.

Acceptance:

- Runtime switches are reduced to strategy registration/lookup boundaries.
- Unknown control kinds are isolated and observable in diagnostics.
- Existing button, toggle, split-button, menu, QAT, and collapsed-group behaviors remain covered.

### Phase 3 - RibbonManager Collaborators

Lead: @core-architect

Tasks:

1. Extract QAT state, command canonicalization, tab merging, provider catalog, and refresh scheduling in dependency-safe order.
2. Keep extracted types package-private unless a public contract is explicitly approved.
3. Preserve public `RibbonManager` facade methods and behavior.
4. Move or add focused tests around each extracted collaborator.

Acceptance:

- Public facade compatibility is maintained.
- Collaborators have direct tests for their ownership slices.
- Existing provider failure, duplicate diagnostics, QAT restore, selected-tab fallback, and context churn tests pass.

### Phase 4 - Documentation, Migration, And Closure

Lead: @spec-steward with @core-architect support

Tasks:

1. Update provider-authoring docs, release/migration notes, and status pages.
2. Record exact validation commands and outcomes in `progress.md`.
3. Record dispositions for deferred busy state, customization UI/schema, keytips, galleries, performance budgets, and archetype work.
4. Capture reviewer handoff and remaining risks.

Acceptance:

- Documentation matches implemented API/runtime behavior.
- `progress.md` contains validation evidence and reviewer/handoff notes.
- Deferred work has owners and reasons.

## Validation Expectations

Minimum focused checks after API/runtime changes:

1. `./mvnw -pl papiflyfx-docking-api,papiflyfx-docking-docks -am compile`
2. `./mvnw -pl papiflyfx-docking-docks -am -Dtest=Ribbon*Test,CommandRegistryTest -Dsurefire.failIfNoSpecifiedTests=false -Dtestfx.headless=true test`
3. `./mvnw -pl papiflyfx-docking-github,papiflyfx-docking-hugo -am -Dtestfx.headless=true test`
4. `./mvnw -pl papiflyfx-docking-samples -am -Dtest=*Ribbon*FxTest -Dsurefire.failIfNoSpecifiedTests=false -Dtestfx.headless=true test`
5. `git diff --check`

Broaden validation if the implementation touches Maven configuration, samples beyond ribbon tests, archetype templates, login/settings modules, CSS/theming, or persisted-session migration tooling.

## Acceptance Criteria

1. Ribbon command API separates action-only and toggle-capable commands.
2. Ribbon boolean state API uses explicit subscription disposal.
3. Runtime command canonicalization preserves Ribbon 5 refresh semantics and diagnoses incompatible command kinds.
4. Control handling supports built-in strategies/render plans without scattered exhaustive switches.
5. `RibbonManager` delegates major responsibilities to focused package-private collaborators.
6. Session policy remains `extensions.ribbon` and QAT id-first.
7. Provider docs, Javadocs, release/migration notes, and progress validation are current.

## Handoff Contract

Each phase handoff must record:

1. Lead and supporting agents.
2. Files changed.
3. Validation commands and results.
4. Reviewer needs.
5. Deferred items and reasons.
6. Compatibility risks for downstream providers.
