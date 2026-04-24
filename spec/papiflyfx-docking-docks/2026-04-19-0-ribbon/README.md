# Docking Ribbon Toolbar

> Historical note: this Ribbon 1 document is preserved for traceability. Its persistence section shows the original top-level `ribbon` payload. Current Ribbon 5 code persists ribbon shell state under `extensions.ribbon`; see [../ribbon-status.md](../ribbon-status.md) and [../ribbon-release-notes.md](../ribbon-release-notes.md).

## Status

Phase 5 complete (API, runtime, adoption, persistence, docs, and validation coverage).

## Scope Summary

The ribbon initiative adds a Microsoft-style command surface to PapiflyFX while
keeping the framework modular and session-compatible:

- UI-agnostic command and ribbon SPI in `papiflyfx-docking-api`.
- Runtime shell in `papiflyfx-docking-docks` (`Ribbon`, `RibbonManager`,
  adaptive group sizing, collapsed popup groups, Quick Access Toolbar, minimized state).
- ServiceLoader provider adoption in `papiflyfx-docking-github`,
  `papiflyfx-docking-hugo`, and sample providers.
- Contextual-tab visibility driven by `RibbonContext`.
- Session persistence for ribbon shell state.

## Runtime Architecture

### SPI layer (`papiflyfx-docking-api`)

- `PapiflyCommand` provides stable command identity, metadata, and state props.
- `RibbonProvider` contributes tabs via ServiceLoader.
- `RibbonTabSpec` and `RibbonGroupSpec` describe tab/group structure and
  visibility/reduction policies.
- `RibbonControlSpec` variants define control descriptors without coupling to
  JavaFX view classes.

### Host/runtime layer (`papiflyfx-docking-docks`)

- `RibbonManager` discovers providers and materializes visible tabs for current
  `RibbonContext`.
- `Ribbon` renders tab strip, groups, adaptive size transitions
  (`LARGE`/`MEDIUM`/`SMALL`/`COLLAPSED`), and QAT.
- `RibbonDockHost` mounts ribbon + dock content and binds theme/context.
- `DockManager` session capture/restore now supports extensible
  `DockSessionStateContributor` hooks; ribbon uses this hook for persistence.

## Persistence Model (Phase 5, Superseded)

The JSON example below is the historical Ribbon 1 shape. It was intentionally superseded by Ribbon 2's namespaced extension model. Current persisted state lives under `extensions.ribbon.minimized`, `extensions.ribbon.selectedTabId`, and `extensions.ribbon.quickAccessCommandIds`.

Ribbon state is stored in the dock session payload under optional `ribbon`:

- `minimized`: `boolean`
- `selectedTabId`: `string | null`
- `quickAccessCommandIds`: `string[]`

Example:

```json
{
  "type": "dockSession",
  "version": 2,
  "layout": { "...": "..." },
  "ribbon": {
    "minimized": true,
    "selectedTabId": "hugo-editor",
    "quickAccessCommandIds": ["github.fetch", "hugo.preview"]
  }
}
```

Compatibility guarantees:

- Sessions without `ribbon` still deserialize and restore.
- Missing tabs during restore fall back to the first available visible tab.
- Missing QAT command IDs are ignored (no restore failure).
- Persisted unknown/non-string QAT entries are filtered out.

## Validation Coverage

Automated:

- Serializer/persistence round-trip now covers ribbon payload.
- Ribbon restore tests cover:
  - minimized state
  - selected tab restore
  - QAT restore by command IDs
  - fallback when persisted tab/command IDs are unavailable
- Adaptive layout regression tests cover collapse and re-expand transitions.
- Existing contextual-tab and provider-integration suites remain in place.

Manual target scenario (samples app):

1. Open `Ribbon Shell` sample.
2. Change tab selection, minimize/expand ribbon, alter QAT commands.
3. Save session, restart app, restore session.
4. Verify restored ribbon shell state and contextual-tab behavior.
