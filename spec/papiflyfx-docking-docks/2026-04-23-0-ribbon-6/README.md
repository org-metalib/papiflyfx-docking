# Ribbon 6 - Design-Only Compatibility Candidates

**Status:** design-only; no implementation authorized by Ribbon 5
**Created from:** Ribbon 5 Phase 4
**Lead:** @core-architect
**Support:** @spec-steward
**Required reviewers before implementation:** @feature-dev, @ui-ux-designer, @qa-engineer, @spec-steward; consult @ops-engineer for build, archetype, release, or session migration tooling

## Purpose

This directory records future Ribbon 6 candidates that would require a dedicated implementation plan before any public API, session, runtime, CSS, Maven, test, or provider behavior changes.

## Canonical Artifact

- [ribbon-6-design.md](ribbon-6-design.md) - command contract split, boolean state replacement, control extensibility, `RibbonManager` decomposition, and future customization/session policy.
- [plan.md](plan.md) - implementation phases, invariants, validation expectations, and acceptance criteria derived from the design note.
- [progress.md](progress.md) - phase tracker and handoff notes for future implementation sessions.
- [prompt.md](prompt.md) - ready-to-use @core-architect session starter for implementing the plan.

## Current Baseline Link

Ribbon 5 remains the implemented baseline. See [../ribbon-status.md](../ribbon-status.md) and [../ribbon-release-notes.md](../ribbon-release-notes.md).
