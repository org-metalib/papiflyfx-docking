# Docking Ribbon 2 - Decoupling And Hardening

**Status:** implemented; current for the Ribbon 2 breaking hardening cutover
**Lead:** @core-architect
**Compatibility stance:** breaking changes were explicitly accepted

## Purpose

Ribbon 2 replaced the Ribbon 1 prototype contracts with UI-neutral command/context APIs, stable command identity, id-first QAT state, adaptive-layout hardening, SVG icon support, and namespaced dock-session extension persistence.

## Canonical Artifacts

- [plan.md](plan.md) - approved scope, phases, compatibility stance, and validation strategy.
- [research.md](research.md) - source findings and improvement options.
- [adr-0001-svg-icons.md](adr-0001-svg-icons.md) - SVG icon support decision.
- [progress.md](progress.md) - implementation record, validation, and final handoff.

## Current Session Note

Ribbon 2 intentionally replaced the historical top-level `ribbon` payload with the current `extensions.ribbon` payload. The authoritative fields are:

- `extensions.ribbon.minimized`
- `extensions.ribbon.selectedTabId`
- `extensions.ribbon.quickAccessCommandIds`

See [../ribbon-status.md](../ribbon-status.md) and [../ribbon-release-notes.md](../ribbon-release-notes.md) for current status and migration notes.
