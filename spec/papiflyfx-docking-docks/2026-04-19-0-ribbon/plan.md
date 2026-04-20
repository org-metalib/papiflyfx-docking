# Plan — Docking Ribbon Toolbar

**Priority:** P2 (Normal)
**Lead Agent:** @core-architect
**Required Reviewers:** @ui-ux-designer, @feature-dev, @qa-engineer, @ops-engineer
**Workflow:** research complete -> phased implementation -> validation

## Goal

Introduce a ribbon-style command surface for PapiflyFX that separates command metadata from JavaFX presentation, allows feature modules to contribute commands through SPI, adapts to available width, and preserves docking/session compatibility.

The first delivery should focus on a practical ribbon foundation for `papiflyfx-docking-api`, `papiflyfx-docking-docks`, `papiflyfx-docking-github`, `papiflyfx-docking-hugo`, and `papiflyfx-docking-samples`. Advanced Office-style features should be staged behind a stable core instead of being attempted all at once.

## Current baseline

- `papiflyfx-docking-api` already provides shared theme/UI primitives, but it does not yet define a command or ribbon SPI.
- `papiflyfx-docking-docks` owns the docking runtime, active layout context, and session restore flow, making it the right runtime layer for ribbon orchestration.
- `papiflyfx-docking-github` and `papiflyfx-docking-hugo` already have domain-specific actions that can be mapped into ribbon groups.
- The research doc establishes the target interaction model: tab > group > control, adaptive group scaling, modular contribution, and persisted ribbon state.

## Scope

### In scope

1. Define a minimal command/ribbon SPI in `papiflyfx-docking-api`.
2. Implement a ribbon runtime and JavaFX controls in `papiflyfx-docking-docks`.
3. Support adaptive group sizing with `LARGE`, `MEDIUM`, `SMALL`, and `COLLAPSED` presentation modes plus reduction priority.
4. Add a Quick Access Toolbar and minimized ribbon state.
5. Add `ServiceLoader`-based ribbon contribution so modules can register tabs/groups without hard coupling.
6. Support contextual tab visibility based on active dock/content context.
7. Provide first-party ribbon contributions for GitHub and Hugo workflows.
8. Add sample-app wiring, documentation, and regression coverage for layout, context switching, and session restore.

### Out of scope for iteration 1

- Full end-user ribbon customization UI
- Tab drag-reordering UI
- Rich ScreenTips with images/previews
- Full KeyTips implementation
- Gallery-heavy controls beyond basic menu/popup behavior
- Migration of every content module to ribbon contributions in the same change set

## Impacted modules

| Module | Planned responsibility |
| --- | --- |
| `papiflyfx-docking-api` | Command metadata, ribbon contracts, provider SPI, Javadoc, shared token expectations |
| `papiflyfx-docking-docks` | Ribbon runtime controls, layout/scaling engine, contextual visibility, session serialization hooks |
| `papiflyfx-docking-github` | GitHub ribbon provider and command group mapping |
| `papiflyfx-docking-hugo` | Hugo ribbon provider and contextual editor tooling |
| `papiflyfx-docking-samples` | Demo host integration and manual validation surface |
| `spec/**` | Plan/progress/validation artifacts and README updates |

## Key invariants

1. Existing docking layout/session restore must remain backward compatible; ribbon state must be optional and version-tolerant.
2. Feature modules contribute through SPI/contracts rather than direct runtime coupling to `papiflyfx-docking-docks`.
3. Ribbon commands remain UI-agnostic and testable outside of JavaFX Node construction.
4. Visual styling must use the shared `-pf-ui-*` token vocabulary and established theme patterns.
5. Adaptive resizing must not strand commands; collapsed groups still need accessible popup content.
6. Headless validation must remain possible for layout and persistence coverage.

## Proposed architecture

### Phase 1 — API and contribution model

1. Add a command abstraction that carries:
   - stable command id
   - label/tooltip metadata
   - small and large icon handles
   - enable/selected state
   - execution callback
2. Add ribbon contracts/SPI:
   - `RibbonProvider`
   - `RibbonTabSpec`
   - `RibbonGroupSpec`
   - control descriptors for button, toggle, split-button, and menu/dropdown use cases
   - a small `RibbonContext` abstraction for active dock/content state
3. Document the SPI with Javadoc and `ServiceLoader` registration rules.

### Phase 2 — Ribbon shell and shared visuals

1. Implement the ribbon container, tab strip, group chrome, and Quick Access Toolbar in `papiflyfx-docking-docks`.
2. Add minimized/expanded behavior.
3. Add shared ribbon CSS using the existing `-pf-ui-*` token vocabulary and align the visuals with the repository’s UI standards.
4. Keep the shell usable before advanced scaling is complete so module providers can be integrated incrementally.

### Phase 3 — Adaptive layout and collapsed groups

1. Implement group size modes: `LARGE`, `MEDIUM`, `SMALL`, `COLLAPSED`.
2. Add reduction priority so lower-priority groups shrink first as width decreases.
3. Define per-group layout templates or reduction rules so controls remain legible in each mode.
4. Implement collapsed-group popup content with the same command set as the expanded group.
5. Add tests around resize transitions and popup behavior.

### Phase 4 — Contextual tabs and module adoption

1. Add runtime context resolution in `papiflyfx-docking-docks` so providers can declare when tabs are visible.
2. Map GitHub workflows into stable groups such as sync, branches, collaborate, and state.
3. Map Hugo workflows into groups such as development, new content, build, modules, and environment.
4. Add at least one contextual Hugo editing tab driven by active dock/content type.
5. Wire providers into the sample application to validate end-to-end behavior.

### Phase 5 — Persistence, documentation, and validation

1. Persist ribbon state needed for iteration 1:
   - minimized state
   - selected tab id
   - Quick Access Toolbar command ids
2. Keep serialization backward compatible and tolerant of missing providers/commands on restore.
3. Update relevant README/spec docs with the SPI and hosting model.
4. Add regression coverage for:
   - resize/scaling transitions
   - contextual tab appearance/disappearance
   - persisted ribbon state restore
   - provider absence/fallback behavior

## Acceptance criteria

- [ ] `papiflyfx-docking-api` exposes a documented command/ribbon SPI suitable for `ServiceLoader` adoption
- [ ] `papiflyfx-docking-docks` hosts a functional ribbon shell with tabs, groups, Quick Access Toolbar, and minimized state
- [ ] Ribbon groups scale through the planned size modes without losing command access
- [ ] GitHub and Hugo modules contribute ribbon content through SPI instead of direct host wiring
- [ ] At least one contextual tab flow is implemented and validated
- [ ] Ribbon state restores safely without breaking existing layout/session files
- [ ] Sample application demonstrates the ribbon end-to-end
- [ ] Specs/README documents explain how new modules contribute commands/tabs
- [ ] Headless tests cover the critical regression paths for layout/context/persistence

## Validation strategy

1. `./mvnw -pl papiflyfx-docking-api,papiflyfx-docking-docks,papiflyfx-docking-github,papiflyfx-docking-hugo,papiflyfx-docking-samples -am compile`
2. `./mvnw -pl papiflyfx-docking-docks,papiflyfx-docking-github,papiflyfx-docking-hugo,papiflyfx-docking-samples -am -Dtestfx.headless=true test`
3. `./mvnw clean package`
4. Manual validation in the samples app:
   - resize window from narrow to wide and verify predictable group reduction
   - switch active content and verify contextual tab appearance/disappearance
   - minimize/restore the ribbon
   - restart with saved session state and verify ribbon state restoration

## Risks and mitigations

| Risk | Mitigation |
| --- | --- |
| API surface becomes too broad too early | Keep iteration 1 contracts narrow and map richer controls later |
| Session restore breaks older layouts | Add optional/versioned ribbon fields and restore fallbacks |
| JavaFX resize/popup behavior flickers across monitors | Isolate layout logic and validate collapsed popup behavior manually plus in TestFX |
| GitHub/Hugo commands already exist in bespoke toolbars | Reuse existing action logic; wrap it in ribbon commands instead of duplicating behavior |
| Contextual tab logic becomes dependent on concrete dock implementations | Route visibility through a small `RibbonContext` abstraction rather than Node-type probing in providers |

## Handoff notes

Lead Agent: `@core-architect`
Task Scope: Ribbon SPI, runtime shell, module contributions, persistence hooks, docs
Impacted Modules: `papiflyfx-docking-api`, `papiflyfx-docking-docks`, `papiflyfx-docking-github`, `papiflyfx-docking-hugo`, `papiflyfx-docking-samples`, `spec/**`
Files Changed: `spec/papiflyfx-docking-docks/2026-04-19-0-ribbon/plan.md`
Key Invariants: session compatibility, SPI-first modularity, shared theme token usage
Validation Performed: document synthesis review only
Open Risks / Follow-ups: confirm package placement and serialization shape before code work
Required Reviewer: `@spec-steward`
