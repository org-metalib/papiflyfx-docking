# ADR 0001 — Ribbon SVG Icon Loading

**Status:** Accepted  
**Date:** 2026-04-21  
**Lead Agent:** @core-architect  
**Reviewers:** @ui-ux-designer, @feature-dev, @qa-engineer, @ops-engineer, @spec-steward

## Context

Ribbon 2 Phase 3 requires the runtime icon resolver in `papiflyfx-docking-docks` to support SVG icon resources without changing the public SPI. Providers still contribute icon handles as URI-like strings through `PapiflyCommand.smallIcon` / `largeIcon` and `RibbonMenuSpec.smallIcon` / `largeIcon`.

The previous implementation handled:

- octicon pseudo-URIs (`octicon:...`)
- raster image resources resolvable as JavaFX `Image`

It explicitly skipped `.svg` resources, which limited icon quality and forced providers to choose raster-only assets.

## Decision

Implement SVG support inside `papiflyfx-docking-docks` with a small internal loader and **no new Maven dependency**.

The runtime now uses `RibbonIconLoader`, which resolves icon handles in this order:

1. octicon pseudo-URI via JavaFX `SVGPath`
2. SVG resource via a lightweight XML parse into one or more JavaFX `SVGPath` nodes
3. raster fallback via the existing JavaFX `Image` path
4. alternate icon handle fallback (`smallIcon` ↔ `largeIcon`) when the preferred handle fails

If SVG parsing or rendering fails:

- the loader logs at `WARNING`
- no exception is propagated to the provider caller
- raster loading is attempted next

## Rationale

Why no external library:

- Phase 3 only needs icon-grade SVG support, not a general-purpose SVG engine.
- The common ribbon icon case is path-based and maps directly to JavaFX `SVGPath`.
- Avoiding a new dependency keeps the runtime footprint small and avoids another transitive surface in the core docking module.
- The prompt allowed a minimal dependency at agent discretion; choosing “no additional dependency” is the lowest-footprint option that still satisfies the behavior requirement.

Why keep the SPI unchanged:

- Providers already emit stable string-based handles.
- The improvement is a runtime concern, not a provider-contract concern.
- Phase 3 is focused on rendering efficiency and correctness, not another SPI migration.

## Consequences

Positive:

- SVG icons now render in the ribbon without changing provider code.
- Broken SVG assets fail safely and degrade to raster handles or existing fallback glyph behavior.
- No new dependency was introduced, so no packaging-wide rebuild requirement was added for this decision.

Negative:

- The loader is intentionally icon-oriented, not a full SVG feature-complete renderer.
- Rich SVG features beyond path-based icon documents may still fall back to raster handles.

## Follow-up

- If Phase 5 provider migration introduces richer SVG assets and the internal path-based loader becomes insufficient, revisit this ADR and evaluate a dedicated lightweight SVG library with explicit performance and footprint measurements.
