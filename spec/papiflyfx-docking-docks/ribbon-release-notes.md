# Ribbon Release And Migration Notes

**Status:** current for Ribbon 6 compatibility implementation
**Scope:** spec-level release notes because the repository does not currently have a project changelog

## Ribbon 2 Breaking Change Context

Ribbon 2 intentionally accepted breaking changes to harden the public ribbon SPI and session model. Consumers upgrading from Ribbon 1-era code should expect source and session compatibility impacts.

Breaking context:

- Shared command contracts were made UI-neutral; providers should not expose JavaFX controls or runtime scene-graph details through API-level command descriptors.
- Provider action resolution moved toward typed capabilities instead of `ACTIVE_CONTENT_NODE` casting. `ACTIVE_CONTENT_NODE` remains a compatibility bridge but is not recommended for new providers.
- Command identity became canonical and id-first. Reusing a command id for a different semantic action is a provider defect.
- QAT persistence became id-first through stable command ids rather than persisted command objects.
- Dock-session extension state moved to namespaced extension payloads. Current ribbon state is under `extensions.ribbon`, not a top-level `ribbon` field.
- Ribbon 1 top-level ribbon session payloads are not restored as ribbon state by the current Ribbon 2+ session model.

See:

- [Ribbon 2 plan](2026-04-20-0-ribbon-2/plan.md)
- [Ribbon 2 progress](2026-04-20-0-ribbon-2/progress.md)
- Runtime README session guidance: [../../papiflyfx-docking-docks/README.md](../../papiflyfx-docking-docks/README.md)

## Current Ribbon 5 Session Notes

Current persisted ribbon payload:

- `extensions.ribbon.minimized`
- `extensions.ribbon.selectedTabId`
- `extensions.ribbon.quickAccessCommandIds`
- `extensions.ribbon.placement`

Compatibility behavior:

- Missing `extensions.ribbon` means no ribbon shell state to restore.
- QAT ids remain pinned by id; unresolved ids can resolve later when providers contribute matching commands.
- Unknown fields under `extensions.ribbon` are ignored on decode.
- Missing, unknown, or malformed `extensions.ribbon.placement` restores as `TOP` without dropping minimized state, selected tab id, or QAT ids.
- Other malformed known fields remain strict for the ribbon extension and are isolated from core layout restore.
- Unknown customization fields are not preserved on save in Ribbon 5 because the runtime recaptures known shell state only.

## Ribbon 9 Placement Notes

Ribbon 9 adds host-configurable placement without changing provider contracts:

- `RibbonPlacement` supports `TOP`, `LEFT`, `RIGHT`, and `BOTTOM`.
- `RibbonDockHost` and `Ribbon` expose `placementProperty()`, `getPlacement()`, and `setPlacement(...)`.
- Existing hosts default to `TOP`.
- Side placements keep a readable vertical tab strip on the outside edge and render selected-tab command groups in the inner pane.
- The SamplesApp catalog includes `Ribbon Placement` for deterministic `TOP` and `LEFT` comparison.

## Ribbon 5 Completed Follow-Up Notes

Ribbon 5 closed the review loop with non-breaking runtime, provider, test, and documentation work:

- Refreshed command state/action projection while preserving canonical command identity.
- Disposable JavaFX bindings and focused listener-leak coverage.
- Provider failure and duplicate-id diagnostics.
- Accessibility/focus/theme fixes for ribbon chrome.
- Typed context metadata and explicit capability contribution.
- Provider-authoring guidance and focused Maven selector docs.

## Ribbon 6 Notes

Ribbon 6 implements the planned compatibility break from `2026-04-23-0-ribbon-6/plan.md`:

- action-only commands use `RibbonCommand`;
- toggle-capable commands use `RibbonToggleCommand`;
- action-only commands no longer expose selected state;
- boolean state uses `RibbonBooleanState#subscribe(...)` and `RibbonStateSubscription`;
- the deprecated `PapiflyCommand`, `BoolState`, and `MutableBoolState` bridge types are removed;
- built-in controls expose `RibbonControlSpec#kind()` and are dispatched through package-private runtime strategies/render plans;
- unknown control kinds are diagnosed and skipped;
- `QuickAccessState` owns id-first QAT state behind the stable `RibbonManager` facade.

Source compatibility breaks:

- Providers should replace `PapiflyCommand.of(...)` with `RibbonCommand.of(...)` for action-only controls.
- Providers that constructed `PapiflyCommand` directly should use `RibbonCommand.of(...)` for actions or `RibbonToggleCommand.of(...)` for toggles.
- Toggle controls must provide `RibbonToggleCommand`; passing an action-only `RibbonCommand` to `RibbonToggleSpec` no longer compiles.
- Code that read `selected()` from a push/action command must move that state to a toggle command or provider-local state.
- `BoolState#addListener` / `removeListener` call sites should migrate to `subscribe(...).close()`.

Session compatibility:

- Ribbon state remains under `extensions.ribbon`.
- QAT persistence remains `quickAccessCommandIds`; command objects are not persisted.
- Unknown ribbon fields are ignored on decode; malformed known fields remain strict and isolated to ribbon extension restore.

## Deferred Work

| Item | Owner | Reason |
| --- | --- | --- |
| Archetype ribbon scaffold | @ops-engineer | Useful for generated apps, but Phase 5 is docs-only and provider-authoring guidance is sufficient for current implementation. |
| Surefire/TestFX POM centralization | @ops-engineer with @qa-engineer | Current duplicated blocks have module-specific native-access needs; changing build config needs a separate plan. |
| Performance budgets | @qa-engineer with @ops-engineer and @core-architect | No accepted timing/layout budget exists. Any benchmark should be opt-in and benchmark-tagged. |
| Busy/running command state | @ui-ux-designer with @core-architect and @feature-dev | Needs UI, accessibility, cancellation, and announcement semantics before API work. |
| Keytips | @ui-ux-designer with @core-architect | Needs focus scope and accessibility design. |
| Galleries | @ui-ux-designer with @core-architect and @feature-dev | Needs item identity, keyboard behavior, layout budget, QAT, and customization policy. |
| Customization UI/schema | @core-architect with @ui-ux-designer and @spec-steward | Requires schema versioning and unknown-field round-trip policy before implementation. |
| Code/tree/media providers | @feature-dev | Future feature-module work; Ribbon 6 does not add production provider requirements for those modules. |
