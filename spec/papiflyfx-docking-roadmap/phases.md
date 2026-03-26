# papiflyfx-docking roadmap phases

This document breaks the roadmap into practical initiatives, expected deliverables, and completion signals.

## Status summary

| Phase | Status | Focus |
| --- | --- | --- |
| Phase 1 — foundations and onboarding | `planned` | Documentation, module map, and sample discoverability |
| Phase 2 — build, CI, and release hardening | `planned` | Platform coverage, smoke validation, and release safety |
| Phase 3 — docking core reliability | `planned` | Persistence, restore behavior, and integration regressions |
| Phase 4 — API clarity and module boundaries | `planned` | Public API surface and dependency clarity |
| Phase 5 — adoption, UX polish, and developer experience | `planned` | Samples, visuals, and evaluation experience |

## Checklist conventions

- `[ ] planned` — not started yet.
- `[~] in-progress` — actively being worked on.
- `[x] completed` — finished and verified.
- `[-] blocked` — waiting on a prerequisite or decision.

## Phase 1 — foundations and onboarding

### Objective

Make the repository easier to understand, build, and run for a first-time contributor.

### Initiatives

1. Refresh root documentation
   - Remove TODO placeholders from the top-level README.
   - Expand the module summary so it reflects the actual module list in the root POM.
   - Add a short “start here” path for library users versus contributors.

2. Add a module status matrix
   - Label modules as `stable`, `active`, or `experimental`.
   - State the owner, purpose, and primary entry point for each module.
   - Link each module to its README and relevant spec directory.

3. Curate the `spec/` directory
   - Add a simple index of active plans, historical investigations, and completed work.
   - Standardize naming for roadmap, progress, and validation documents going forward.

4. Improve sample discoverability
   - Document the main sample launcher and sample entry points.
   - Add a short explanation of which samples demonstrate which modules.

### Completion signals

- Root README is current and free of placeholder TODO text.
- The repository has a clear module matrix and startup path.
- `spec/` has a discoverable landing page for active work.

### Checklist

- [ ] planned — Refresh the root README and remove placeholder sections.
- [ ] planned — Expand the top-level module summary to match the root POM.
- [ ] planned — Add a “start here” split for contributors versus library consumers.
- [ ] planned — Add a module status matrix with purpose, maturity, and entry points.
- [ ] planned — Curate the `spec/` landing page for active versus historical work.
- [ ] planned — Document the sample launcher and what each sample demonstrates.

## Phase 2 — build, CI, and release hardening

### Objective

Reduce release risk and make failures reproducible.

### Initiatives

1. Expand CI platform coverage
   - Add macOS and Windows jobs in addition to Ubuntu.
   - Keep headless tests where appropriate and isolate platform-sensitive flows.

2. Add targeted smoke validation
   - Run a fast smoke pass for core modules on pull requests.
   - Run deeper module tests on mainline or scheduled workflows if needed.

3. Document release validation
   - Create a short release checklist.
   - Include required Java version, signing steps, publishing profiles, and dry-run commands.

4. Add dependency and packaging sanity checks
   - Validate dependency resolution in CI.
   - Guard against accidental drift in plugin configuration or unsupported runtime assumptions.

### Completion signals

- CI clearly reflects supported JavaFX platforms.
- Release steps are documented and reproducible.
- Build failures are easier to diagnose from workflow output.

### Checklist

- [ ] planned — Add macOS CI coverage for JavaFX validation.
- [ ] planned — Add Windows CI coverage for JavaFX validation.
- [ ] planned — Keep or refine fast Ubuntu headless smoke coverage for pull requests.
- [ ] planned — Document a release checklist with Java, signing, and publishing steps.
- [ ] planned — Add sanity checks for dependency resolution and plugin drift.

## Phase 3 — docking core reliability

### Objective

Increase confidence in the most complex and user-visible docking behaviors.

### Initiatives

1. Persistence regression suite
   - Add focused tests for save/restore of split, tab, floating, minimized, and maximized layouts.
   - Verify content-state round-tripping with representative modules.

2. Restore behavior hardening
   - Validate placeholder behavior when content adapters are unavailable.
   - Verify behavior when layout data is partial, stale, or versioned.

3. Integration test coverage
   - Add targeted cross-module tests using code, media, settings, or GitHub content where useful.
   - Prefer deterministic scenarios over broad UI automation.

4. Failure analysis and observability
   - Improve error reporting around layout restore, adapter mismatch, and window lifecycle issues.
   - Capture known edge cases in spec docs with reproduction steps.

### Completion signals

- Core layout persistence scenarios have stable automated coverage.
- Restore failures are easier to understand and reproduce.
- Regressions in floating and content restoration are caught earlier.

### Checklist

- [ ] planned — Add persistence tests for split and tab layouts.
- [ ] planned — Add persistence tests for floating, minimized, and maximized states.
- [ ] planned — Verify round-trip behavior for representative content-state adapters.
- [ ] planned — Harden restore behavior for missing adapters and partial layout data.
- [ ] planned — Add deterministic cross-module integration tests where risk is highest.
- [ ] planned — Improve restore and window lifecycle diagnostics.

## Phase 4 — API clarity and module boundaries

### Objective

Make the project safer to consume as a library and easier to evolve internally.

### Initiatives

1. Define public API surfaces
   - Identify which packages and entry points are supported for downstream use.
   - Add or improve Javadoc for stable entry points.

2. Clarify module coupling
   - Review dependencies between API, docks, settings, GitHub, login, and samples.
   - Reduce accidental coupling where lighter contracts are possible.

3. Versioning expectations
   - Document what kinds of changes are considered compatible.
   - Align release notes and tags with module-level changes.

4. Internal versus external contracts
   - Mark internal helpers more clearly in docs and package structure.
   - Keep samples and experiments from becoming accidental APIs.

### Completion signals

- Downstream users can identify the supported API surface quickly.
- Module responsibilities and dependencies are easier to reason about.
- Release notes communicate compatibility expectations.

### Checklist

- [ ] planned — Identify and document the supported public API entry points.
- [ ] planned — Add or improve Javadoc for stable public types.
- [ ] planned — Review inter-module dependencies for accidental coupling.
- [ ] planned — Document compatibility expectations for releases and module changes.
- [ ] planned — Distinguish internal helpers from external contracts in docs and structure.

## Phase 5 — adoption, UX polish, and developer experience

### Objective

Make the framework easier to evaluate and more attractive to downstream desktop applications.

### Initiatives

1. Improve visual documentation
   - Add screenshots or short recordings for core docking behaviors and major feature modules.
   - Show before-and-after examples for persistence and floating window flows.

2. Create a minimal starter app path
   - Provide a tiny example that embeds the docking core with one or two simple leaves.
   - Keep it smaller than the samples module and optimized for quick understanding.

3. Strengthen sample quality
   - Ensure sample naming, descriptions, and launch behavior are consistent.
   - Make the sample launcher the obvious evaluation entry point.

4. Publish a contributor roadmap snapshot
   - Revisit this roadmap on a regular cadence.
   - Mark initiatives as completed, active, deferred, or dropped.

### Completion signals

- A new user can evaluate the framework visually and functionally in minutes.
- The minimal starter path complements the richer samples module.
- Roadmap status becomes a living planning artifact instead of a one-time document.

### Checklist

- [ ] planned — Add screenshots or short recordings for core docking workflows.
- [ ] planned — Add a minimal starter app path smaller than the full samples module.
- [ ] planned — Standardize sample naming, descriptions, and launch behavior.
- [ ] planned — Keep the roadmap updated with `planned`, `in-progress`, `blocked`, and `completed` states.

## Suggested execution order

1. Phase 1
2. Phase 2
3. Phase 3
4. Phase 4
5. Phase 5

This order prioritizes clarity first, release confidence second, core reliability third, and polish after the basics are stable.
