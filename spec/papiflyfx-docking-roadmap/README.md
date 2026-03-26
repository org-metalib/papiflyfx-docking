# papiflyfx-docking roadmap

This directory captures a practical improvement roadmap for the repository as it exists today.
The goal is to improve onboarding, release confidence, module clarity, and long-term maintainability
without interrupting the current feature work across the PapiflyFX modules.

## Why this roadmap exists

The repository already has strong technical depth:
- a capable docking core,
- a broad set of feature modules,
- runnable samples,
- and extensive design material in `spec/`.

The main opportunities are not major rewrites. The biggest gains come from tightening documentation,
hardening CI, clarifying module maturity, and adding stronger regression coverage around the docking core
and cross-module persistence flows.

## Improvement goals

1. Make the project easier to adopt for a new contributor in less than 15 minutes.
2. Increase confidence in the docking core across floating, minimize/maximize, and persistence flows.
3. Clarify which modules are stable, evolving, or experimental.
4. Make releases more predictable across supported operating systems.
5. Improve the public-facing experience with better samples, docs, and success metrics.

## Status legend

- `planned` — agreed target, not yet started.
- `in-progress` — active work is underway.
- `blocked` — important prerequisite or decision is missing.
- `completed` — roadmap item is delivered and validated.

## Current assessment

### Strengths

- Strong core value proposition: IDE-style docking for JavaFX desktop applications.
- Good module breadth: docks, code, media, tree, settings, GitHub, login, Hugo, and samples.
- Existing tests across several modules, including TestFX-based UI coverage.
- Rich design history in `spec/`, which makes architectural intent discoverable.

### Main gaps

- The top-level documentation still contains TODO placeholders and partial module descriptions.
- CI is useful but narrow; it is mainly Ubuntu headless and does not fully reflect JavaFX platform risk.
- The repository does not clearly label module maturity or support expectations.
- The highest-risk behaviors appear to be integration behaviors rather than isolated unit behavior.
- The `spec/` directory is rich, but it is not yet curated as a clear map for current versus historical work.

## Priority order

### Priority 1 — onboarding and documentation

Reduce friction for first-time users and contributors:
- refresh the root README,
- add a module status matrix,
- create a short bootstrap guide,
- and make the sample entry points obvious.

### Priority 2 — build and release confidence

Strengthen CI and release checks:
- add a platform matrix,
- add targeted headless smoke coverage,
- and make release validation reproducible.

### Priority 3 — core reliability

Protect the docking engine where the UX is most complex:
- floating windows,
- drag and drop,
- persistence,
- restore flows,
- and cross-module content state.

### Priority 4 — public API and module boundaries

Clarify what is stable for reuse and what remains internal or experimental.

### Priority 5 — adoption and product polish

Make it easier to evaluate the project quickly through starter samples, screenshots, and a cleaner docs story.

## Prioritized checklist

| Priority | Area | Status | Near-term outcome |
| --- | --- | --- | --- |
| 1 | Foundations and onboarding | `planned` | Current README, module matrix, and sample startup path |
| 2 | Build and CI hardening | `planned` | Platform-aware CI and documented release validation |
| 3 | Docking core reliability | `planned` | Regression coverage for persistence and restore flows |
| 4 | API clarity and module maturity | `planned` | Clear public surface and module support expectations |
| 5 | Adoption and polish | `planned` | Better samples, visuals, and evaluation flow |

### Immediate next checklist

- [ ] `planned` — Refresh the root README and remove placeholder TODO sections.
- [ ] `planned` — Add a module status matrix covering stability, purpose, and entry points.
- [ ] `planned` — Curate `spec/` so active plans and historical documents are easier to navigate.
- [ ] `planned` — Expand CI to cover macOS and Windows in addition to Ubuntu headless runs.
- [ ] `planned` — Add targeted persistence and restore regression tests for the docking core.
- [ ] `planned` — Document the supported public API surface and internal-only boundaries.
- [ ] `planned` — Add screenshots or short recordings plus a minimal starter app path.

## Phased plan

### Phase 1 — foundations

Focus on documentation accuracy and contributor onboarding.

Target outcomes:
- root docs are current,
- module responsibilities are explicit,
- and a contributor can compile and run a sample quickly.

### Phase 2 — build and CI hardening

Focus on CI breadth, repeatable headless execution, and release safety.

Target outcomes:
- supported platforms are visible in CI,
- release steps are documented,
- and breakages are caught before publishing.

### Phase 3 — docking reliability

Focus on the highest-risk user behaviors in the docking framework.

Target outcomes:
- persistence scenarios are regression tested,
- floating and restore flows are deterministic,
- and complex layout changes are easier to validate.

### Phase 4 — API and module maturity

Focus on reuse, compatibility expectations, and module boundaries.

Target outcomes:
- public APIs are easier to discover,
- stable versus evolving modules are clearly labeled,
- and coupling between modules is more intentional.

### Phase 5 — adoption and polish

Focus on project presentation, discoverability, and sample quality.

Target outcomes:
- the project is easier to demo,
- easier to compare against alternatives,
- and easier to adopt in downstream desktop applications.

## Execution notes

- Use `phases.md` as the working checklist for phase-by-phase status updates.
- Update item status directly in the checklist as work moves from `planned` to `in-progress` to `completed`.
- Prefer closing Phase 1 and Phase 2 items before expanding feature scope further.

## What this roadmap avoids

- No large rewrite of the docking architecture unless testing shows a structural issue.
- No expansion into many new modules before the existing core and integration points are hardened.
- No broad dependency churn without a clear compatibility or maintenance benefit.

## Success signals

- A new user can build the repository and launch a sample with one short setup guide.
- CI passes on the supported desktop platforms relevant to JavaFX packaging and runtime behavior.
- Dock layout save/restore regressions are covered by targeted automated tests.
- The README and `spec/` index make the project structure obvious.
- Each module has a clearly stated role and maturity level.

## Next document

Use `phases.md` in this directory as the execution-oriented version of the roadmap.
