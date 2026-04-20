# Progress — Docking Ribbon Toolbar

**Status:** Phase 1 implemented, Phase 2 not started
**Current Milestone:** Phase 1 complete in `papiflyfx-docking-api`
**Priority:** P2 (Normal)
**Lead Agent:** @core-architect
**Required Reviewers:** @ui-ux-designer, @feature-dev, @qa-engineer, @ops-engineer, @spec-steward

## Completion summary

- Research: 100%
- Planning: 100%
- Phase 1 — API and contribution model: 100%
- Phase 2 — Ribbon shell and shared visuals: 0%
- Phase 3 — Adaptive layout and collapsed groups: 0%
- Phase 4 — Contextual tabs and module adoption: 0%
- Phase 5 — Persistence, documentation, and validation: 0%

## Accomplishments

- [2026-04-19] Created the ribbon spec folder and `README.md` title for the initiative.
- [2026-04-19] Captured architectural research in `research-gemini.md`, covering command abstractions, ribbon hierarchy, scaling modes, persistence goals, and GitHub/Hugo mappings.
- [2026-04-19] Distilled the research into an implementation-oriented `plan.md` with phased delivery, scoped iteration-1 goals, invariants, validation strategy, and review ownership.
- [2026-04-19] Established the initial reviewer set for this cross-cutting effort:
  - `@core-architect` for API/runtime ownership
  - `@ui-ux-designer` for ribbon chrome, tokens, and ergonomics
  - `@feature-dev` for GitHub/Hugo command mapping
  - `@qa-engineer` for resize/persistence/context regression coverage
  - `@ops-engineer` for sample-app integration and build validation
- [2026-04-19] Added the Phase 1 ribbon SPI in `papiflyfx-docking-api` under `org.metalib.papifly.fx.api.ribbon`, including `PapiflyCommand`, `RibbonContext`, `RibbonProvider`, `RibbonTabSpec`, `RibbonGroupSpec`, and the initial control descriptors.
- [2026-04-19] Documented the contribution model with Javadoc and package-level ServiceLoader guidance so feature modules can register `RibbonProvider` implementations via `META-INF/services`.
- [2026-04-19] Preserved the UI-agnostic API invariant by modeling commands and descriptors without `javafx.scene.Node`, while keeping enable/selected state on JavaFX properties for binding.
- [2026-04-19] Validated the new API surface with `./mvnw -pl papiflyfx-docking-api compile`.

## Next tasks

1. Implement the Phase 2 ribbon shell in `papiflyfx-docking-docks`, starting with the tab strip, group chrome, and host/runtime wiring for `RibbonProvider` discovery.
2. Confirm whether ribbon state extends the existing docking session JSON or uses a parallel persisted payload before persistence work begins.
3. Inventory reusable GitHub and Hugo actions so provider implementations wrap existing behavior instead of re-implementing it.
4. Define the active dock/content signals that `papiflyfx-docking-docks` will surface when constructing `RibbonContext`.
5. Outline the first headless/TestFX scenarios for resize, collapsed groups, contextual visibility, and restore.

## Open risks

- The work still crosses API, runtime, feature modules, sample hosting, and docs; uncontrolled scope growth remains the main delivery risk after the API foundation.
- Session persistence shape still needs early agreement to avoid rewriting serialization work later.
- A full Microsoft-style ribbon feature set remains too broad for one iteration; the current plan intentionally defers customization UI, rich ScreenTips, and full KeyTips.
- JavaFX popup/layout behavior for collapsed groups will likely need manual validation on top of headless coverage once the ribbon shell exists.

## Validation status

- `./mvnw -pl papiflyfx-docking-api compile` passed after adding the Phase 1 ribbon SPI.
- Validation is currently limited to API compilation; runtime rendering, ServiceLoader integration, and persistence remain unimplemented.

## Handoff snapshot

Lead Agent: `@core-architect`
Task Scope: Implement Phase 1 of the planned ribbon SPI/runtime foundation
Impacted Modules: `papiflyfx-docking-api`, `spec/**`
Files Changed: `papiflyfx-docking-api/src/main/java/org/metalib/papifly/fx/api/ribbon/*`, `spec/papiflyfx-docking-docks/2026-04-19-0-ribbon/progress.md`
Key Invariants: session compatibility, SPI-first module contributions, shared `-pf-ui-*` styling, accessible fallback for collapsed groups
Validation Performed: `./mvnw -pl papiflyfx-docking-api compile`
Open Risks / Follow-ups: wire provider discovery into the docks runtime and settle ribbon persistence shape before Phase 2/5 work
Required Reviewer: `@spec-steward`, `@ops-engineer`
